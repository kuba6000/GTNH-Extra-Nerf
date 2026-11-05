# Project map

Paths are relative to the repository root. Generated `.gradle/`, `build/`, and
`run/` trees are intentionally excluded.

## Native loot tables and global limits

`loader/LootLoader` applies `loot/LootModule` implementations to ELB's actual
tables after deserialization. `loot/NativeLootTables` provides staged native edits
and weight verification; `loot/modules/MeControllerLootModule` validates both
controller sources and changes AE Good from four units to one.

`loot/GlobalLootLimits` checks and claims server-wide allowances. `loot/runtime/`
owns save lifecycle and observes native opening/spawning. `loot/storage/` retains
v1 durable claims. Remaining value classes exist for ledger payloads; there is no
separate XML importer, catalog or random planner.

`NativeLootTablesTest`, `GlobalLootLimitsTest`, `FileLootLedgerTest` cover the public
boundaries. Opt-in `LootOpeningIntegrationTests` exercises native right-clicks,
reload/cache invalidation and persistence on the full pack; it is excluded from
ordinary JARs. `loot/runtime/LootAllowanceSync`, `client/nei/LootAllowancePreview`
and `mixins/late/MixinLootBagPreview` synchronize and display the world allowance
without changing ELB selection. `LootPreviewScenarios` exercises the live GUI.

## Spatial Assembly Lines

`mixins/late/MixinAssemblyLineDimension` adds the hidden Spatial-world structure
condition to normal and Advanced Assembly Lines. Startup verification lives in
`machines/ProgressionMachineNerf.verifySpatialAssemblyLines`; the opt-in
`SpatialAssemblyIntegrationTests` uses native AE2 cells and GT server tiles.

## Questbook crafting tasks

`progression/quest/CraftingTaskRetrieval` uses native BetterQuesting retrieval
with separate progress. `MixinCraftingTaskRetrieval` adds inventory and
observation behavior alongside crafting; `MixinCraftingTaskRetrievalGui` shows
the combined progress. Integration scenarios exercise loaded quests, item
matching, hand Chisel, non-consumption and questbook reload.

## Void Miner gas costs

`mixins/late/MixinVoidMinerFluids` owns the three-tier/four-gas consumption
table. It modifies only the native gas availability and deduction amounts.
`machines/ProgressionMachineNerf.verifyVoidMinerFluids` audits registrations,
hook presence and native gas ordering. `VoidMinerFluidIntegrationTests` uses
formed server miners, powered output, hatch balances and batch processing.

## Lightning Rod generation

Lightning Rod balancing lives in `mixins/late/MixinLightningRod`. Its native
startup verification is in `machines/ProgressionMachineNerf.verifyLightningRods`;
the opt-in `LightningRodIntegrationTests` exercises actual server tiles, weather,
fence structures and generated energy through the public post-tick API.

## Root

| Path | Responsibility |
| --- | --- |
| `README.md` | Concise public entry point. |
| `CHANGES.md` | Current-change summary; canonical detail is in `docs/NERFS.md`. |
| `AI/` | Agent and maintainer source-of-truth documentation. |
| `docs/` | Human-facing project documentation. |
| `build.gradle` | Applies the GTNH convention plugin, configures full-pack CI tasks and the `telemetryReleaseInfo` release descriptor. |
| `settings.gradle` | Includes the local GTNHGradle build and configures plugin repositories. |
| `dependencies.gradle` | Pinned mod and test dependencies; enables JUnit Platform. |
| `repositories.gradle` | Empty extension point for additional repositories. |
| `gradle.properties` | Mod identity, Minecraft/Forge versions, mixin/coremod and Gradle settings. |
| `gradlew`, `gradlew.bat`, `gradle/wrapper/` | Gradle 9.7.1 wrapper. |
| `gradle/gradle-daemon-jvm.properties` | Requests an Adoptium Java 25 Gradle daemon. |
| `.gitmodules` | Pins `gradle/GTNHGradle` as a Git submodule. |
| `.editorconfig`, `.gitattributes`, `.gitignore` | Repository formatting and generated-file policy. |
| `CODEOWNERS` | Requires `@kuba6000` ownership for `.github/**`. |
| `LICENSE` | LGPL-3.0-or-later license notice. |

