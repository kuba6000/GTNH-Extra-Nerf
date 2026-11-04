# Technical patch catalog

This is the exhaustive technical catalog for the current source snapshot. It
records implemented behavior, not historical intent. Player-facing wording is
in [`../docs/NERFS.md`](../docs/NERFS.md).

## Blighted Soybean

When HarvestCraft is loaded, `MixinSoybeanCrop` modifies the crop and seed
return values of `BlockPamCrop`. It substitutes Blighted Soybean only when the
original item is `harvestcraft:soybeanItem`; a distinct seed item is not
substituted. The two target methods require at least two return matches in
total. `BlightedSoybean` is registered during common pre-init, is not edible
and has no recipe registered by this mod. NEI hides it from its item panel.
The complete-server startup checks mixin application; the existing integration
suite does not separately exercise soybean crop drops.

## Magicnetized Manasteel Rod

`MagicnetizedManasteelRod` registers an arcane recipe using a Manasteel Rod,
two Magnetic Steel Rods, a Solegnolia, 10 Aer and 10 Ordo. The workbench
keeps the flower in the workbench and converts the Magnetic Steel Rods to Steel Rods.
`ArcaneWorkbenchSync` sends the final Shift-click inventory at server tick END.
`CraftingNerf` and `CoreModRecipeNerf` replace the
Magnetic Steel Rod in the MV motor's hand and Assembler recipes. The COAL
recipe uses Long Magnetic Steel Rods.

`CommonProxy` registers the item in pre-init and its arcane recipe during
load-complete. `MixinArcaneCraftingRemainders` handles the rod and flower
remainders and requests the final inventory snapshot. `LvRecipeIntegrationTests`
checks the recipes, arcane ingredients and remainder helper.
`ArcaneWorkbenchScenarios` checks native workbench clicks and client inventory
synchronization.

## Audit conventions

`MixinVoidMinerFluids` replaces the old tier-based rejection with a consumption
matrix. Its strict two-site `ModifyExpressionValue` changes both reads of
`batchMultiplier` in native `consumeNobleGas`: availability and deduction.
Native insufficient-gas fallback remains unboosted and does not drain fluid.
`VERIFY_VOID_MINER_GAS_USAGE` checks native registrations, the hook and the
four-gas identity/order contract. The obsolete GUI error resource is removed.
`VoidMinerFluidIntegrationTests` covers every tier/gas pair through native
processing, powered output and input balances, including exact/short payment
and large batch costs. Relationships cover tuning without duplicating its table.

`MOVE_RUTHENIUM_TO_COOLANT_TOWER` copies the full native separation recipe
into `ProgressionMachineNerf.COOLANT_RECIPES` and removes the old DT route in
one transaction. `MachineRecipes.addCopyOne` supports unchanged cross-map
copies with destination category normalization. The map is created in preInit
before GT/NEI category and controller-catalyst enumeration. `MixinCoolantTower`
adds the map, accepts Energy/Output Bus hatches and dispatches matching fluids
through TT's normal ProcessingLogic bridge. Mandatory `protectsExcessItem`
and `protectsExcessFluid` getters are required because native output helpers
read them again; flags on ProcessingLogic alone do not enforce capacity.
The tower's long-power getter and running-tick bridge reuse native TT drain.
Its native persisted `lEUt` also determines the powered cycle after reload.
Steam dispatch clears old power and retains the original passive conversion.
`VERIFY_COOLANT_TOWER` checks the hook, map, protection and NEI registration.
Native fixtures form both passive and powered towers, reject missing bus/power
and full outputs before consuming input, and run a complete energized cycle
with all three outputs. Client visuals were not exercised by that server test.

`REMOVE_EV_CABLE_ASSEMBLER_RECIPES` identifies outputs through the registered
GT `MTECable` prototype's insulation and voltage, verifies native LATEX
production for each, then removes the complete Assembler scope transactionally.
Its minimum-removal audit allows native/addon recipe cardinality to vary,
while requiring zero additions/modifications; the operation still checks the
exact observed target count and staged absence. Cable-to-cable conversions
are rejected for scope review. Tests retain all Gold/HV widths, reject all
Aluminium/EV and Tungsten/IV widths, keep their LATEX routes and retain bare
wire production and downstream cable consumers.

`ADD_NEPTUNIUM_FLUORIDE_TO_EUROPIUM_BOULE` requires eight final EBF variants
by Europium boule output and appends two consumed Neptunium Hexafluoride
dust to each copied recipe. Native tests check missing-dust rejection, paid
lookup and consumption in every variant and retain the cutter route.

`MixinPlasmaForgeCoils` adds a translatable native error at `checkMachine`
TAIL only when the existing list is empty and coil heat is below Hypogen.
The common late mixin implements a hook marker checked by `VERIFY_PLASMA_FORGE_COILS`, which
also validates the packaged default localization. Server verification reads
the resource rather than invoking client-only text/widget rendering. Native
fixtures build the full Forge, test incomplete/low/Hypogen/Eternal states
and error clearing, and use `StructureErrorRegistry` packet serialization.

`MODIFY_MUTATED_SOLDER_STAR` replaces eight Gravistars with eight Nuclear
Stars in the exact native Chemical Plant recipe, including casing metadata.
The fixture checks consumed stars and preservation of QFT production. Native
`ProcessingLogic` with the Autoclave map/prototype produces a Nuclear Star at
4 A UHV, proving that the UEV Energy Hatch's solder input does not force a
bootstrap deadlock. Actual multiblock construction is outside that power check.

`ADD_MINERAL_MIX_TO_RAW_BIO_MEDIUM` finds exactly three Bacterial Vat recipes
by `BioMediumRaw` fluid output and appends one consumed GT++ Mineral Mix dust.
All special-slot/culture and recipe metadata are copied without changing
the Bio Vat's native fluid multiplier. Native tests use the culture-sensitive
recipe query and input consumption, including missing-dust rejection.

`MODIFY_PCB_FACTORY_WET_BIO_PLASMA` targets 12 Wetware Extreme and 9 Bioware
Ultra recipes by native output identity. Each replaces its unique Sulfuric
Acid input with the same amount of Copper Plasma using a copied recipe;
existing metadata and input/output data are retained. Tests query the final
native PCB backend with plasma and old acid, and retain Circuit Assembler
and both chemical-map board alternatives.

`MODIFY_AMERICIUM_BOULE` removes seven non-Oganesson EBF variants and adds
two consumed Protactinium dust to the one retained native Oganesson recipe.
Its signature includes all original inputs, fluid, time, EU/t and heat.
`RecipeChange.addItemInput` preserves old input alternatives/chances and gives
the appended stack guaranteed consumption. The native integration fixture
checks gas admission, missing-ingredient rejection, consumption and the
retained wafer-cutting route.

`MODIFY_HIGH_TIER_HULL_INSULATION` in `ProgressionRecipeNerf` changes six
Assembler and six hand recipes in one transaction. UV/UHV use Kevlar;
UEV-UXV use Radox Polymer. NHCore's higher hand recipes live in
`GT_Loader_Machines`, separately from casing recipes and recycling data.
`CraftingRecipes.expectOutputCount` audits the entire production output
scope, rejecting extra routes. Native recycling deliberately omits additional
hull insulation and is retained. Native fixtures verify hand crafting,
machine lookup, obsolete-polymer rejection and consumed fluid.

`AuditScope` is the only logical audit identifier. Audited mixins and every
load-complete recipe change record aggregate added, modified and removed
counts under one enum value. `AuditSession.finish()` validates all active scope
contracts at load-complete; optional Dreamcraft, TConstruct and Forestry
scopes are inactive when their owning mod is absent.

Recipe patches throw immediately on an absent, duplicate or failed target.
Their diagnostics contain the typed scope, operation and recipe map.
Production execution is fail-fast; `AuditManager.run` catches the resulting
`IllegalStateException` only in a deobfuscated development environment, logs
the skipped scope and lets later recipe patches run. Final audit gaps are also
warnings only in that environment.

## Lootbags and ME Controller

ELB 1.3.5 constructs tables from XML, then a common mixin invokes `LootLoader`
before publication. `MODIFY_ME_CONTROLLER_LOOT` requires one actual modification
(Good 4 -> 1); Basic's one-unit source is separately validated. The final
`VERIFY_NATIVE_LOOT_TABLES` scope verifies publication, native weights and item
resolution. Later reloads run both scopes in a fresh audit session. ELB clears
merged caches and owns all normal selection/delivery and NEI table data.

