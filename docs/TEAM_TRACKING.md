# Team tracking

An opt-in reporting mode that can list a server as a team and contributes to community statistics.
It is mutually exclusive with [basic statistics](TELEMETRY.md): enabling either mode turns the
other off. Turning a mode off does not turn the other one on.

## Opting in

An operator (or the console, or the world owner in singleplayer) runs the command below, or
chooses **Enable team tracking** in the window that asks a new world about telemetry (see
[TELEMETRY.md](TELEMETRY.md#consent)). Either way the world counts as asked.

```
/extranerfteam on [player]
/extranerfteam status
/extranerfteam off
```

- The team is named after its **leader**: `<leader name>'s team`. In game, `on` without a
  player name selects the player running it; an operator may also name an online player.
  From the console, an online player's name is required: `extranerfteam on <player>`.
- The first `on` creates a permanent **team ID** (a random UUID), saved in the world. It never changes, also
  across `off` and `on` and restarts of that world.
- Running `on` again selects the specified player, or the in-game sender when no player is
  specified, as leader and keeps the ID.

## Telling players and opting out

When team tracking is enabled, all connected players receive a chat notice with a link to the
Team Tracking Policy and a separate notice explaining their right to object. The same notices are
shown to players who join while tracking is on. Older versions may not notify players who were
already connected when tracking was enabled and may not mention the right to object.

`/extranerfteam off` stops the reports. To request deletion of the stored data, email the team ID
from `gtnhextranerf-team-id.txt` in the world's folder to `extranerf@kuba6000.pl`. The ID is not
shown in chat, and the file remains available after tracking is turned off.

## What is saved and sent

The values are saved in the world (`TelemetryConsent`): whether it is on, the team ID, the
leader's UUID and `leaderName`. A team that older versions saved in the configuration file is moved into the
first world that is loaded. The leader's name is refreshed whenever the
leader is online.

The first report is sent after about 10 minutes and later reports every 15 minutes.
It contains the team ID, the leader's Minecraft name and UUID, and the names and UUIDs
of up to 256 online players. It also includes the progression tier, world type and an
optional build proof. Team reports do not include the session ID used for basic statistics.

Team reports can contribute to aggregate server statistics. See the
[team tracking policy](https://extranerf.kuba6000.pl/team-tracking) for details.
Development environments never report.

A world that earned a certificate above Steam before its overworld had run 5 hours never reports, as for
telemetry (see [TELEMETRY.md](TELEMETRY.md)): team tracking stops with a log line and `/extranerfteam status` reports that it is not sending.

## Tests

```powershell
.\gradlew.bat test --tests '*teams*'
```