## Core mod classes

| File | Responsibility |
| --- | --- |
| `src/main/java/pl/kuba6000/gtnhextranerf/GTNHExtraNerf.java` | Forge `@Mod` entry point and lifecycle delegation. |
| `.../CommonProxy.java` | Pre-init configuration/component registration and load-complete patch/audit orchestration. |
| `.../ClientProxy.java` | Component renderer registration and client startup-error handling. |
| `.../components/ComponentType.java` | Stable component ids and lazy UEV appearance references. |
| `.../components/ComponentTier.java` | Cosmetic tier ids, abbreviations and appearance policies. |
| `.../components/ComponentRegistry.java` | Complete catalog and fresh stack API. |
| `.../components/ComponentItem.java` | Independent component identity, localized names/tooltips and borrowed vanilla appearance. |
| `.../loader/ComponentLoader.java` | Shared catalog and Forge pre-init registration. |
| `.../client/ComponentItemRenderer.java` | Borrowed custom rendering, including GregTech material effects. |
| `.../client/nei/NEIExtraNerfConfig.java` | Hides all component metadata/NBT variants from NEI item search. |
| `.../client/nei/UfoAssemblerRecipeHandler.java` | One UEV-to-UQV fictional blueprint per component, UFO icon/beam and item-query routing. |
| `.../fabrication/FabricationIngredient.java` | Explicit stable fictional ingredient identities and component mapping. |
| `.../fabrication/FabricationItem.java` | Inert items with owned pixel textures and localized joke tooltips. |
| `.../fabrication/UfoAssemblyRegistry.java` | Shared UFO/ingredient catalog and fresh-stack/lookup API. |
| `.../Config.java` | Opens the suggested Forge configuration and saves only when changed. |
| `.../items/HungryAxe.java` | Hungry Axe mark, hunger drain and translated display name. |
| `.../items/BlightedSoybean.java` | Registered replacement for HarvestCraft soybean crop drops. |
| `.../items/MagicnetizedRods.java` | Registered magicnetized motor rods, arcane recipe and crafting remainders. |
| `.../audit/AuditScope.java` | Typed semantic patch identities, activation conditions and expected statistics. |
| `.../audit/AuditManager.java` | Scoped exclusive-time/statistic collection, success reporting, final verification and lifecycle cleanup. |
| `.../audit/AuditDefinition.java` | State-free id, activation and expectation contract. |
| `.../audit/AuditExpectation.java` | Typed exact/family count contracts and descriptions. |
| `.../audit/AuditSession.java` | Independent lifecycle, nesting, outcome collection and finalization. |
| `.../audit/AuditHandle.java` | One-use, thread-owned execution handle. |
| `.../audit/AuditCounts.java` | Immutable operation counts. |
| `.../audit/AuditResult.java` | Immutable per-scope outcomes and validation. |
| `.../audit/AuditReport.java` | Retained complete report and diagnostics. |
| `.../audit/AuditStatus.java` | Success, failure, skipped and missing states. |

## Telemetry, team tracking and update check

All paths start at `src/main/java/pl/kuba6000/gtnhextranerf/`. The reporting modes are mutually
exclusive; the update check is independent.
The mod only sends reports and reads one version file. See `docs/TELEMETRY.md`, `docs/TEAM_TRACKING.md` and
`docs/UPDATE_CHECK.md`.

