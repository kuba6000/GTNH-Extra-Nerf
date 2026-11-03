# Building and running

## Clone with the submodule

The GTNHGradle fork is required by `settings.gradle`:

```bash
git clone --recurse-submodules <repository-url>
```

For an existing clone:

```bash
git submodule update --init --recursive
```

## Compile and verify

Windows:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
.\gradlew.bat check
```

Linux/macOS:

```bash
./gradlew compileJava
./gradlew test
./gradlew check
```

`check` is the recommended validation command. It runs unit tests, Checkstyle
and Spotless verification.

To apply formatting:

```bash
./gradlew spotlessApply
```

To create JARs:

```bash
./gradlew build
```

Artifacts are written to `build/libs`. The non-`-dev` JAR is the reobfuscated
runtime artifact.

Development environments do not send telemetry. Packaged builds report even if their JAR cannot be read. Tagged release
JARs report the release version and use the exact published artifact's hash for encryption. Release download notes describe
the telemetry and how to opt out.
We'd appreciate you keeping telemetry enabled. It is used only for community
statistics about active servers, online players, and world progression.
Read our [Privacy & Telemetry Policy](https://extranerf.kuba6000.pl/privacy).

To generate its release descriptor, run `telemetryReleaseInfo` at a
clean release tag, or supply `-PtelemetryReleaseTag=YOUR_TEST_TAG` and the matching
`VERSION` environment override for a local test. The output is
`build/telemetry/release.json` with `modVersion`, `jarSha256` and `artifact`. See
[Telemetry](TELEMETRY.md) for commands and end-to-end testing.

## Reduced development client

Use `./gradlew runClient` or a Java-specific task such as `runClient25` for the
deobfuscated development runtime. Its default EnhancedLootBags configuration is
a sample, so it may lack GTNH groups 37/38. A known loot-table validation mismatch
is logged and remains FAILED in the audit, but does not stop development startup.
Native editor changes are rolled back and lootbag opening is unavailable in
integrated development worlds for that session. Other development work can proceed.
To test lootbag behavior, use matching GTNH tables and restart the client, or use
the complete pack. The mod does not overwrite the local XML.

Late-tier certification fee materials can also be absent in the reduced runtime
(for example the Infinity block required by UIV). This remains a FAILED audit,
but development startup continues with the remaining registry/recipe checks.
An unavailable fee cannot be paid and its NEI page displays a missing-material
message rather than dereferencing a missing item.

Production/full-pack launches still fail on missing fees or loot validation
errors. Unexpected programming errors and failed rollback remain fatal even
in development.

Brainrotter substitutes vanilla structure blocks for missing mods only in
development, and skips the pedestal mechanic only when its block is absent.
See [the placeholder list](BRAINROT_MATERIALS.md#reduced-development-runtime).
The reduced-runtime fixture runs through Horizon-QA:

```powershell
python .github/scripts/run_horizon_tests.py --client-task :runClient
```

## Run in a complete GTNH environment

```bash
./gradlew runFullPack
```

or for a dedicated server:

```bash
./gradlew runFullPackServer
```

The server task does not accept the Minecraft EULA for normal local runs. Read
and accept the EULA in the prepared runtime before expecting the server to
start. A local run keeps the accepted `eula.txt` and the world; only the CI
path (`-PfullPackCi`) starts from an empty directory. A new runtime directory
appears whenever the downloaded pack manifest changes, and needs the EULA again.

Full-pack preparation downloads a moving daily manifest by default, so it may
take a significant amount of time on the first run and may test a newer pack
snapshot than an earlier local build.

GTNHGradle supplies JVM Downgrader API (Java 17 variant) on the client's
bootstrap classpath. Angelica's RFB plugin uses its classes before ordinary mod
dependency loading; downloading the API through a mod dependency loader alone
does not make it visible to the RFB system class loader. The dependency is
managed by GTNHGradle and is not bundled in Extra Nerf. No project-local
bootstrap dependency configuration is needed.

## Run the server-harness tests

```bash
python .github/scripts/test_run_full_pack_server.py
```

These tests do not launch Minecraft. They verify timeout, clean shutdown,
audit-failure and crash-evidence behavior using fake Gradle processes.

Use `python -X utf8 .github/scripts/run_full_pack_server.py --gradle-command .\gradlew.bat`
on Windows when running the real full-pack harness. Some mod log messages cannot be
encoded using the default Windows console code page. Native integration checks run through Horizon-QA:

```powershell
python .github/scripts/run_horizon_tests.py
python .github/scripts/run_horizon_tests.py --tests gtnhextranerftests:NativeScenarios.loot
python .github/scripts/run_horizon_tests.py --tests gtnhextranerftests:ArcaneWorkbenchScenarios
```

The runner builds the pinned framework from Horizon-QA PR 263, creates a scratch
world, executes the selected scenarios, verifies JSON/JUnit reports and ends its
own client. It retains evidence under `build/horizonqa`. Use `--report-dir` with a
new directory to choose another location and `--timeout-seconds` for a deadline.

The loot scenario controls ELB's item RNG and the world's budget RNG for
repeatable right-click checks, reload, cache rebuilding and ledger reopen.
The GUI scenarios check actual workbench input and inventory synchronization.

## Test the included GTNHGradle fork

Only needed when changing the submodule:

```bash
cd gradle/GTNHGradle
./gradlew test
./gradlew functionalTest
```

## Preview the Extra Nerf error screen

Use `./gradlew runClient -PforceAuditErrorScreen=true` for a dev-client preview.
For IDE runs, add `-Dgtnhextranerf.forceAuditErrorScreen=true` to the Minecraft
JVM options. The flag is ignored outside deobfuscated development clients.
See [Audit system](AUDIT_SYSTEM.md#client-startup-error-screen).
