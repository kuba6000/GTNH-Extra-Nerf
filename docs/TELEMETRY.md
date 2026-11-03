# Telemetry

Extra Nerf reports optional community statistics from the logical Minecraft server, only for worlds whose owners have agreed.
Dedicated servers and integrated singleplayer servers report; connected remote clients do not.
Development environments remain disabled. Other builds can report even when their JAR cannot be read.
The endpoint is `https://extranerf.kuba6000.pl/iamalive`.

## Report

HTTPS POST, `Content-Type: application/json`, `User-Agent: GTNH-Extra-Nerf`.
There are exactly five JSON fields:

```json
{
  "sessionId": "62420a97-276b-4afd-a226-558f247802ac",
  "onlinePlayers": 3,
  "highestTier": "lv",
  "singleplayer": true,
  "jarProof": null
}
```

`sessionId` is a random UUID v4 held only in memory for one server run. Restarting
or reopening the world changes it; midnight and toggling telemetry do not.
`singleplayer` means an integrated server, including one opened to LAN.
`highestTier` is the highest usable progression tier with all earlier prerequisites
satisfied, from Stone through UXV. Missing Steam gives Stone; a revoked earlier
certificate blocks later tiers. MAX is not reported.

No player names, player UUIDs, inventories, coordinates, world files or exam answers
are sent. The mod refuses to send over plain HTTP (only a loopback test endpoint is
exempt), so HTTPS always protects transport.

## Responses

The server answers `204 No Content` to every POST and does not say whether the report
was used, so the mod logs `HTTP 204` as "sent", not as "accepted". After `429` or `503`
the mod waits for the `Retry-After` time (60 seconds when the header is missing) before
the next attempt.

## JAR proof

The worker attempts to hash Forge's original JAR once, away from the server thread.
Missing/unreadable files produce `jarProof: null` without suppressing the report.
For readable files, every report uses a fresh 16-byte random salt:

```text
jarDigest = SHA-256(original JAR bytes)
jarProof = {salt: lowercaseHex(salt), sha256: lowercaseHex(SHA-256(salt || jarDigest))}
```

The proof tells the receiver which release the report comes from without sending the
JAR itself. The hash is salted, so two reports from the same JAR look different.

## What is not in the report

Your IP address is not part of the report. See the
[privacy notice](https://extranerf.kuba6000.pl/privacy) for how long the receiving
server keeps what it receives.

## Consent

Telemetry is opt-in, and the choice belongs to the world: it is saved in the world
(`TelemetryConsent`, in the overworld's map storage) together with the team tracking settings.
Until a world has been **asked**, no setting counts and nothing is sent; `status` says
`not asked yet`.

- **The window.** A few seconds after an operator (or the owner of a singleplayer world, who
  needs no cheats) joins a world that has not been asked, the server opens a window that
  describes what minimal statistics are, that they are used only to show community statistics on
  the website, and links to the privacy and team tracking policies.
  It has three buttons: **Disable telemetry**, **Send minimal statistics** and **Enable full team
  tracking** (minimal statistics plus team tracking, with the player who clicked as leader).
  The server checks who clicked; the first answer wins. Closing the window with Esc is not an
  answer: the same player is asked again after the next login, once per server run.
- **The console.** A dedicated server logs the question about fifteen seconds after it has started (so that
  the start-up output of other mods does not bury it), with the two commands that answer it,
  and a reminder two minutes later. The server does not wait: it
  carries on and sends nothing. The console cannot enable team tracking while nobody is online,
  because the team needs a leader.
- **The commands.** `/extranerftelemetry on` and `off`, and `/extranerfteam on` and `off`, all
  count as an answer. `/extranerfteam on` does not turn minimal statistics on.
- **Older settings.** A world without a record takes over what older versions kept in
  `config/gtnhextranerf.cfg`, once: a team that was listed there is moved into the first world
  that is loaded (and cleared from the file, so that no other world lists itself under the same
  ID) and counts as an answer; a refusal (`telemetry.enabled=false`) is copied to every world
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
require permission. Status shows the last result and next scheduled report.
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
```

The fixture explicitly disables production reporting; verify the scenario result in the generated JSON/JUnit reports.