| File | Responsibility |
| --- | --- |
| `net/SecureEndpoint.java` | Outbound traffic is HTTPS only; plain HTTP is accepted solely for a loopback test endpoint. |
| `telemetry/ServerTelemetry.java` | One logical-server session: captures the snapshot on the server thread, schedules reports, join notice. |
| `telemetry/TelemetryClient.java` | Outgoing telemetry transport and optional build information; owned by the telemetry worker. |
| `telemetry/TelemetryCommand.java` | `/extranerftelemetry on\|off\|status` for the owner. |
| `telemetry/TelemetryConsent.java` | Per-world record, immediately persisted atomically; preserves unreadable records, migrates old configuration and keeps a readable team-ID file. |
| `telemetry/ConsentPrompt.java`, `ConsentGui.java`, `ConsentSyncHandler.java` | The question: operator window (MUI2), console notice, server-side check and application of the answer. |
| `teams/TeamTracking.java` | Opt-in team report every 15 minutes, explicit leader, player list and player notices. |
| `teams/TeamClient.java` | Encodes and posts one team report. |
| `teams/TeamCommand.java` | `/extranerfteam on\|off\|status`. |
| `progression/TelemetryEligibility.java` | Turns reporting off for worlds that earn a certificate above Steam too early. |
| `update/UpdateChecker.java` | Reads the published version at startup and every twelve hours. |
| `update/ReleaseVersion.java` | Compares dotted release tags; development builds and prereleases never compare. |

Tests: `src/test/java/.../telemetry/` (client packet vector, HTTP headers and redirects, artifact proof,
lifecycle, command), `.../teams/TeamTrackingTest.java`, `.../teams/TeamCommandTest.java`, `.../update/` and `.../progression/TelemetryEligibilityTest.java`.
The opt-in `TelemetryIntegrationTests` run on the full pack.

## Load-complete recipe layer

All paths below start at
`src/main/java/pl/kuba6000/gtnhextranerf/loader/`.

| File | Responsibility |
| --- | --- |
| `RecipeLoader.java` | Explicitly registers recipe modules, then executes `RecipeRegistry`. |
| `../recipes/RecipeModule.java`, `RecipeModifier.java`, `RecipeRegistry.java` | Public registration contracts, ordered execution, activation and audit ownership. |
| `../recipes/RecipeContext.java`, `RecipeEditor.java`, `RecipeTransaction.java`, `RecipeStore.java` | Shared editing API, staged changes and rollback across registries. |
| `../recipes/machine/` | Exact signatures, metadata-preserving copies, map emitter integration and indexed registry writes. |
| `../recipes/machine/RecipeChange.java` | Named changes by item/fluid identity with chance and alternative handling. |
| `../recipes/crafting/CraftingRecipeBuilder.java` | Typed pattern, ingredient, output and flag authoring. |
| `../recipes/crafting/CraftingIngredient.java` | Immutable ingredient interface with a typed visitor conversion boundary. |
| `../recipes/crafting/ItemCraftingIngredient.java` | Defensive copies of exact item ingredients. |
| `../recipes/crafting/OreCraftingIngredient.java` | OreDict name preservation and lazy probe resolution. |
| `../recipes/crafting/CraftingDefinition.java` | Immutable description shared by matching and construction. |
| `../recipes/machine/RecipeMatch.java` | Public fluent builder for immutable recipe signatures and alternatives. |
| `../recipes/RecipeItems.java` | Shared required mod-item lookup; missing registry entries raise `RecipeValidationException`. |
| `../recipes/crafting/` | Typed targets/handles, direct factories and Minecraft registry adapter. |
| `recipes/CraftingNerf.java` | Bronze Dust, Large Chemical Reactor, LV Circuit Assembler, Good Circuit, flint-and-steel and Tool Station crafting recipes. |
| `recipes/AluminiumNerf.java` | Ilmenite chemical baths, Ruby Juice chain and Ilmenite Slag centrifuge. |
| `recipes/BlastFurnaceNerf.java` | Primitive and electric Steel, Aluminium, TConstruct aluminium, ilmenite and titanium EBF recipes, plus corresponding Alloy Blast Smelter routes. |
| `recipes/ChemicalAndMiscNerf.java` | Chemical, silicon cutting, Lubricant, platinum, samarium and miscellaneous processing changes. |
| `recipes/CoreModRecipeNerf.java` | NHCore/GT recipe changes for low-tier circuits, Cupronickel Coils, MV Energy Hatches, casings, indium, endgame and components. |
| `recipes/IntergalacticRecipeNerf.java` | Audited UQV Sensor replacement in the Crafting Input Proxy's Space Assembler recipe. |
| `recipes/SlimeRecipeNerf.java` | Removes slime processing and replaces selected slime crafting/assembler routes with EMT Glue. |

