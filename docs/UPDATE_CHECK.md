# Update check

The mod tells operators when a newer release is published. It is separate from
[telemetry](TELEMETRY.md) and has no setting of its own.

## What the mod does

- At server start (including singleplayer) and then every 12 hours, a daemon worker
  checks `https://extranerf.kuba6000.pl/version.json`, which returns the published
  version in the form `{"version":"1.2.3"}`. There are 5-second timeouts, no redirects,
  no retry queue and a 1 KiB answer limit.
- The mod knows its own release tag (`Tags.VERSION`, which CI forces to equal the release tag).
- Only clean release tags (dotted numbers such as `0.0.7`) are compared, component by
  component. Development builds (`0.0.6-86-g02984ed`) and prereleases never check.
- When the published tag is newer, the console logs one warning per detected version and
  every operator gets a chat message when joining. In singleplayer the world owner gets it
  without cheats. Other players are not told.
- Network errors, bad answers and a missing release stay silent; an unusable answer keeps
  what was already known.

The worker only reads the file. The chat message is sent on the server thread when a
player joins.

Tagged releases include the runtime JAR and a generated `release.json`. Upload both to
the website's `downloads` directory; the website then serves `/version.json` from that
release metadata, so there is no separate version file to upload.

## Tests

```powershell
.\gradlew.bat test --tests '*update*'
```
