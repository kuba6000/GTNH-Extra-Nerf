# Build, dependencies and tests

## Toolchains

| Layer | Version or mode |
| --- | --- |
| Minecraft | 1.7.10 |
| Forge | 10.13.4.1614 |
| Gradle wrapper | 9.7.1 |
| Gradle daemon | Adoptium Java 25 |
| Mod bytecode compatibility | Java 8 |
| Modern source syntax | Jabel mode through GTNHGradle |
| Mixins | GTNH Mixin 0.8.5 minimum in JSON; AP currently reports 0.8.7 |
| Unit tests | JUnit Jupiter 6.1.3, compiled and run on Java 17 |

The wrapper may discover multiple local JDKs. The daemon criteria file asks
Gradle to provision/use Adoptium Java 25. GTNHGradle configures the actual mod
toolchains and Java 8-compatible output.

## Pinned root dependencies

`dependencies.gradle` declares:

- `com.github.GTNewHorizons:GT5-Unofficial:5.09.54.261:dev`;
- `com.github.GTNewHorizons:NewHorizonsCoreMod:2.9.99:dev`;
- EnhancedLootBags `1.3.5:dev` as an implementation dependency;
- BetterQuesting `3.8.92-GTNH:dev`, Chisel `2.17.34-GTNH:dev`, Extra Utilities `2264384`, Custom Main Menu
  `1.14.1:dev` and ChromaticTooltips `1.0.36-GTNH:dev` for compilation;
- NotEnoughItems `2.8.171-GTNH:dev` for non-published runtime development;
- JUnit Platform/Jupiter 6.1.3, Objenesis 3.6, fastutil 8.5.19 and LWJGL 2.9.3
  for tests.

The mod is designed for the complete GTNH environment. Direct class
references in source exceed the three Forge requirements currently declared
by `@Mod`.

## Common commands