## Mixin infrastructure

All paths below start at
`src/main/java/pl/kuba6000/gtnhextranerf/mixins/`.

| File | Responsibility |
| --- | --- |
| `../ExtraNerfCore.java` | FML core plugin and early mixin selector. |
| `../ExtraNerfLateMixinLoader.java` | GTNH late-mixin selector. |
| `Mixin.java` | `MixinBuilder` registry and phase/mod selection. |
| `TargetedMod.java` | Mod identity model used by `Mixin`; some declared entries are currently unused. |
| `support/GeneratorEfficiency.java` | Shared tier-relative efficiency curve for generator mixins. |
| `support/SteamConsumption.java` | Shared fivefold steam multiplier and single-block conversion. |
| `support/PhysicalCircuitItemStackHandler.java` | One real Integrated Circuit mapped to a machine's reserved circuit slot. |
| `support/PhysicalCircuitSlots.java` | Shared tier and reserved-slot eligibility for physical circuits and their drops. |
| `support/ProcessingWireRecipeTransformer.java` | Semantic transformer shared by the Processing Wire receiver mixin. |

## Early mixins

| File | Upstream target | Change |
| --- | --- | --- |
| `mixins/early/MixinCraftingManagerAccessor.java` | Minecraft `CraftingManager` | Descriptor-safe raw recipe-list access. |
| `mixins/early/MixinRecipeAccessor.java` | Minecraft `IRecipe` | Descriptor-safe output and matching invokers. |
| `mixins/early/MixinShapedOreRecipeAccessor.java` | Forge `ShapedOreRecipe` | Direct access to the private mirroring flag. |
| `mixins/early/MixinSmallOres.java` | `gregtech.api.enums.SmallOres` | Naquadah small-ore amount. |
| `mixins/early/MixinLoaderGTBlockFluid.java` | `gregtech.loaders.preload.LoaderGTBlockFluid` | Glowstone breeder pulse and nuclear-cell lifetime. |

## Late mixins

