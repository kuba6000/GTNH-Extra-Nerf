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
- Chisel `2.17.34-GTNH:dev`, Extra Utilities `2264384`, Custom Main Menu
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

## Full-pack tasks

The included GTNHGradle fork supplies:

```text
prepareFullPackClient
prepareFullPackServer
runFullPack
runFullPackServer
```

These tasks download manifests and assets and construct a complete GTNH
runtime with the current project artifact overlaid. Downloads are shared in a
Gradle cache. The root build sets `cleanServerRuntime = true`.

`runFullPackServer` does not accept the Minecraft EULA for normal users. The
CI-only `-PfullPackCi` path creates `eula.txt` and a minimal offline server
configuration after preparing the runtime.

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

At the documented snapshot, its reports contain:

- 45 successful unit tests;
- 20 successful functional tests.

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
not apply mixins to live upstream classes. The strongest behavior check is the
full-pack server job.

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
