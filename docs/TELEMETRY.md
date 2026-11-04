# Telemetry

Extra Nerf reports optional community statistics from the logical Minecraft server, only for worlds whose owners have agreed.
Dedicated servers and integrated singleplayer servers report; connected remote clients do not.
Development environments remain disabled. Other builds can report even when their JAR cannot be read.

## Report

Basic reports contain a random ID for one server run, the online player count, the
world's progression tier, whether the server is integrated or dedicated, and an
optional randomized build proof. Restarting or reopening the world changes the
session ID; midnight and toggling telemetry do not. An integrated server includes
a singleplayer world opened to LAN.

The progression tier is the highest usable tier with all earlier prerequisites
satisfied, from Stone through UXV. Missing Steam gives Stone; a revoked earlier
certificate blocks later tiers. MAX is not reported.

No player names, player UUIDs, inventories, coordinates, world files or exam answers
are included in basic reports. The optional build proof helps identify published
releases without sending the JAR or its unmodified fingerprint.

## What is not in the report

Your IP address is not part of the report. See the
[privacy notice](https://extranerf.kuba6000.pl/privacy) for how long the receiving
server keeps what it receives.

## Consent

Reporting is opt-in, and the choice belongs to the world: it is saved immediately in the world
(`TelemetryConsent`, in the overworld's map storage) together with the team tracking settings.
Until a world has been **asked**, no setting counts and no statistics or team reports are sent;
`status` indicates that no choice has been made.
If an existing decision cannot be read, reporting stays off and the original file is left in
place for recovery; check the server log and world storage.

- **The window.** A few seconds after an operator (or the owner of a singleplayer world, who
  needs no cheats) joins a world that has not been asked, the server opens a window that
  describes what basic statistics are, that they are used to show anonymous, aggregated statistics on
  the website, and links to the privacy and team tracking policies.
  It has three buttons: **Disable reporting**, **Send basic statistics** and **Enable team
  tracking** (a single team report that also supplies server statistics, with the player who clicked as leader).
  The server checks who clicked; the first answer wins. Closing the window with Esc is not an
  answer: the same player is asked again after the next login while the world remains undecided.
- **The console.** A dedicated server logs the question about fifteen seconds after it has started (so that
  the start-up output of other mods does not bury it), with the two commands that answer it,
  and a reminder two minutes later. The server does not wait: it
  carries on without sending statistics or team reports. The console must name an online player to enable team tracking,
  because the team needs a leader.
- **The commands.** `/extranerftelemetry on` selects basic statistics and turns team tracking off;
  `/extranerfteam on` selects team reporting and turns basic statistics off. The `off` command
  stops its own mode without enabling the other. All four commands count as an answer.
- **Older settings.** A world without a record takes over what older versions kept in
  `config/gtnhextranerf.cfg`, once: a team ID, including one that was disabled, is moved into the
  first world that is loaded and then cleared from the file. An active team stays enabled and
  takes precedence over basic statistics; a refusal (`telemetry.enabled=false`) is copied to every world
  and is not asked again; everything else, including the old default of `true`, is asked.
  The keys in the file are only read for this.

## Lifecycle and opt-out

First report after about ten minutes, then hourly. Snapshot capture happens on the server
thread; hashing/HTTP run on one daemon worker. Five-second connect/read timeouts, no redirect
following, one in-flight attempt, no retry queue. HTTP and JAR errors do not affect gameplay.
Server stop and opt-out cancel queued work; a request already in progress may finish.
Development runtimes never start a sender (set `-Dgtnhextranerf.consentPreview=true` to see
the question there anyway).

Owners can use `/extranerftelemetry on`, `off` and `status` (no slash in console). Dedicated
servers require operator level 4. The integrated world owner does not need cheats; LAN guests
require permission. Status reports whether the selected mode is sending, waiting, or needs attention;
after a successful report, it also shows how long ago the response was received. This status is
kept in memory and starts over when the reporting mode is restarted.
A world that earns any certificate above Steam before its overworld has run 5 hours (`getTotalWorldTime`, which `/time set` does not change) is treated as a test, creative or cheat world: telemetry is turned off for it for good. The decision is saved in the world (`ProgressionData`, `telemetryBlocked`), is taken when the certificate is earned, and `/extranerftelemetry on` does not undo it; the log says so. Nothing extra is sent. Team tracking follows the same decision and does not report from such a world either.

There is no force-send command. While telemetry is active, everyone who may run
that command (operators, and the world owner in singleplayer) gets one chat message
when joining, saying what is reported, how to turn it off and where the privacy policy
is. Other players are not told.

## Verification

```powershell
.\gradlew.bat test --tests '*telemetry*'
python -X utf8 -m unittest discover -s .github/scripts -p 'test_*.py'
```

The fixed `report.json` vector is independently derived with Python hashlib and
checked by Java. Native configuration/lifecycle/capture checks use:

```powershell
python .github/scripts/run_horizon_tests.py --tests gtnhextranerftests:NativeScenarios.telemetry
python .github/scripts/run_horizon_tests.py --tests gtnhextranerftests:ConsentScenarios.promptAfterAnotherLogin
```

The first fixture explicitly disables production reporting. The second closes the question and simulates
the next Forge login event for the same player; it exercises the UI without a network reconnect. Verify
the scenario results in the generated JSON/JUnit reports.