| File | Upstream target | Change |
| --- | --- | --- |
| `mixins/late/MixinMTESteamTurbine.java` | `MTESteamTurbine` | Efficiency and description. |
| `mixins/late/MixinMTEPlasmaGenerator.java` | `MTEPlasmaGenerator` | Efficiency. |
| `mixins/late/MixinSteamOverclockDescriber.java` | `SteamOverclockDescriber` | EU-to-steam conversion. |
| `mixins/late/MixinSteamInputHatch.java` | `GregtechCustomHatches` | Steam Input Hatch capacity. |
| `mixins/late/MixinSteamMachineTank.java` | `MTEBasicMachineBronze` | Steam tank amount and capacity. |
| `mixins/late/MixinSteamMachineDisplay.java` | `MTEBasicMachine` | Single-block steam usage in WAILA. |
| `mixins/late/MixinSteamMachineGui.java` | `MTEBasicMachineBronzeGui` | ModularUI 2 steam gauge values. |
| `mixins/late/MixinSteamMultiBlock.java` | `MTESteamMultiBlockBase` | Steam consumption and WAILA, excluding the Water Pump. |
| `mixins/late/MixinSteamMultiBlockWailaData.java` | `MTEExtendedPowerMultiBlockBase` | Integer-accurate WAILA data for steam multiblocks. |
| `mixins/late/MixinMTEDieselGenerator.java` | `MTEDieselGenerator` | Efficiency cap. |
| `mixins/late/MixinMTEGasTurbine.java` | `MTEGasTurbine` | Efficiency cap. |
| `mixins/late/MixinMTELargeChemicalReactor.java` | `MTELargeChemicalReactor` | 3/4 overclock and tooltip. |
| `mixins/late/MixinHealingAxeHunger.java` | Extra Utilities `ItemHealingAxe` | Marks the carried axe, changes its displayed name and replaces hunger gain with drain. |
| `mixins/late/MixinSoybeanCrop.java` | HarvestCraft `BlockPamCrop` | Replaces soybean item returns from crop and seed methods. |
| `mixins/late/MixinArcaneCraftingRemainders.java` | Thaumcraft `SlotCraftingArcaneWorkbench` | Returns the flower and demagnetized rods when crafting the MV ingredient. |
| `mixins/late/MixinMetalTransmutation.java` | Witching Gadgets `WG_alchemic_clusters` | Modifies GT-generated metal transmutation recipes during registration. |
| `mixins/late/MixinMetalResearchText.java` | Witching Gadgets `WGResearch` | Uses text describing the resulting metal dusts. |
| `mixins/late/MixinIronToSteelTransmutation.java` | GregTech `ThaumcraftRecipes` | Changes the iron-to-steel crucible recipe during registration. |
| `recipes/crafting/ArcaneWorkbenchSync.java` | Server tick event | Sends final workbench inventories after the crafting transaction. |
| `mixins/late/MixinMetaTileEntity.java` | `MetaTileEntity` | Steam fill conversion. |
| `mixins/late/MixinConfigurationCircuitSupport.java` | `CommonWidgets.createCircuitSlot` | Physical Integrated Circuit slot below HV for every tiered machine with configuration-circuit support. |
| `mixins/late/MixinPhysicalCircuitBlockDrop.java` | `BlockMachines.breakBlock` | Include the reserved physical circuit in ordinary inventory drops. |
| `mixins/late/MixinPhysicalCircuitExplosionDrop.java` | `BaseMetaTileEntity.doExplosion` | Include the reserved physical circuit in native protected explosion drops. |
| `mixins/late/MixinCraftingMaterialLoader.java` | BartWorks `CraftingMaterialLoader` | Workstoff ring crafting. |
| `mixins/late/MixinMTESemiFluidGenerator.java` | GT++ `MTESemiFluidGenerator` | Efficiency. |
| `mixins/late/MixinProcessingWire.java` | GregTech `ProcessingWire` | PPS foil in five cable recipe families. |
| `mixins/late/MixinCircuitAssemblerRecipes.java` | Dreamcraft `CircuitAssemblerRecipes` | Sixfold nano/quantum processor inputs, capped at 64 per stack. |
| `mixins/late/MixinMetaGeneratedItem01.java` | `MetaGeneratedItem01` | LV motor crafting routes. |
| `mixins/late/MixinMaterialsInit.java` | `MaterialsInit` | Benzene and phenol fuel values. |
| `mixins/late/MixinProcessingShaping.java` | `ProcessingShaping` | GregTech ring crafting. |
| `mixins/late/MixinAssemblyLineRecipes.java` | `AssemblyLineRecipes` | Dense Netherite amount. |

## Resources

| File | Responsibility |
| --- | --- |
| `src/main/resources/mcmod.info` | Forge display metadata; dependency fields are currently unused. |
| `src/main/resources/mixins.gtnhextranerf.json` | Root mixin config and shared refmap declaration. |
| `src/main/resources/mixins.gtnhextranerf.early.json` | Required early mixin config. |
| `src/main/resources/mixins.gtnhextranerf.late.json` | Required late mixin config. |
| `src/main/resources/assets/gtnhextranerf/textures/items/blighted_soybean.png` | Blighted Soybean item texture. |
| `src/main/resources/assets/gtnhextranerf/textures/items/magicnetized_manasteel_rod.png` | Magicnetized Manasteel Rod texture. |
| `src/main/resources/LICENSE` | License included in the built JAR. |

## Tests