`MeControllerLootModule` preserves the two controller sources, their weights and
Fortune inheritance. Global allowance `gtnhextranerf:me_controller` permits one
unit per save, without a guarantee. Narrow opening hooks claim it durably before
ELB spawns the stack. The limit never alters preview tables or client packets.
Native player counts continue in YAMCore, with old journal counts retained as a
floor. Storage handles reset between saves. Existing inventories and other reward
systems are outside this allowance.

## LV machine-casing quest

Quest UUID `00000000-0000-0000-0000-000000000049`, task 0, remains a native
crafting task with its original progress format. For the exact finished LV casing,
`MixinCasingQuestCrafting` handles Detect and `MixinCasingQuestInventory` uses
BQ's existing inventory-change queue. `CasingQuest` delegates to the native
task progress method without consuming the item. Task 1 still requires crafting
the hull; other quests and item tiers retain their normal behavior. The patch
does not edit the questbook or require a special reload path.

The [questbook snapshot at `81027c5b`](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/blob/81027c5b455085a55618e3f9498b1fe0dec625c2/config/betterquesting/DefaultQuests/Quests/Tier05Steam-AAAAAAAAAAAAAAAAAAAAAg%3D%3D/BasicCraftingMac-AAAAAAAAAAAAAAAAAAAASQ%3D%3D.json#L94-L130)
contains 3,814 quests. A search for finished LV–MAX machine casing tasks found
this one crafting task and nine optional retrieval tasks; hulls are separate items.

## Fictional component catalog

`ComponentLoader` registers eight UQV (Ultimately Questionable Voltage)
components during common pre-init: motor, piston, pump, conveyor, robot arm,
sensor, emitter and field generator. Each is an independent inert item with a
stable `gtnhextranerf:uqv_<type>` registry name and no production recipe or
Ore Dictionary alias. `ComponentTier` is cosmetic. `ComponentRegistry.stack`
returns fresh stacks for recipe consumers.

Client appearance delegates to the corresponding real UEV component, including
icons, colors, atlas, passes, effects and custom material rendering.
`NEIExtraNerfConfig` hides all damage/NBT variants from
the item panel; exact ingredients remain visible inside recipes. These items
have no Creative tab. English names/tooltips are supplied.

The item tooltip contains only the short tier tagline. `UfoAssemblerRecipeHandler`
shows one NEI-only blueprint per UQV component: its UEV source and a mapped
unavailable ingredient lead to that component. `UfoAssemblyRegistry` registers
the UFO Assembly category/catalyst item and eight distinctive ingredients with
hand-authored pixel textures and joke tooltips. The page places UFO above the
ingredient with an animated cyan beam, arrows between inputs/output and the
full UQV tier name below. UFO's tooltip is Unidentified Fabrication Operation.
R/U on UQV selects one page; U on a fictional part selects its matching page;
R/U on UFO opens eight pages. UEV source usage queries do not select UFO pages;
the source remains visible as a blueprint ingredient. Generic all/category queries are rejected. All
fictional items remain hidden from search. No gameplay production recipe is
registered, and overlay/favorite buttons remain disabled.

`IntergalacticRecipeNerf` uses this catalog to replace one UEV Sensor with one
UQV Sensor in the Space Assembler's Crafting Input Proxy recipe. The patch
requires one Proxy output recipe and its exact NHCore input/fluid/duration/EU/t
signature. All other recipe data, including module-tier metadata, is retained.
Audit scope `MODIFY_CRAFTING_INPUT_PROXY_RECIPE` requires exactly one modified
recipe when GTNH Intergalactic, AE2 and AE2 Fluid Craft are loaded. Details and
extension points are in [`../docs/COMPONENTS.md`](../docs/COMPONENTS.md).

## Wireless connection power

Wireless power is changed separately by the late `MixinWirelessConnectionPower`
using `@WrapMethod` on `TileWirelessBase.setConnectionsPowerDraw`, replacing the
original calculation without calling it. The common connector/
hub calculation caps each link at 10,000,000 EU/t before assigning half its
distance^4 cost to each endpoint, and uses AE2's
configured conversion rates and power multiplier. It visits only active links
and compares squared distance with a precomputed cap threshold before taking
the fourth power. Coordinate subtraction uses doubles to avoid integer overflow.
Recalculation runs when upstream requests a power update.

## TPV Alloy mixer yield

`ChemicalAndMiscNerf.reduceTpvMixerOutput` targets the Titanium/Platinum/Vanadium
recipe with circuit 1 and seven TPV dust output in `mixerRecipes` and
`mixerNonCellRecipes`. A single transaction changes only the output amount to
three. `REDUCE_TPV_MIXER_OUTPUT` requires exactly two modifications. No global
recipe-map traversal is used.

## ME P2P channel capacity

`MixinMeP2PChannelCapacity` removes `DENSE_CAPACITY` from the ME tunnel's
external proxy at constructor return, before its node is created. AE2's standard
eight-channel node limit then applies, including the common input for multiple
outputs. Other flags, including `CANNOT_CARRY_COMPRESSED`, are retained. Ordinary
Dense Cable keeps its dense capacity. AE2's global channels-disabled mode is
respected. No recurring tick hook or startup-audit mutation is added.

The server integration fixture constructs real parts and external nodes, checks
their capacity before and after connecting two output branches, and checks a
separate Dense Cable node. It runs after world creation without placing blocks.

## Early mixins

### Crafting accessors

`MixinCraftingManagerAccessor` exposes the raw private Minecraft crafting
recipe list. `MixinRecipeAccessor` exposes `IRecipe.getRecipeOutput` and
`IRecipe.matches` through remapped invokers without putting `IRecipe` in the
load-complete patcher's descriptors. `MixinShapedOreRecipeAccessor` exposes
Forge's private `mirrored` flag so replacement recipes can disable mirroring
without reflection. They do not independently change gameplay and have no
audit scope.

### `MixinSmallOres`

- Target: `SmallOres.<clinit>` calls to `SmallOreBuilder.amount(int)`.
- Match: `builder.smallOreName == "ore.small.naquadah"`.
- Behavior: requested vein amount is replaced with `1`.
- Audit scope: `SET_SMALL_NAQ_ORE_TO_1`, at least one modified ore.

### `MixinLoaderGTBlockFluid`

- Target: construction of `ItemBreederCell` in `LoaderGTBlockFluid.run`.
- Match: unlocalized name `rodGlowstone`.
- Behavior: required pulse is replaced with `1,000`.
- Audit scope: `SET_GLOWSTONE_BREEDER_REQUIRED_PULSE_TO_1000`, exactly one
  modified value.

The same mixin redirects every `ItemRadioactiveCellIC` construction:

- Match: unlocalized name contains `Thorium`, `Uranium`, `MOX`, or `Mox`.
- Behavior: `maxDamage` is divided by 10 using integer division.
- Unchanged: cell count, energy value, radiation, heat, depleted item, MOX
  flag and heat bonus.
- Audit scope: `REDUCE_THORIUM_URANIUM_MOX_CELL_LIFETIME`, at least one
  modified cell.
- Lithium and any unrecognized cell names remain unchanged.

## Late machine and power mixins

`MixinSteamInputHatch` changes the Steam Input Hatch capacity from 64,000 L to
640,000 L in `GregtechCustomHatches.run1`. The larger buffer reduces power
failures on steam multiblocks with high per-tick demand.

The generator mixins use `GeneratorEfficiency.forTier(machineTier,
firstTier)`. The shared curve yields 20%, 30% and 40% for the first three
available tiers, then adds 5 percentage points for every later tier.

### `MixinMTESteamTurbine`

- Modifies `getEfficiency()` so the resulting displayed efficiency follows
  the standard generator curve: 20% at LV, 30% at MV, 40% at HV and
  5 percentage points per later tier.
- Consumption text uses
  `4000 * (8 * 4^tier + 2^max(tier-1,0)) / efficiency` L/s.
- Audit: mixin application only.

### `MixinMTEPlasmaGenerator`

- Modifies the returned efficiency with a progression relative to its first available
  tier: EV 20%, IV 30%, LuV 40%, ZPM 45% and UV 50%.
- Audit: mixin application only.

### `MixinMTEDieselGenerator`

- Modifies the returned efficiency with the standard generator curve: LV 20%, MV 30%,
  HV 40% and 5 percentage points per later tier.
- Audit: mixin application only.

### `MixinMTEGasTurbine`

- Modifies the returned efficiency with the standard generator curve: LV 20%, MV 30%,
  HV 40%, EV 45% and IV 50%.
- Audit: mixin application only.

### `MixinMTESemiFluidGenerator`

- Target class belongs to GT++.
- Modifies the returned efficiency with the standard generator curve: LV 20%, MV 30%,
  HV 40%, EV 45% and IV 50%.
- Audit: mixin application only.

### Steam consumption and displays

`SteamConsumption` owns the fivefold multiplier and the single-block
conversion of 10 L per EU. `MixinMetaTileEntity` changes both vanilla 2:1
conversion sites in the fluid-fill overload; both matches are required.
`MixinSteamOverclockDescriber` uses the same ratio for NEI. The single-block
WAILA multiplier, Bronze/Steel fluid tank information, and ModularUI 2 steam
gauge use the same conversion through `MixinSteamMachineDisplay`,
`MixinSteamMachineTank`, and `MixinSteamMachineGui` respectively.

`MixinSteamMultiBlock` multiplies the native efficiency-adjusted running-tick
steam drain and WAILA rate by five. The Steam Water Pump is excluded, including
its separate 1,500 L per-cycle operation. `MixinSteamMultiBlockWailaData`
reproduces the native integer drain calculation for WAILA data; the inherited
floating-point calculation can otherwise round differently. All new injection sites have strict expected match
counts; audit is mixin application only. The integration fixture checks real single
and multiblock drain, insufficient steam, displayed values, the water pump and
Small Coal Boiler production.

### `MixinConfigurationCircuitSupport`

- Targets `CommonWidgets.createCircuitSlot(PanelSyncManager, IMetaTileEntity)`.
- A cancellable HEAD injection replaces the widget for every
  `MTETieredMachineBlock` below HV that implements
  `IConfigurationCircuitSupport`.
- The replacement is a normal `ItemSlot` backed by
  `PhysicalCircuitItemStackHandler`: one real `ItemIntegratedCircuit`, mapped
  to the machine's existing reserved circuit index. Recipe matching continues
  through upstream `allowSelectCircuit()` and therefore retains the circuit
  input without exposing free ghost selection.
- The widget injection clears a saved circuit with `stackSize <= 0` on the
  server when the GUI first creates the physical slot.
- `MixinPhysicalCircuitBlockDrop` and `MixinPhysicalCircuitExplosionDrop`
  include that real reserved-slot circuit in GT's normal block-break and
  explosion inventory-drop paths. Both retain GT's native item and NBT handling;
  phantom circuits and non-circuit slots remain subject to native rules.
- `PhysicalCircuitSlots` shares the tier and slot eligibility between the GUI
  and the two drop hooks.
- Injector requirement: exactly one widget factory method (`require = 1`).
- Each drop wrapper requires exactly one native slot-validity call.
- Audit: mixin application only.

### `MixinMTELargeChemicalReactor`

- Preserves the object returned by `createProcessingLogic()` and calls
  `setOverclock(3.0, 4.0)` on it.
- Redirects the perfect-overclock tooltip call to the literal text
  `Now with amazing 3/4 OC!`.
- Tooltip redirect requires one match.
- Audit: mixin application only.

## Hungry Axe

With Extra Utilities present, `MixinHealingAxeHunger` marks a stack when a
player first carries it on the server, changes its name and replaces the held
axe's hunger gain with a one-point drain every 40 ticks, bounded at zero.
`HungryAxe.drain` reads and writes `FoodStats` through its common NBT API,
preserving saturation, exhaustion and food timer; the apparent setter
`FoodStats.setFoodLevel` is client-only in Minecraft 1.7.10.
`HealingAxeIntegrationTests` exercises the actual item through server player
inventory ticks, including the name, timing, lower bound and preserved food state.

## Late registration mixins

### Ring crafting

Two different upstream registration systems are patched.

`MixinCraftingMaterialLoader`:

- Target: BartWorks Workstoff ring recipe registration.
- Recognizes the `"h "`, `"fX"` pattern, the X binding, a stick `ItemStack`,
  and a ring output association. Delegates registration through WrapOperation.
- Resolves the output material association and replaces the stick ingredient
  with one long stick of the same material.
- Missing association or long-stick item leaves that call unchanged; the final
  audit checks the aggregate modification/registration expectations.
- Audit scope: `RING_FROM_LONG_STICK_WERKSTOFF_HAND_CRAFTING`; every semantic
  modification must also produce one successful registration.

`MixinProcessingShaping`:

- Target: the `GTModHandler.addCraftingRecipe` operation in GregTech
  `ProcessingShaping.registerOre`.
- Checks whether the crafting output has the `ring` ore prefix.
- Replaces the recipe's original `stick` ingredient with
  `OrePrefixes.stickLong` for the output material.

### `MixinProcessingWire`

Target: every `GTRecipeBuilder.addTo` receiver in
`ProcessingWire.registerOre`, using one `@ModifyReceiver`.

All changes add Polyphenylene Sulfide (`PolyphenyleneSulfide`) foil while
preserving the upstream builder and registration map.

| Family | PPS foil amount formula |
| --- | --- |
| Rubber insulation | first fluid input amount / `INGOTS` |
| SBR insulation | first fluid input amount / `(3 * QUARTER_INGOTS)` |
| Silicone insulation | first fluid input amount / `HALF_INGOTS` |
| Four-cable dielectric | second item input stack size * 4 |
| Single-cable dielectric | second item input stack size |

For the first three families, foil is inserted immediately before the final
item input after verifying it is circuit 24. For the last two, foil is
appended. Builders are changed only for assembler/cable maps, supported wire
prefixes and target materials. MV materials (`Iron`, `Nickel`, `Cupronickel`,
`Copper`, and `AnnealedCopper`) and HV materials (`Kanthal`, `Gold`, `Electrum`,
`Silver`, and `BlueAlloy`) are excluded for all supported wire prefixes and
recipe families. Existing PPS foil and wire outputs are skipped.
Audit scope `ADD_PPS_FOIL_TO_CABLE_MACHINE_CRAFTING` requires at least one
modified recipe.

### `MixinCircuitAssemblerRecipes`

- Target: all `GTRecipeBuilder.addTo` calls in Dreamcraft
  `CircuitAssemblerRecipes.recipesEditedByBW`.
- Filters to `circuitAssemblerRecipes` and outputs equal to Nanoprocessor or
  Quantumprocessor.
- Multiplies every non-null basic item input stack size by 6, capped at 64
  per stack. Zero-sized non-consumable inputs remain zero.
- Expected scope: four builders (nano/quantum, standard/advanced variants).
- Audit scope: `SCALE_NANO_QUANTUM_CIRCUIT_ASSEMBLER_INPUTS`, exactly four
  modified and four successfully added recipes.

### `MixinMetaGeneratedItem01`

Target: shaped recipe registration in
`MetaGeneratedItem01.craftingShapedRecipes`, filtered to one LV electric motor
and the exact `CWR/WIW/RWC` pattern.

- Iron route (`IronMagnetic` center stick and `AnyIron` rods): registration is
  suppressed by returning success without adding the recipe.
- Steel route (`SteelMagnetic` center stick and Steel rods): the
  `DISMANTLEABLE` recipe bit is added before normal registration.
- Audit scopes: `REMOVE_IRON_LV_MOTOR_HAND_CRAFTING` requires one removed
  recipe; `MARK_STEEL_LV_MOTOR_HAND_CRAFTING_DISMANTLEABLE` requires one
  modified and one successfully added recipe.

### `MixinMaterialsInit`

- `loadBenzene`: fuel value argument is divided by 4; documented upstream
  value 360 becomes 90.
- `loadPhenol`: fuel value argument is divided by 4; documented upstream value
  288 becomes 72.
- Audit scopes: `REDUCE_BENZENE_FUEL_VALUE_TO_QUARTER` and
  `REDUCE_PHENOL_FUEL_VALUE_TO_QUARTER`, exactly one modified value each.

### `MixinAssemblyLineRecipes`

- Modifies every `GTRecipeBuilder.addTo` receiver during
  `AssemblyLineRecipes.run`.
- Match: Assembly Line map, one LuV Fusion Computer output, and exactly one
  single Dense Netherite plate input.
- Behavior: requested amount becomes 64 dense Netherite plates.
- Audit scope: `SET_DENSE_NETHERITE_ASSEMBLY_LINE_AMOUNT_TO_64`, exactly one
  modified recipe.

## Crafting load-complete module

Each logical change below owns a typed `AuditScope`; failures propagate
immediately.

### Bronze Dust

- Finds exactly one shapeless crafting recipe whose probe is three AnyCopper
  dust plus one Tin dust and whose current output is 3 Bronze Dust.
- Changes only the output stack size from 3 to 1.
- Audit scope: `REDUCE_BRONZE_DUST_HAND_CRAFTING_OUTPUT`, exactly one modified
  recipe.

### LV Circuit Assembler

- Replaces exactly one non-mirrored shaped recipe for the LV Circuit
  Assembler.
- The two MV circuit ingredients become LV circuits.
- Audit scope: `MODIFY_LV_CIRCUIT_ASSEMBLER_HAND_CRAFTING`, exactly one
  modified recipe.

### Removed Good Circuit hand crafting

- When NewHorizonsCoreMod is loaded, removes exactly one shaped Good Circuit
  recipe containing three Basic Electronic Circuits, two Diodes, a Phenolic
  Circuit Board, two Copper wires and a Steel casing.
- Audit scope: `REMOVE_GOOD_CIRCUIT_HAND_CRAFTING`, exactly one removed recipe.

### `gregtech.large-chemical-reactor`

- Replaces exactly one 3x3 Large Chemical Reactor crafting recipe.
- Circuit ingredient changes from four HV circuits to four EV circuits.
- Stainless Steel rotor, two large PTFE pipes, HV motor and HV hull remain.
- Replacement is a non-mirrored `GTShapedRecipe` with pattern
  `CRC/PMP/CBC`.

### `dreamcraft.flint-and-steel`

Applied only when Dreamcraft is loaded.

- Replaces the active Dreamcraft flint-and-steel recipe.
- Small Steel gear changes to a full Steel gear (`gearSteel`).
- Small Steel spring changes to a full Steel spring (`springSteel`).

### Tool Station Slab

Applied only when Dreamcraft is loaded.

- Replaces the NHCore ShapelessUniversalRecipe for TConstruct CraftingSlab metadata 1.
- Final inputs: crafting saw, ToolStationBlock metadata 0, and any LV circuit
  from the `circuitBasic` OreDictionary category.
- Audit: MODIFY_TOOL_STATION_SLAB_HAND_CRAFTING, exactly one modification.

## Aluminium load-complete module

All changes in this module use typed `AuditScope` contracts and fail fast.

### Removed Ilmenite chemical baths

Two exact chemical-bath recipes are removed:

| Recipe variant | Input | Sulfuric Acid | Outputs | Original slag chance |
| --- | --- | ---: | --- | ---: |
| `aluminium.ilmenite-bath.crushedPurified` | 1 purified crushed Ilmenite | 1,000 L | 2 Rutile dust, 1 Ilmenite Slag dust, 2,000 L Green Vitriol | 30% |
| `aluminium.ilmenite-bath.crushed` | 1 crushed Ilmenite | 1,000 L | same | 60% |

Both original recipes run for 21 seconds at 1,000 EU/t.

### Removed Ruby Juice mixers

For each Ruby prefix `crushed`, `crushedPurified`, `dustImpure`, and
`dustPure`, two mixer recipes are removed:

- 1 Ruby material + 1 tiny Sodium Hydroxide dust + circuit 1 + 1,000 L
  Hydrochloric Acid -> 1,000 L Ruby Juice;
- 9 Ruby material + 1 Sodium Hydroxide dust + circuit 9 + 9,000 L
  Hydrochloric Acid -> 9,000 L Ruby Juice.

This removes eight mixer recipes from each of the ordinary and industrial
mixer maps.

### `aluminium.ruby-juice.centrifuge`

Removes the exact Ruby Juice centrifuge recipe that consumes 1,000 L Ruby
Juice and returns Aluminium Hydroxide, Chrome, Iron, Vanadium, Magnesium and
1,000 L Hydrochloric Acid, from both the ordinary and industrial centrifuge maps.
The complete Ruby Juice scope requires 18 removals.

### `aluminium.ilmenite-slag.centrifuge`

- Preserves the existing Iron, Niobium, Tantalum, Manganese and Magnesium
  outputs and chances.
- Duration changes from 2 seconds to 10 seconds.
- EU/t remains MV recipe voltage.
- Both the ordinary and industrial centrifuge maps are changed, with two
  modifications required by the audit scope.

## Blast furnace load-complete module

All changes in this module use typed `AuditScope` contracts and fail fast.

### Primitive Blast Furnace Steel fuels

`RESTRICT_PRIMITIVE_BLAST_FURNACE_STEEL_FUELS` scopes candidates to recipes
with an `ingotSteel` output. It retains only recipes containing either an
ore-dictionary `fuelCoke` input or the Railcraft Coal Coke Block.

The supported full-pack contract requires exactly 7 retained and 39 removed
Steel recipes. Non-Steel Primitive Blast Furnace recipes are not candidates.

### Electric Blast Furnace Steel routes

`MODIFY_STEEL_BLAST_FURNACE_RECIPES` requires exactly two removed, two
modified and one added recipe across the EBF and Alloy Blast Smelter maps.

Removed route:

- inputs: 1 Iron dust, 1 Any Carbon dust and circuit 11;
- fluid input: 1,000 L Oxygen;
- outputs: 1 Steel ingot, 1 Ash dust and 250 L Carbon Dioxide;
- 25 seconds at MV recipe voltage and 1,000 K.

Modified Cast Iron route:

| Field | Original | Current |
| --- | --- | --- |
| Cast Iron dust | 1 | 3 |
| Circuit | 11 | 11 |
| Oxygen | 1,000 L | 6,000 L |
| Steel ingots | 1 | 3 |
| Ash dust | 1 | 1 |
| Carbon Dioxide | none | 2,500 L |
| Duration | 5 s | 45 s |
| Voltage | MV | MV |
| Heat | 1,000 K | 1,000 K |

Added Cast Iron route:

- inputs: 3 Iron dust, integrated circuit 1 and an ore-dictionary input of 2 Any Carbon dust;
- output: 3 Cast Iron ingots;
- no item or fluid byproducts;
- 20 seconds at MV recipe voltage and 1,000 K.

After applying the changes, the module asserts that the bulk EV route still
exists unchanged: 50 Iron dust, 1 Any Carbon dust and circuit 2 produce 50
Steel ingots and 1 Ash dust in 150 seconds at EV recipe voltage and 2,000 K.

In the Alloy Blast Smelter, the old Iron plus Coal route is removed and the
Cast Iron route consumes 3 dust and 6,000 L Oxygen for 432 L molten Steel in
36 seconds at MV. Its generated molten-metal route does not return EBF
byproducts.

### Aluminium

| Patch | Current behavior |
| --- | --- |
| `blast-furnace.aluminium.carbon` | Removes the direct `10 Aluminium Oxide + 3 Carbon -> 4 Aluminium + 3,000 L CO2` recipe. |
| `blast-furnace.aluminium.cryolite` | Cryolite route duration changes from 80 to 120 seconds. |
| `blast-furnace.aluminium.nitrogen` | Aluminium dust + circuit 11 + 1,000 L Nitrogen duration changes from 60 to 90 seconds. |
| `blast-furnace.aluminium.no-gas` | No-gas circuit 1 route remains available; duration changes from 75 to 140 seconds. Heat remains 2,054 K. |
| `blast-furnace.aluminium.obsolete-gases` | Removes exactly seven Helium, Argon, Radon, Neon, Krypton, Xenon and Oganesson variants. |

The Aluminium dust routes operate at HV recipe voltage and produce one
Aluminium ingot.

### TConstruct aluminium

Applied only if class `tconstruct.TConstruct` exists.

| Patch | Current behavior |
| --- | --- |
| `blast-furnace.tconstruct-aluminium.nitrogen` | TConstruct aluminium + circuit 11 + 1,000 L Nitrogen duration changes from 60 to 90 seconds at MV and 1,300 K. |
| `blast-furnace.tconstruct-aluminium.no-gas` | No-gas route remains; duration changes from 75 to 120 seconds and heat from 1,300 to 1,600 K. |
| `blast-furnace.tconstruct-aluminium.obsolete-gases` | Removes the same seven noble-gas variants. |

Both aluminium inputs also update their generated Alloy Blast Smelter recipes:
the seven obsolete gas variants are removed; Nitrogen takes 72 seconds, while
the no-gas route takes 112 seconds for Aluminium dust or 96 seconds for
TConstruct aluminium. Those durations preserve GT++'s 80% EBF conversion.

### `blast-furnace.ilmenite`

| Field | Original | Current |
| --- | ---: | ---: |
| Ilmenite dust | 5 | 10 |
| Carbon dust | 1 | 2 |
| Wrought Iron ingot | 1 | 2 |
| Rutile dust | 3 | 6 |
| Ilmenite Slag dust | none | 1 |
| Carbon Monoxide | 1,000 L | 2,000 L |
| Duration | 160 s | 300 s |

HV recipe voltage and 1,700 K remain unchanged.

### `blast-furnace.titanium`

- Magnesium Chloride dust output changes from 6 to 4.
- Inputs remain 2 Magnesium dust, circuit 12 and 1,000 L Titanium
  Tetrachloride.
- Hot Titanium ingot output, 40-second duration, HV voltage and 2,140 K remain.

## Chemical and miscellaneous load-complete module

All changes in this module use typed `AuditScope` contracts and fail fast.

### Indium concentrate

`chemical.indium-concentrate` is applied to both single-block and multiblock
chemical reactor maps.

- Aluminium dust input changes from 4 to 8.
- Circuit 1, 8,000 L concentrate/solution fluids, tiny Indium output, duration
  and voltage remain unchanged.

### Plutonium decay

`chemical.plutonium-decay.circuit-8`:

- consumes 64 Plutonium ingots, 1 Uranium dust, circuit 8 and 8,000 L Air;
- Plutonium dust output changes from 64 to 63;
- 800 L Radon, 75 seconds and HV voltage remain unchanged.

The circuit-10 route first asserts that the exact replacement does not already
exist, then:

- adds a multiblock recipe consuming 64 Plutonium ingots, 1 Uranium dust,
  circuit 10, 8,000 L Air and 800 L Helium;
- outputs 63 Plutonium dust and 960 L Radon;
- duration is 75 seconds at HV recipe voltage.

### Silicon processing

| Patch | Maps | Change |
| --- | --- | --- |
| `chemical.silicon.sodium` | chemical reactor + LCR | Salt output changes from 8 to 7 for the Sodium/Silicon Tetrachloride route. |
| `chemical.silicon.hydrogen-cells` | chemical reactor | Hydrochloric Acid output changes from 3,000 L to 2,000 L. |
| `chemical.silicon.multiblock-hydrogen` | LCR | Hydrochloric Acid output changes from 3,000 L to 2,000 L. |
| `chemical.silicon.trichlorosilane-cell` | chemical reactor | Hydrochloric Acid output changes from 3,000 L to 2,000 L. |
| `chemical.silicon.bartworks-zinc` | chemical reactor + LCR | BartWorks Zinc Chloride-like dust output stack changes from 6 to 5. |

Silicon SG output and all other explicitly matched values remain unchanged.

### Silicon boule cutting

- Removes the 37 L Water and 28 L Distilled Water Cutter recipes for one
  Monocrystalline Silicon Boule.
- Each removed recipe outputs 16 Silicon Wafers and 4 Silicon SG dust in 40
  seconds at LV recipe voltage.
- The Lubricant route remains unchanged.
- Audit scope: `REMOVE_SILICON_WAFER_WATER_CUTTER_RECIPES`, exactly two
  removed recipes.

### LV Lubricant distillation

- For circuit-24 Light, Medium and Heavy Oil Distillery recipes, fluid input
  changes from 12 L to 24 L.
- Lubricant outputs remain 3 L, 6 L and 9 L respectively; duration and LV
  voltage remain unchanged.
- Audit scope: `MODIFY_LV_OIL_DISTILLERY_RECIPES`, exactly three modified
  recipes.

### Endstone centrifuge

`misc.endstone-centrifuge`:

- Tungstate output chance changes from 37.5% to 10%;
- Helium output changes from 4,320 L to 1,500 L;
- Platinum Metallic Powder chance stays 25%;
- Sand chance stays 90%;
- inputs, 576-second duration and 20 EU/t remain unchanged.

Both ordinary and industrial centrifuge maps require the same change.

### Europium and Americium fusion

`ChemicalAndMiscNerf.modifyFusionRecipes` edits four exact signatures in
`fusionRecipes`, in one transaction. `MODIFY_EUROPIUM_AMERICIUM_FUSION_RECIPES`
requires exactly four modifications; missing, duplicate or changed upstream
signatures fail the audit. Each signature checks empty item slots, complete
fluid inputs/output, duration, EU/t and `FUSION_THRESHOLD` metadata.

Original signatures (fluid quantities in mB):

| Output | Inputs | Amount | Ticks | EU/t | Ignition EU |
| --- | --- | --- | --- | --- | --- |
| Molten Europium | 16 molten Neodymium + 48 Hydrogen gas | 16 | 32 | 24,576 | 150,000,000 |
| Molten Europium | 32 molten Neodymium + 16 molten Promethium | 32 | 8 | 24,576 | 150,000,000 |
| Molten Americium | 16 molten Lutetium + 16 molten Chrome | 16 | 96 | 49,152 | 200,000,000 |
| Molten Americium | 32 molten Lutetium + 16 molten Promethium | 32 | 24 | 38,764 | 200,000,000 |

Base routes multiply both inputs by four and divide output by four, with
duration unchanged. Promethium routes halve both inputs and output and multiply
duration by four. All four retain EU/t and ignition metadata; Americium plasma
is outside this modifier's match. The integration fixture checks the documented
final recipes and machine lookup after reindexing.

### Americium plasma fusion ignition

`ChemicalAndMiscNerf.modifyAmericiumPlasmaFusion` targets the separate
`fusionRecipes` entry with no items, 144 mB molten Plutonium-241 and 2,000 mB
Hydrogen gas, 144 mB Americium plasma output, 64 ticks, 98,304 EU/t, and
500,000,000 EU `FUSION_THRESHOLD`. It replaces only that metadata value with
1,000,000,000 EU, raising the required fusion tier from MK3 to MK4.
`MODIFY_AMERICIUM_PLASMA_FUSION_IGNITION` requires exactly one modification.
The integration fixture verifies ignition, the resulting fusion tier, retained
recipe parameters and machine lookup.

### Staballoy centrifuge

`ChemicalAndMiscNerf.modifyStaballoyCentrifuge` requires the original 10-dust
Staballoy decomposition (9 Uranium-238 dust + 1 Titanium dust, 48 seconds,
480 EU/t, guaranteed outputs, no fluids) in both `centrifugeRecipes` and
`centrifugeNonCellRecipes`. Only EU/t changes, to `TierEU.RECIPE_IV` (7,680).
`MODIFY_STABALLOY_CENTRIFUGE_RECIPES` is active with GT++ and requires exactly
two modifications. A missing, duplicate or changed source recipe fails validation.

### Quantum Star bath

`misc.quantum-star` changes Radon required for one Nether Star -> Quantum Star
chemical-bath recipe from 1,250 L to 5,000 L.

### Tellurium blast furnace

`misc.tellurium-blast-furnace` sets output chances to:

- Lead ingot: 100%;
- two Tellurium nuggets: 0.1%.

The target consumes one crushed Lead with circuit 17 at MV, 722 K, for four
seconds.

### Platinum sludge

`misc.platinum-salt-blast-furnace` removes the 87 L Chlorine fluid output from
the refined Platinum Salt blast-furnace recipe.

`misc.platinum-raw-powder` applies to both chemical maps and changes Calcium
Chloride dust output from 3 to 2. Platinum output remains 2 dust.

### Samarium

| Patch | Current chance/output behavior |
| --- | --- |
| `misc.samarium.concentrate` | Samarium dust chance changes from 90% to 5%; Thorianite remains 80%. |
| `misc.samarium.small-blend` | Output chances become Samarium 10%, Sodium 100%, rare-earth residue 100%. |
| `misc.samarium.large-blend` | Output chances become Samarium 10%, Sodium 100%, rare-earth residue 100%. |

All three recipes are changed in both their ordinary and industrial maps; the
scope requires six modifications.

### Extreme Heat Exchanger

`misc.extreme-heat-exchanger` selects the existing GT shaped recipe by its
exact nine-slot probe and output, removes it and registers:

```text
E P E
P H P
S P S
```

- `P`: Tritanium plate;
- `H`: ZPM machine hull;
- `S`: MAR-Ce-M200 plate;
- `E`: IC2 Diamond Reactor Heat Switch with metadata 1.

The existing upstream recycling data is retained (GT's `addItemData` does not
overwrite it). Removal and replacement signatures are validated in one
transaction before the crafting list is changed.

## Development loot validation failures

`LootPatchRunner` owns audited execution and rollback of the native loot editor's
group membership/drop-list changes and weight caches. `LootValidationException`
identifies known pack-data mismatches; arbitrary exceptions are not suppressed.
Only initial deobfuscated startup may continue after such a mismatch. Both loot
audit scopes remain failed, while `ServerLootRuntime.start` leaves opening disabled
for that session. `LootLoader.verifyPublished` applies the same development rule
to known post-publication validation failures. Normal launches and runtime reloads
still reject invalid tables. The local XML is never replaced with generated fixtures.

## Electrical progression certification

See [the progression contract](../docs/PROGRESSION.md). `lv_certificate` is a
new registered item displayed as Electrical Safety Certificate, with no acquisition recipe. It certifies the world via
server-side right-click. `ProgressionData` persists the LV flag in overworld
MapStorage; it is shared across players and dimensions. `steam_certificate`
adds High Pressure Operator certification for Bronze/Steel Steam machines and
as a prerequisite for basic electrical LV+ machines. Missing prerequisites are
selected Steam first, regardless of activation order.

`MixinProgressionTickGate` wraps exactly one `MetaTileEntity.onPostTick` call in
`BaseMetaTileEntity.doPostTick`. It gates electric `MTEBasicMachine` instances
from LV upwards plus basic Steam machines, and retains in-flight work. `MixinProgressionMachineStatus` replaces
the error widget's construction with a clickable subclass, adds a lock syncer,
and prepends the certification message to normal/shift tooltips. Missing
certification selects the custom certificate-warning texture; ordinary errors
retain the native drawable. The independent MUI2 path is handled by
`MixinProgressionMachineGui` on `MTEBasicMachineBaseGui.createErrorWidget`.
Its `CertificateStatusPanel` uses a panel-local, server-to-client-only missing-certificate ID;
the client never queries the server's world data, even in integrated singleplayer.

`VERIFY_PROGRESSION_CERTIFICATES` is a zero-recipe-change audit that checks item
registration and the machine status hook. Both mixins have strict injection
counts. Certificates are hidden from NEI's item panel with wildcard item hiding;
their information pages remain accessible from machine errors. Client NEI pages
resolve only the registered certificate and explicit
tier queries; opening a page never alters server certification.

The page's exam button opens `CertificationExams`' common MUI2 `SimpleGuiFactory`.
`ExamGui` creates the panel on both sides and uses the client proxy for its pencil
canvas. `DrawingExamWidget` draws the real Minecraft font, wraps/scrolls long
question text and retains one client-local `AnswerSheet` bitmap per question.
Font texturing is restored after the scroll stencil; the pencil uses absolute
GUI coordinates in the untransformed foreground pass. Each page change updates
the vertical scroll size and layout so long answers remain reachable.
On the client the panel grows to the current page's height, capped by the
scaled screen height with top/bottom margins. The viewport grows with the
panel; excess content remains scrollable. Footer controls use bottom anchors
and the background uses a nine-slice texture to preserve the pixel frame.
Only submission sends four-bit A-D choices through `ExamSyncHandler`; page
changes and strokes send no packets. Its network callback queues work in
`ProgressionServerTasks`, which checks the active container before server-tick grading.
Client callbacks publish immutable paper/result data for the GUI thread.
`ExamAttempt` caches completion before issuing the reward so duplicate packets
cannot issue another item. Inventory overflow uses normal server item drops.

`VERIFY_CERTIFICATION_EXAM` checks the registered factory and the public wire
projection. The LV exam has ten questions, the key B,C,A,D,B,C,A,D,C,B and a pass
threshold of 8/10; the independent Steam factory uses the key B,D,A,C,B,A,D,C,A,B with
the same threshold. Both are defined in `BundledExams`, and the registered factory
chooses the reward certificate. The questions, options, introduction and result
messages are language keys (`gtnhextranerf.exam.<tier>...`) stored in `en_US.lang`;
the server sends the keys and each client translates them. There is no exam file in
the configuration directory any more. See [the exam contract](../docs/CERTIFICATION_EXAM.md).

`MixinElectricalCableFire` wraps the successful fire-block replacement in
`BaseTileEntity.setToFire`, capturing a live `MTECable` before the tile is
invalidated. Minecraft's invocation target is remapped even though the GT
method is not. Fluid pipes do not trigger revocation. `MixinElectricalMachineExplosion`
observes `BaseMetaTileEntity.doExplosion` at its valid meta-entity `onExplosion`
call. `ElectricalSafety.record` accepts only logical-server observations and
queues saved-state mutation/global notification through `ProgressionServerTasks`.
`VERIFY_ELECTRICAL_SAFETY_HOOKS` verifies distinct injected marker interfaces;
both accident targets have strict injection counts.

`ProgressionData.revoke` clears the shared LV flag, advances its saved issue
epoch and marks data dirty. `ProgressionCertificates.issue` tags rewards with
the current epoch; native item activation rejects stale copies without consuming
them. Legacy epoch-zero items remain compatible until the first revocation.
The tick gate retains paused work and every open GUI receives its usual S2C
status update. New certification restores the relevant basic tiers; generators,
boilers and multiblocks are not included. Server stop clears queued progression
work even when loot-runtime cleanup fails.

`MixinSteamPipeDamage` wraps native `GTUtility.applyHeatDamage` and
`applyFrostDamage` calls in fluid-pipe contact and leak paths.
`MixinSteamExhaustDamage` wraps Bronze's `allowToCheckRecipe` heat call, inherited
by Steel machines. `SteamSafety` requires a server player, an accepted hit and
reduced health/absorption before queueing Steam revocation. `VERIFY_STEAM_SAFETY_HOOKS`
audits both markers; injection counts are strict. Electrical certification is
retained, but all basic electrical machines stop on the missing Steam prerequisite.

## Core-mod recipe load-complete module

All changes in this module use typed `AuditScope` contracts and fail fast.

### Superconductor coil casing

`CoreModRecipeNerf.modifySuperconductorCoil` modifies only the LuV and ZPM
assembler variants in one transaction. `MODIFY_SUPERCONDUCTOR_COIL_RECIPE`
requires exactly two modifications, each with a complete original signature:

- LuV: 64 `wireGt02` SuperconductorLuV and 48 Mica Insulator Foils become
  64 `wireGt08` SuperconductorLuV and 192 Mica Insulator Foils. This replaces
  the old 128-wire / 384-foil nerf; foil scales with wire thickness (4x).
- ZPM: 32 `wireGt02` SuperconductorZPM become 32 `wireGt04` SuperconductorZPM;
  Naquarite Universal Insulator Foil increases from four to eight (2x).
- Both retain one casing output, 50 seconds and 9,001 EU/t, with no fluid
  outputs. Molten Trinium remains 1,440 L for LuV and 576 L for ZPM.
- Higher-tier variants are outside both exact matches. Missing, duplicate or
  changed original signatures fail the audit before either edit is committed.

### Large Chemical Reactor assembler

`core.large-chemical-reactor.assembler` changes four circuit items from HV to
EV. HV motor/hull, Stainless Steel rotor, PTFE pipes, circuit configuration,
duration and voltage remain unchanged.

The crafting-table version is independently patched by `CraftingNerf`.

### Low-tier circuits

| Patch | Change | Audit contract |
| --- | --- | --- |
| Advanced Circuit, standard + ASMD | ILC Chips `2 -> 8`; RAM Chips `2 -> 8` | `MODIFY_ADVANCED_CIRCUIT_INPUTS`, exactly 2 modified |
| Integrated Logic Circuit | ILC Chips `1 -> 4` | `MODIFY_INTEGRATED_LOGIC_CIRCUIT_INPUTS`, exactly 1 modified |
| Good Circuit | Basic Electronic Circuits `2 -> 4` | `MODIFY_GOOD_CIRCUIT_INPUTS`, exactly 1 modified |

### Heating coils

`CoreModRecipeNerf` changes Assembler coils from Cupronickel through Awakened
Draconium to eight 8x wires and doubles their insulation. The Cupronickel
variants use `MODIFY_CUPRONICKEL_COIL_INPUTS`; the following tiers use
`MODIFY_HEATING_COIL_INPUTS`.

Awakened Draconium is the final heating coil covered by the nerf.

### MV Energy Hatch

- ULPIC Chips change from 2 to 4.
- Audit scope: `MODIFY_MV_ENERGY_HATCH_INPUTS`, exactly one modified recipe.
- The shaped hand recipe `ICL/WHP/ICL` (ULPIC Chip, MV Coil, Lubricant cell, Copper
  cable, MV Hull, MV Pump) is removed by `CraftingNerf`. Audit scope:
  `REMOVE_MV_ENERGY_HATCH_HAND_CRAFTING`, exactly one removed recipe.

### Diamond Sawblade

- Assembler: Cobalt Brass Gear changes to Blood Steel Gear. Audit scope: `MODIFY_DIAMOND_SAWBLADE_RECIPE`.
- Hand recipe ` D /DGD/ D ` (Small Diamond Dust, Cobalt Brass Gear) takes the Blood Steel Gear too.
  Audit scope: `MODIFY_DIAMOND_SAWBLADE_HAND_CRAFTING`, exactly one modified recipe.

### Elite circuits

The separate Circuit Assembly Line recipe map is intentionally outside the
Circuit Assembler cost changes and may retain cheaper circuit recipes.

For each standard and ASMD recipe family, the currently active old solder
recipe is removed, exact absence of the new recipe is asserted, and a new
cleanroom recipe is added with 100 L Soul Infused Medium.

Before registration, the module asserts that neither exact replacement already
exists.

Standard replacement recipe:

- 2 Aluminium frames;
- 2 Data circuits;
- 12 coils;
- 24 capacitors;
- 16 RAM chips;
- 24 Annealed Copper 1x wires;
- one Elite circuit output;
- 80 seconds at HV recipe voltage.

ASMD replacement recipe:

- 2 Aluminium frames;
- 2 Data circuits;
- 3 ASMD inductors;
- 4 ASMD capacitors;
- 16 RAM chips;
- 16 Annealed Copper 1x wires;
- one Elite circuit output;
- 40 seconds at HV recipe voltage.

### PPIC wafer

`core.ppic-wafer` applies to both chemical maps and changes the NPIC wafer
input from 1 to 64. The 64 Indium Gallium Phosphide dust and 1,440 L molten
Sunnarium inputs remain unchanged.

### Removed diode assembler recipes

Four exact 30-second LV assembler recipes are removed:

| Recipe variant | Semiconductor | Output | Glass fluid |
| --- | --- | ---: | --- |
| `core.diode.gallium-arsenide.gt-glass` | 1 small Gallium Arsenide dust | 2 diodes | 288 L GregTech molten Glass |
| `core.diode.gallium-arsenide.registry-glass` | same | 2 diodes | 1,000 L registry `glass.molten` |
| `core.diode.silicon-wafer.gt-glass` | 1 Silicon Wafer | 1 diode | 288 L GregTech molten Glass |
| `core.diode.silicon-wafer.registry-glass` | same | 1 diode | 1,000 L registry `glass.molten` |

All four also consume 4 fine Copper wires and circuit 4.

### Diamond sawblade

`gt.diamond-sawblade` changes the gear input from one Cobalt Brass gear to one
Blood Steel gear, resolved with the GT++ material API (`BLOODSTEEL.getGear(1)`).
The former generic OreDict lookup returned null in the pinned pack. Diamond
dust, output, 16-second duration and 20 EU/t remain.

### Machine casings

Each assembler recipe changes `8 plate` to `8 plateDouble`, effectively
doubling material mass while preserving material, output, circuit 8, duration
and LV/2 voltage.

| Patch suffix | Tier | Material |
| --- | --- | --- |
| `ulv` | ULV | Cast Iron |
| `lv` | LV | Steel |
| `mv` | MV | Aluminium |
| `hv` | HV | Stainless Steel |
| `ev` | EV | Titanium |
| `iv` | IV | Tungsten Steel |
| `luv` | LuV | Rhodium-Plated Palladium |
| `zpm` | ZPM | Iridium |
| `uv` | UV | Osmium |
| `max` | MAX | Neutronium |

All ten Assembler variants are aggregated by `MODIFY_MACHINE_CASING_RECIPES`.
The ten hand-crafting variants also require double plates, retaining the wrench
and tier-specific output; `REPLACE_MACHINE_CASING_HAND_CRAFTING` requires ten modifications.

### AE2 progression

The old Charged Certus Quartz modifier is removed. Redstone remains the
upstream ingredient in both chemical maps.

`Ae2RecipeNerf` removes Controller crafting/Assembler recipes, substitutes TPV
components and fluids in GT recipe maps for AE2 outputs, and replaces the three Glowstone
plates in Blank Pattern crafting with Sunnarium. `MixinAe2Crafting` wraps the
NHCore AE2 script crafting registration methods and replaces Titanium
ingredients, preserving other native ingredients and OreDictionary names.
There are no ordinals or per-item injection points. All rules use separate audit
scopes. The Controller uses a native Assembly Line recipe at LuV, with research
from a 64k ME Storage Cell scanned at HV. Standard Scanner consumption and NBT
matching apply; no Scanner mixin is added.

Titanium substitution is restricted to AE2-owned outputs. GT processing
times, energy, fluid quantities and output amounts are retained. See docs/AE2_PROGRESSION.md.

This is the only production traversal of all GT maps. Keep it local until a
second global rule is needed, then consolidate the rules into one shared,
batched traversal. Use known-recipe identity replacement within the traversal,
not repeated predicate searches of each map.

### Raw Neutronium

`core.raw-neutronium` changes Helium plasma from 144 L to 1,440 L. One
Neutronium dust, circuit 1, Raw Neutronium output, 11.25-second duration and ZPM
voltage remain. Both ordinary and industrial mixer maps are changed.

### Forestry-dependent Indium recipes

These patches run only when Forestry reports loaded.

The Praseodymium centrifuge change applies to both ordinary and industrial
centrifuge maps.

| Patch | Change |
| --- | --- |
| `core.indium-comb.concentrate` | Indium dust output changes from 3 to 2. |
| `core.indium-comb.phosphate` | Indium dust output changes from 6 to 4. |
| `core.indium-comb.purified-crushed` | Purified crushed Indium output changes from 12 to 8 in both chemical maps. |
| `core.indium-comb.roquesite` | Indium dust output changes from 3 to 2. |
| `core.praseodymium-centrifuge` | Final Indium dust output changes from 12 to 8. |

All other exact inputs, outputs, fluids, chances, durations and voltages used
to identify these recipes remain unchanged.

### Forestry-dependent vacuum furnace recipes

`core.vacuum-furnace.sphalerite`:

- three separate 64-dust Indium output stacks each become 32;
- Zinc, Iron, Germanium, Red Mud, Water, duration, voltage and heat remain.

`core.vacuum-furnace.chalcopyrite`:

- Indium dust output changes from 30 to 20;
- Copper, Iron, Cadmium, Red Mud, Water, duration, voltage and heat remain.

## Slime and EMT Glue load-complete module

`SlimeRecipeNerf` runs last, after every other recipe module. It uses seven
typed audit scopes and preserves Sticky Resin recipes.

### Removed machine processing

`REMOVE_SLIME_EXTRACTOR_RECIPES` removes:

- the vanilla Slimeball Extractor recipe;
- TConstruct `strangeFood:0` Gelatinous Slime when TConstruct is loaded;
- TConstruct `slime.gel` metadata 0 and 1 when TConstruct is loaded;
- 16 TConstruct `slime.leaves:0` producing Raw Rubber when TConstruct is
  loaded.

Its audit contract therefore expects one removal without TConstruct and five
with it.

`REMOVE_SLIME_CENTRIFUGE_RECIPES` is active only with TConstruct and removes
exactly six recipes whose inputs are vanilla Slimeball, TConstruct
`strangeFood:0`, `slime.gel` metadata 0, 1 and 2, or `CraftedSoil:0` Slime
Mud.

`REMOVE_SLIME_MULTIBLOCK_CENTRIFUGE_RECIPES` is active with TConstruct and
GT++. It removes the six cell-free copies generated in
`centrifugeNonCellRecipes` for the Industrial Centrifuge.

### Sticky Piston

When Electro-Magic Tools is loaded, the exact five-second, 4 EU/t Assembler
recipe using one Piston and one vanilla Slimeball is removed. One otherwise
identical recipe using `EMT:EMTItems:8` Glue is added. The Sticky Resin and
fluid-glue Assembler recipes do not match this signature and remain.

When NewHorizonsCoreMod and Electro-Magic Tools are both loaded, the one
Dreamcraft shaped crafting recipe backed by the `slimeball` ore dictionary is
removed. It is replaced by a non-ore-dictionary recipe requiring one Glue,
one Piston and the same soft-mallet tool position.

### Coated Circuit Boards

The module removes eight slime recipes in the full pack: single-board and
three-board recipes for vanilla Slimeball plus TConstruct `materials:36`,
`strangeFood:0` and `strangeFood:1`. It adds exactly two Glue recipes:

- one board from one Wood plate and two Glue;
- three boards from three Wood plates and six Glue.

Without TConstruct, the audit contract expects only the two vanilla slime
recipes to be removed. The shaped three-board replacement is non-mirrored.

### Resistors

For each of Coal, Carbon, Charcoal and Lignite dust, the module removes the
four full-pack slime-binder variants and adds one recipe using two Glue. This
is 16 removals and 4 additions with TConstruct, or 4 removals and 4 additions
without it. Copper wire layout and output count are unchanged; replacements
are non-mirrored.

The crafting rewrites plan and validate every original and replacement recipe
before changing the raw Minecraft recipe list. A missing or ambiguous target
therefore prevents the whole logical rewrite from being applied.

## Server telemetry

Optional server-only telemetry is independent of recipe audits. The logical server
captures its in-memory session UUID, connected-player count, highest available tier
and integrated/dedicated status on its own thread. UUID persists through toggling
telemetry, changes on restart, and does not rotate at midnight. Development mode
remains disabled. Packaged builds do not require a release manifest or readable JAR.

`ServerTelemetry` owns lifecycle/ticks and one daemon worker. `TelemetryClient`
handles JSON, optional SHA256(salt || SHA256(JAR)) proof and bounded HTTP. The JAR
is attempted once in the worker; failure yields null proof. The report has no version
header, client timestamp or request UUID, and the client refuses plain HTTP outside loopback. Only these two classes plus the existing
administrative `TelemetryCommand` are needed. Commands implement `ICommand` for
production remapping. Owner opt-out persists configuration, without chat notices.

See `docs/TELEMETRY.md` for the JSON contract, the join notice, the early-certificate shutdown and
opt-out, `docs/TEAM_TRACKING.md` for the separate opt-in team report and `docs/UPDATE_CHECK.md` for the
version check.

## Iron LV motor Assembler alternative

`REMOVE_IRON_LV_MOTOR_ASSEMBLER_RECIPE` removes exactly one recipe: one
magnetic Iron rod, two Tin cables, the OreDict slots `stickAnyIron` (2) and
`wireGt01AnyCopper` (4), one LV motor output, no fluids, 20 ticks and LV recipe
voltage. Other motor routes remain. The existing mixin already removes the
corresponding hand-crafting route and preserves dismantleable Steel motors.

## Progression load-complete module

`MixinAssemblyLineDimension` targets the public three-argument `checkMachine`
methods on `MTEAssemblyLine` and `MTEAdvAssLine`. It injects at every return,
including the normal line's early successful orientation checks. On the logical
server it adds a native `StructureError` only when all upstream diagnostics are
empty and the tile's world does not use AE2 `StorageWorldProvider`.
The parent multiblock's formed-state validation gates native processing. The
`VERIFY_ASSEMBLY_LINE_SPATIAL_DIMENSION` audit verifies both hooks, registered
controllers and the English error resource when AE2 is loaded.

`MixinLightningRod` replaces GT 5.09.54.226's public `onPostTick` generation
algorithm for the three native rods. It keeps server-only execution, native
buffer loss, the 256-tick cadence, sky/contiguous-fence checks, lightning effects
and one-fence consumption. Storms require at least 16 fences; a world-local
random roll uses `min(length, 32) / 128`, independently of controller Y.
Mast and sky validation include the top valid layer, `world.getHeight() - 1`.
A strict constant injection reduces storage to 10,000,000 EU.
`VERIFY_LIGHTNING_RODS` checks the shared hook, all three native registrations,
storage and the existing voltage/amperage ratings.

`MixinSingleblockTransformerPorts` targets the common BaseMetaTileEntity energy
facade. It rejects sided/unsided input/output, native network participation and
legacy offers/demands for `ProgressionMachineNerf.isDisabledTransformer`'s typed
families. The same-voltage BartWorks distributor is excluded. Native RF pulling
and GT network emission already consult the closed faces. `VERIFY_SINGLEBLOCK_TRANSFORMER_PORTS`
checks strict hook activation, registrations and Manual Transformer exemption.
IC2 transformers are outside this mixin and explicitly remain functional.

`MixinEvergladesMachineBan` cancels the shared machine tick on the logical
server when its valid tile uses the native Everglades provider. It invokes
native `doExplosion` with LV voltage, then removes/blasts a surviving tile
whose controller overrides explosion with an empty body. Both paths respect
the native terrain-damage setting and retain the existing accident hook.
`VERIFY_EVERGLADES_MACHINE_BAN` checks mixin presence and, when loaded, the
registered final dimension/provider. No dimension number is hardcoded and
there is no world scan or client mutation. MetaPipeEntity is outside this hook.

`RawMachineCasings` is one registered block with fourteen persisted metadata
variants and a nested ItemBlock. Chisel's native isolated groups are registered
in post-init, after Chisel, before GT's lazy NEI cache. Existing ULV-UHV casing
modifiers preserve their double-plate changes and redirect LV-UHV outputs.
`REDIRECT_HIGH_TIER_RAW_CASINGS` changes the four higher-tier hand/Assembler
pairs with an exact eight-modification contract. `VERIFY_RAW_CASING_FINISHING`
checks effective groups, NEI entries and absence of direct finished production.
Without Chisel in reduced dev, output redirection is inactive; missing integration
fails the full-pack audit. Finished IDs and native recycling are retained.

`REMOVE_UNAMPLIFIED_UU_RECIPES` removes two unpaid CPU recipes, two CropsNH
material-leaf UU extraction recipes and the unpaid singleblock fake recipe
(if native configuration registered it). It verifies zero remaining UU
Fluid Extractor routes. `MixinMassFabricatorAmplifier` rejects missing or
insufficient amplifier at `checkRecipe` HEAD, leaving paid native processing
and consumption intact. `VERIFY_MASS_FABRICATOR_AMPLIFIER` checks all native
prototypes and the configured positive fee. Native fixtures exercise unpaid,
insufficient and paid batches; map checks retain amplifier routes.

`REMOVE_FLOCCULATION_WASTE_RECYCLING` removes exactly one Distillation Tower
recipe (10000 mB waste -> Aluminium / Oxygen / Hydrochloric Acid) and checks
zero waste-input recipes in the staged tower table and the Distillery map.
The existing transaction and backend tests cover the removal machinery;
the full-pack startup verifies its native signature and exact audit count.

`MODIFY_HIGHEST_OZONATION_RECIPE` changes one native top-variant ozone input
from 1024000 to 1000000 mB and verifies the three lower variants' signatures.
`MixinOzonationSafety` changes the native threshold reads in admission and
tooltip creation. For GT 5.09.54.261's Markdown tooltip, it redirects the
`addMarkdown(ResourceLocation, Map)` resource to the mod's English template,
retaining the native variable map and replacing the exact-doubling claim with
the four recipe variants. `VERIFY_OZONE_SAFETY`
checks hook presence and `ISecondaryDescribable.getPrimaryDescription()`;
calling `getDescription()` here would access client keyboard state.
`MixinOzonationExplosion` snapshots `explodeMultiblock`'s hatch iterable and
guards its explosion calls against a controller already removed by a hatch
blast. Both branches activate only for the ozonator. Native fixtures check
safe admission/consumption and actual destruction above the boundary and
at full ZPM capacity.

`MixinLegacyFusionCoils` targets only native MK2/MK3 and GT++ MK4/MK5.
Coil block/meta getters return BEC metadata 1. GT .221's shared parent builds
its tooltip from the actual getters, so no tooltip injection is needed.
MK4/MK5's ordinary casing getters delegate to the coil getter upstream, so
their original block identities are explicitly preserved. Common startup
calls `ProgressionMachineNerf.verifyFusion`; its zero-mutation
`VERIFY_LEGACY_FUSION_COILS` audit checks all four changed prototypes,
unchanged ordinary casing block/meta, MK1 and the five Compact coils, plus
the native secondary description naming the actual BEC coil.
The opt-in native fixture constructs and validates each legacy reactor,
rejects an old coil, and verifies restoration.

`ProgressionRecipeNerf` runs after the existing load-complete recipe modules
and before Tungstensteel production and Brainrotter registration. It uses the
same `RecipeContext` transactions, backend index maintenance and development
validation policy.

`ADD_NAQUADAH_PURIFICATION_WATER` requires exactly nine modified recipes:
three EBF reductions, Naquadahine in both chemical maps, and the two sulphates
in both Electrolyzer maps. Each batch consumes 1000 mB of Grade 1, 2 or 3 water
for Naquadah, Enriched Naquadah or Naquadria respectively. The modification
selects intermediate conversion inputs and outputs, preserving copied GT++
routes and all other native recipe data. Pure dust smelting and independent
Earth/QFT routes remain outside this scope. The opt-in native integration
fixture checks required water, insufficient/waterless rejection, actual input
consumption and native machine lookup in all five affected maps.

LV circuits introduced by the crafting module remain OreDictionary ingredients:
both slots in LV Circuit Assembler crafting and the Tool Station Slab accept all
registered `circuitBasic` variants. Resolving the first ore entry is appropriate
for a matching probe, but must not replace the category in a published recipe.
