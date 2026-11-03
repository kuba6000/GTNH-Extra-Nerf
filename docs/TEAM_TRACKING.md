# Team tracking

An opt-in feature that lists a server as a team. It is separate from
[telemetry](TELEMETRY.md): its own command and report. Turning telemetry off does not affect it, and the other
way around.

## Opting in

An operator (or the console, or the world owner in singleplayer) runs the command below, or
chooses **Enable full team tracking** in the window that asks a new world about telemetry (see
[TELEMETRY.md](TELEMETRY.md#consent)). Either way the world counts as asked.

```
/extranerfteam on
/extranerfteam status
/extranerfteam off
```

- The team is named after its **leader**: `<leader name>'s team`. The leader is the player
  who ran the command. From the console it is the first player online; with nobody online the
  command asks you to join first.
- The first `on` creates a permanent **team ID** (a random UUID), saved in the world. It never changes, also
  across `off` and `on` and restarts of that world.
- Running `on` again makes the player who ran it the new leader and keeps the ID.

## Telling players and opting out

Every player who joins a server with team tracking on gets a chat message saying so, with a link to the Team Tracking Policy.

`/extranerfteam off` stops the reports and prints the team ID with the instruction to email it to the contact address if the stored data should be deleted now.

## What is saved and sent

The values are saved in the world (`TelemetryConsent`): whether it is on, the team ID, the
leader's UUID and `leaderName`. A team that older versions saved in the configuration file is moved into the
first world that is loaded. The leader's name is refreshed whenever the
leader is online.

Every 15 minutes (first report after 10 minutes), a daemon worker posts to
`https://extranerf.kuba6000.pl/teamtracking`:

```json
{
  "teamId": "0f8fad5b-d9cb-469f-a165-70867728950e",
  "leader": {"id": "<uuid>", "name": "Kuba"},
  "highestTier": "lv",
  "singleplayer": false,
  "players": [{"id": "<uuid>", "name": "Kuba"}],
  "jarProof": {"salt": "...", "sha256": "..."}
}
```

`highestTier` is the highest progression tier the world has unlocked (the same value telemetry uses).
`singleplayer` is true on an integrated server.
`players` are the players online when the report is captured on the server thread (at most
256, names of 1–16 letters, digits or underscores). `jarProof` is the same salted JAR proof
as in telemetry. Development environments never report.

A world that earned a certificate above Steam before its overworld had run 5 hours never reports, as for
telemetry (see [TELEMETRY.md](TELEMETRY.md)): team tracking stops with a log line and `/extranerfteam status` shows the reason.

## Tests

```powershell
.\gradlew.bat test --tests '*teams*'
```