| File | Scope |
| --- | --- |
| `src/test/java/.../recipes/RecipeEditorTest.java` | Exact targets, retention, absence, duplicate/invalid additions. |
| `src/test/java/.../recipes/RecipeTransactionTest.java` | Staged operations, failures across maps, stale plans and rollback. |
| `src/test/java/.../recipes/RecipeRegistryTest.java` | Registration order and committed audit counts. |
| `src/test/java/.../recipes/RecipeMatchTest.java` | Fluent matching, exact ingredients, alternatives and defensive snapshots. |
| `src/test/java/.../mixins/support/PhysicalCircuitItemStackHandlerTest.java` | Physical Integrated Circuit insertion, extraction and validation. |
| `src/integrationTest/java/.../IndustrialRecipeIntegrationTests.java` | Final machine lookup and parity across the affected ordinary and industrial maps. |
| `src/integrationTest/java/.../AlloyBlastSmelterIntegrationTests.java` | Steel and Aluminium EBF/Alloy Blast Smelter lookup and removed gas routes. |
| `src/integrationTest/java/.../PhysicalCircuitDropIntegrationTests.java` | Physical circuit and ordinary inventory drops from real machines and input buses. |
| `src/integrationTest/java/.../MetalTransmutationIntegrationTests.java` | Crucible behavior and generated metal research recipes. |
| `src/integrationTest/java/.../MetalTransmutationNeiScenarios.java` | Metal transmutation outputs, inputs and aspects in NEI. |
| `src/integrationTest/java/.../HealingAxeIntegrationTests.java` | Server-side player inventory ticks with the Extra Utilities axe, run by `NativeScenarios.healingAxe`. |
| `src/test/java/.../mixins/support/GeneratorEfficiencyTest.java` | Standard and EV-relative generator efficiency curves. |

## CI and auxiliary tooling

| File | Responsibility |
| --- | --- |
| `.github/workflows/build-and-test.yml` | Automatic shared GTNH build, unit tests and style checks with standalone `runServer` disabled. |
| `.github/workflows/full-pack-test.yml` | Manual complete-modpack server compatibility test. |
| `.github/workflows/release-tags.yml` | Verifies and publishes only the tagged runtime JAR and its final release descriptor. |
| `.github/scripts/run_full_pack_server.py` | Starts, waits for, stops and validates a full GTNH server. |
| `.github/scripts/test_run_full_pack_server.py` | Unit tests the server harness with fake Gradle processes. |
| `.github/scripts/telemetry_release_metadata.py` | Validates the final JAR and writes release metadata and the release-notes disclosure. |
| `.github/scripts/test_telemetry_release_metadata.py` | Unit tests the release descriptor CLI. |

## Included GTNHGradle fork

`gradle/GTNHGradle` is a separate repository pinned at commit `94fb3ef`. The
root build consumes it with `includeBuild`. Its project-specific additions
provide:

- full-pack client and server manifests;
- content-addressed asset caching;
- safe ZIP extraction and runtime materialization;
- local project/dependency overlays;
- `prepareFullPackClient`, `prepareFullPackServer`, `runFullPack`, and
  `runFullPackServer` tasks.

Consult `gradle/GTNHGradle/README.MD` and its own `docs/` directory before
editing the submodule.

Additional public API tests: `RecipeChangeTest` covers named edits and guarded
cardinality; `CraftingDefinitionTest` covers pattern-derived independent probes.

`RecipeFailurePolicy` configures registry handling of known
`RecipeValidationException` failures. Audit lifecycle tests are under
`src/test/java/.../audit/`; registry tests additionally prove development
continuation preserves failed results and rejects programmer exceptions.

Client error presentation: `client/StartupErrorRequest.java` holds the support
message and preview selection; `ClientStartupErrors.java` defers the request;
`StartupErrorDisplay.java` draws on Forge's error screen. The client-only early
`MixinFMLClientHandler.java` raises the request inside FML's catch boundary.

## Automated integration tests

`src/integrationTest` contains Horizon-QA holders, native fixtures and structure
templates. `NativeScenarios` reports native checks individually.
`ArcaneWorkbenchScenarios` owns GUI crafting and inventory synchronization tests.
`IntegrationFixtures` supplies pre-init recipe alternatives.

`gradle/horizon-tests.gradle` includes this source tree for HQA builds.
`.github/scripts/run_horizon_tests.py` prepares the pinned PR dependency and uses
its portable supervisor to launch, verify reports and finish the owned client.
See `BUILD_AND_TESTS.md` for selectors and commands.