Windows PowerShell:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
.\gradlew.bat check
.\gradlew.bat spotlessApply
.\gradlew.bat build
```

Unix-like shell:

```bash
./gradlew compileJava
./gradlew test
./gradlew check
./gradlew spotlessApply
./gradlew build
```

Use `check` as the normal pre-commit gate. It runs unit tests, Checkstyle and
Spotless checks.

## Automated integration scenarios

Run the full client suite from the repository root:

```powershell
python .github/scripts/run_horizon_tests.py
```

The launcher pins Horizon-QA PR 263 at
`ad5adb0f8ccede91de4b5da2db50fc0534a4250c`. It builds the framework and portable
tools once in the Gradle dependency cache. A source checkout at the pinned
revision may be supplied with `--framework-source`. The upstream supervisor
owns process lifetimes, deadlines, classpath snapshots and JSON/JUnit
verification.

An installed shared automation SDK can be selected with
`--framework-sdk <sdkRoot>`. The launcher uses that SDK's development JAR,
runtime JAR and supervisor together.

Select a fixture or GUI scenario:

```powershell
python .github/scripts/run_horizon_tests.py --tests gtnhextranerftests:NativeScenarios.spatialAssembly
python .github/scripts/run_horizon_tests.py --tests gtnhextranerftests:NativeScenarios.partyRewards
python .github/scripts/run_horizon_tests.py --tests gtnhextranerftests:ArcaneWorkbenchScenarios
```

The loot-preview and quest-retrieval scenarios need a fresh world independent of
the native loot fixture, which spends the world's only controller allowance:

```powershell
python .github/scripts/run_horizon_tests.py --tests gtnhextranerfquesttests,gtnhextranerfloottests
```

The full-pack CI workflow runs this isolated selection after the main suite and
retains separate reports.

Use `--report-dir` with a new directory to choose where evidence is retained.
The default is a fresh directory under `build/horizonqa`. The client creates
a scratch world, executes its selected scenarios and exits automatically.
`launch-result.json` records the final supervisor exit code. JSON and JUnit
results are checked together. Exit codes are 0 for success, 1 for required
scenario failures and 2 for infrastructure or incomplete evidence.

`NativeScenarios` reports the existing recipe, machine, progression, loot and
telemetry fixtures individually. `ArcaneWorkbenchScenarios` exercises real GUI
input and verifies both server inventory and the visible client inventory.
`ConsentScenarios.promptAfterAnotherLogin` closes the consent window and posts a
second Forge login event for the same player before checking that it reopens. This
exercises the client/server UI path but does not reconnect a network client.
`NativeScenarios.teamNotice` and `NativeScenarios.teamLeader` exercise the team
command on the running server with simulated connected players. They verify player
notices and explicit leader selection without opening network client connections.

When changing a recipe, add Horizon-QA assertions for its final registered form
in every machine that offers it, including ordinary, industrial and multiblock
variants. Identify each machine's actual recipe map and verify the expected
ingredients, quantities and outputs after loading. Recipe maps can be independent
or copy and inherit recipes in unexpected ways, so check each affected map
explicitly.

The reduced development fixture uses the same runner:

```powershell
python .github/scripts/run_horizon_tests.py --client-task :runClient
```

The reduced runtime currently fails during recipe registration at the missing
UIV certification fee material, before Brainrotter recipes are registered.
Use the full-pack suite to validate Brainrotter with the complete dependency set.

The `horizonQaIntegration` build property includes fixture sources and their
resources. HQA is an explicit test dependency. Normal builds package the
production mod. `runFullPack` remains the command for manual gameplay.

For automated full-pack runs, preparation copies the assembled pack into the
supervisor-owned game directory before launching it. Test saves, options,
logs and the overlaid mod JAR belong to that invocation.
The integration isolates both `runFullPack` and `runFullPackServer` launches.
The recipe-only selector
`gtnhextranerfrecipes:BrainrotterIntegrationTests.compilation` can run through
the Horizon-QA `runServerTests` supervisor with `--server-task :runFullPackServer`,
the existing `horizonQaIntegration` and `horizonQaHome` properties, and
`--accept-eula` after the operator accepts the Minecraft EULA.

## Full-pack tasks

The included GTNHGradle fork supplies `prepareFullPackClient`,
`prepareFullPackServer`, `runFullPack` and `runFullPackServer`. They construct
GTNH runtimes with the current project artifact overlaid. Downloads are
shared in the Gradle cache. `runFullPackServer` requires the Minecraft EULA
for a normal run. The existing CI-only `fullPackCi` property prepares that
server configuration for the dedicated-server smoke check.
The root build sets `cleanServerRuntime` only for that CI path, so a local server run
keeps its accepted `eula.txt`, its world and its configuration.

The current GTNHGradle fork allocates client and server runtimes in short-code
directories under the full-pack cache. Runtimes created by the older layout are
not automatically migrated: their saves and server worlds remain at the old
paths and must be copied explicitly if they are needed in a new runtime.

## Root test suite

The Java test suite covers:

- `StartupErrorRequestTest`: dev-only preview gating, retained failure cause,
  and support version sourced from the configured constant;
- `AuditExpectationTest`: exact and family count contracts through typed expectations;
- `AuditSessionTest`: explicit outcomes, inactive definitions, nesting/time,
  thread ownership, stale handles, recoverable finish rejection and original failures;
- `RecipeEditorTest`: exact matching, retention, absence assertions, invalid
  and duplicate additions;
- `RecipeTransactionTest`: operation ordering, multi-registry preparation
  failure, stale plans, partial-write rollback and unrecoverable restoration;
- `RecipeRegistryTest`: ordered modifiers and committed audit counts;
- `RecipeChangeTest`: named output targeting, chance preservation, repeated-output
  cardinality, input alternatives, reuse and invalid declaration rejection.
- `CraftingDefinitionTest`: pattern-derived probes, independent changed bindings,
  defensive snapshots, typed ingredient reuse and invalid pattern rejection.
- `RecipeMatchTest`: builder fields, exact ingredient quantities/metadata/NBT,
  omitted versus empty fields, alternatives and immutable built signatures;
- `GeneratorEfficiencyTest` and `PhysicalCircuitItemStackHandlerTest`:
  generator/inventory behavior.

Python server-harness tests validate readiness, shutdown and failure
evidence. Run `python -m unittest discover -s .github/scripts -p "test_*.py"`.

The recipe tests use public interfaces with in-memory stores. Matcher tests
allocate upstream `GTRecipe` data fixtures through the test-only Objenesis
dependency because GT constructors require an initialized Forge proxy. Fastutil
is also supplied on the test runtime classpath for that upstream type. They do not
bootstrap Minecraft registries; the production full-pack launch checks live
adapters, recipe signatures and RFB linkage.

On 2026-09-08, the recipe refactor passed `check` and `build`, all 17 Java
tests and all 3 Python harness tests. The production JAR also reached server
`Done`, passed the Extra Nerf aggregate audit and exited with code 0 in an
isolated copy of the cached full pack with the pinned GT5 and NHCore production
JARs. The pack logged unrelated mod diagnostics, so this is evidence for
startup/recipe integration rather than a claim of an error-free full modpack.

On 2026-09-10, fluent recipe matching and the migrated rule definitions passed
the same build gates with 24 Java tests. The final JAR again passed the full-pack
recipe audit, reached `Done`, and shut down with exit code 0 and no crash reports.

## Included-build test suite

`gradle/GTNHGradle` is not fully tested by the root `check`; the root build
compiles it as an included build. Run its own commands from the submodule:

```powershell
cd gradle\GTNHGradle
.\gradlew.bat test
.\gradlew.bat functionalTest
.\gradlew.bat check
```

At GTNHGradle commit `cc98b41`, its reports contain:

- 63 successful unit tests;
- 25 successful functional tests.

## GitHub Actions

The local `release-tags.yml` builds and checks the runtime artifact, validates
its manifest tag and exact final hash, then uploads only that runtime JAR
and `release.json`. It replaces the shared release workflow's wildcard artifact
upload.

`build-and-test.yml` runs automatically for pushes and pull requests targeting
`master` or `main`. It calls the shared GTNH build-and-test reusable workflow
with `client-only: true`. Despite the historical input name, this keeps the
normal workspace setup, assembly, Gradle `build`, unit tests, Checkstyle,
Spotless and artifact upload; it only skips the incomplete standalone
`runServer` environment.

`full-pack-test.yml` is a separate manual-only workflow exposed through
GitHub's **Run workflow** button (`workflow_dispatch`). It has no push,
pull-request, label, schedule or chained-workflow trigger.

The manual full-pack server job:

1. checks out submodules;
2. installs Java 8, 17, 21 and 25;
3. validates Gradle wrappers;
4. unit-tests the Python harness;
5. prepares and runs the full-pack server;
6. waits for the normal `Done (` marker;
7. sends `stop`;
8. rejects non-zero exit, audit-integrity failures and crash reports;
9. uploads logs and crash evidence for 30 days.

## Verification boundaries

The standard unit suite does not bootstrap Minecraft/Forge registries and does
not apply mixins to live upstream classes. The full-pack client scenarios exercise live behavior and GUI synchronization.
The dedicated-server job separately checks startup compatibility.

A green compile or Mixin annotation processor result proves target signatures
can be resolved at build time, not that every ordinal or semantic assumption
is correct at runtime.

The full-pack job uses a moving daily manifest by default. It tests current
pack compatibility, but is not a bit-for-bit reproducible historical test
unless the manifest is separately pinned.

## Known build warnings

- `settings.gradle` uses two Groovy space-assignment forms deprecated for
  removal in Gradle 10.
- The included fork reports use of a deprecated Gradle API.
- GTNHGradle reports that a newer shared buildscript version exists; this
  repository intentionally uses its pinned local fork and must merge updates
  deliberately.

On 2026-09-11, crafting definitions and named machine changes passed
`spotlessApply check build` (29 Java tests), plus all 3 Python harness tests.
The production JAR passed the isolated pinned full-pack audit with 3006 recipe
changes, reached `Done`, and shut down with exit code 0 and zero audit failures.
Its SHA-256 matched the deployed smoke-test JAR. This validates recipe integration
and startup; unrelated pack diagnostics remain in the console.

The subsequent typed-ingredient revision passed the same build gates with
30 Java tests and 3 Python harness tests. Its production JAR again passed the
full-pack audit (3006 changes), reached `Done`, and exited with code 0 and zero
audit failures. The built and deployed SHA-256 values matched. Verification
covers server startup and recipe integration, not a manual client playthrough.

On 2026-09-12, the audit refactor passed `spotlessApply check build` with
39 Java tests (no failures, errors or skipped tests) and all 3 Python harness
tests. The production JAR matched the deployed smoke-test JAR by SHA-256.
The isolated full pack reported Audit PASSED: 3006 modified, 36 added, 120 removed;
zero failed/skipped/missing scopes. It reached `Done` and exited with code 0.
Pinned GT5/NHCore production JARs were reused from the previously prepared
isolated runtime because the Gradle cache no longer contained their production
artifacts. Existing unrelated full-pack/shutdown diagnostics remain. This is a
server integration check, not a dev-client launch or a controlled performance comparison.

The subsequent client startup-screen change passed `spotlessApply check build`
with 41 Java tests. `runClient -PforceAuditErrorScreen=true` was launched in an
isolated game directory; its Forge error screen was visually inspected and
showed the English GTNH 2.9.0 support text and dev-preview marker. The dev-client
Gradle task completed successfully after the window closed. A normal client
playthrough and a real incompatibility-triggered screen were not exercised.
The client-only hook was confirmed inside FML's custom-error catch boundary in
the transformed class. The production server JAR again passed the complete
audit (3006 modified, 36 added, 120 removed), reached Done and exited with code 0;
the built/deployed SHA-256 values matched. Existing unrelated pack diagnostics remain.

## Gradle verification ownership

Run one Gradle task graph at a time per checkout. Audits, library-model
preparation and Horizon QA packaging share compiled classes and JAR outputs.
Finish build verification before launching the game. Independent agent edits can
run in parallel while one owner schedules Gradle and runtime verification.

## Supported full-pack release

`runFullPack`, `runFullPackServer` and their HQA runs use the supported GTNH 2.9.0
distribution archives. Keep `GT_New_Horizons_2.9.0_Java_17-26.zip` and
`GT_New_Horizons_2.9.0_Server_Java_17-26.zip` in `~/Downloads`, or supply their
paths through `-PfullPackClientArchive` and `-PfullPackServerArchive`.
The preparation tasks verify each archive's pinned SHA-256 and produce a local
manifest for the existing full-pack installer. The client preparation places the
archive's LWJGL3ify early-classpath JAR in the launcher's expected location.
The full-pack workflow downloads the same release archives and verifies their
checksums. HQA uses its pinned automation revision.

## Iron Bath scenarios

Select `gtnhextranerftests:IronBathScenarios` with the Horizon QA runner to check
native power, material feedback and the visual furnace. The scenarios cover the
two-LV-hatch operating point, the eight-energy-hatch limit, physical
phase tapping, redstone behavior, loaded-controller pickup and output-based alloy
switching. Native mouse/keyboard actions edit the tap's amount and trigger mode,
then verify authoritative values and reopening synchronization. Captures include
cardinal and elevated structure views, the controller and the tap GUI.
The production scenario also checks the shared Oxygen limit. `neiDiagrams` opens registered product pages, follows
a Steel ingot output search and selects another carbon additive. Its captures
show the resulting NEI diagrams.
`neiStyleReference` opens the native HTGR page and the bath at the same viewport
and GUI scale for visual comparison. Select multiple scenarios with comma-separated
full names.

`eightEnergyHatchesProvidePerfectAmperageParallel` compares paid heat from two,
four and eight LV energy hatches and checks rejection of a ninth hatch.
`inputPowerAcceleratesColdAndMeltingBath` compares paid heating and melting with
two LV, MV and HV energy hatches. `coilsProvideNativeHeatDiscountAndPerfectOverclock`
compares Cupronickel, Kanthal and Nichrome at the same supplied power to check
native heat discounts and perfect overclocks.
`heldProbeSignalDoesNotFireAgainAfterTapReload` reloads the actual tap tile under
a maintained redstone signal and checks that only a new rising edge extracts
another sample.
`measurementHatchReadsLiveBathThroughOpenComputers` exercises the registered OC
component, live measurements and disconnection after structure or hatch removal.

`gtnhextranerftests:IronBathScenarios.nativeBlueprintRequiresShellCoilsAndDedicatedPorts`
checks the chimney shell, coil tiers, dedicated hatch locations and energy-hatch limit.
`gtnhextranerftests:IronBathScenarios.blueprintStructureViews` captures the native
structure from four sides using the production construction definition.

Select `gtnhextranerflabtests:IronBathLabScenarios.persistentLaboratory` to build
and save the walkable creative laboratory. It provides native debug energy
hatches, a manual station and a ProjectRed-controlled station. The scenario
requests Iron, Wrought Iron, Steel, Cast Iron and Pig Iron by consuming stock
from native inventories. It checks that a needed current alloy takes priority,
then verifies replenishment, the physical metal reserve and access to the
controller, hatches and taps.

For an interactive HQA session, a `ManualSession.Setup` entry point can call
`IronBathLabScenarios.prepareManualLaboratory(world, player)`. It uses the same
native construction, with full supplies and the STOP lever engaged. Build the
entry point against an HQA SDK containing `ManualSession` and launch it through
`runFullPack` with `-Dhorizonqa.manualSession=<selector>`. Verify sustained world
ticks with `clientAutomationEnabled=false` before handing over the session.

`gtnhextranerflabtests:IronBathLabScenarios.controllerRenderingProfile` checks
group pagination and measures native GUI CPU, GPU and allocations.
`gtnhextranerflabtests:IronBathLabScenarios.sampleDrainsWhileControllerIsStopped`
checks local sample drainage, retained RS memory and the 143 + 1 mB casting and
return boundary while the main controller is stopped.


`gtnhextranerftests:IronBathScenarios.paidRemeltingAcrossNativePowerAndCoils`
measures paid native melting through debug hatches, automatic maintenance, real
input admission and a continuous tap. Its diagnostics include exported metal,
actual Steel output, stored-heat changes, EU per ingot and native calculator
references. Cold Steel remelting provides a lower bound for the full steel chain.

The dedicated-server restart check uses two separate launches through
`runServerTests` with `--server-task :runFullPackServer`. Run
`gtnhextranerfpersistencetests:IronBathPersistenceScenarios.preparePersistedBath`
first. Then run
`gtnhextranerfpersistencetests:IronBathPersistenceScenarios.verifyPersistedBath`
with `--gradle-argument=-PhorizonQaSeedWorld=<first-report>/game/world` and a fresh
report directory. The seed hook copies the saved world into the second runtime.
The scenarios compare the native world-save checkpoint before resuming its
partially heated charges. Use `--keep-runtime-files` to retain the seed world,
`--accept-eula` after accepting Minecraft's EULA, and `--shutdown-seconds 120`
to allow the full pack to finish saving during shutdown.

The dedicated runtime reads the supported release's local `fullPack.serverManifestUrl`.

`gtnhextranerflabtests:IronBathLabScenarios.controllerRenderingProfile` compares
an empty controller with a hot, occupied maximum-coil controller at the same
window size and GUI scale. Its artifacts include both screenshots and
`controller-rendering-profile.json` with separate CPU time, GPU timer queries
and allocated bytes. The report records scope and measurement limits.
