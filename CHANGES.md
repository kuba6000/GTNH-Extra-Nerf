# Current gameplay changes

This file is a concise summary of the active ruleset. The complete canonical
catalog, including exact values, conditions and unchanged fields, is
[`docs/NERFS.md`](docs/NERFS.md).

## Lootbag previews

- NEI displays the server's current ME Controller lootbag allowance, including
  an unknown state and 0% after it is spent. Native weight percentages are
  identified as single-draw values before limits and retries, and native player
  counters are marked as potentially stale.

## Questbook crafting tasks

- BetterQuesting crafting tasks also accept their required items from the
  inventory without consuming them. Crafting remains valid, and saved task IDs
  are preserved.

## Telemetry consent

- Telemetry is opt-in and decided per world. A new world asks its operators (or the owner of a
  singleplayer world) in a window with three choices, and a dedicated server's console is told
  how to answer. No statistics or team reports are sent until someone has answered. The old `enabled=true` default of
  the configuration file does not count as an answer; a refusal and a listed team are carried
  into the first world, and the settings now live in the world.
- Basic statistics and team tracking are mutually exclusive; either mode contributes to
  community statistics.
- Reporting choices are written immediately. Team IDs are kept when tracking is off and are available
  in a text file in the world folder instead of being shown in chat.
- Enabling team tracking informs connected players about the policy and their right to object.
  The console must explicitly name an online player as team leader.

## Soybean crops

- HarvestCraft soybean crops yield Blighted Soybean instead of Soybean.

## MV Electric Motor

- The crafting and Assembler recipes require a Magicnetized Manasteel Rod made
  in an Arcane Workbench.

## Hungry Axe

- Fixed a dedicated-server crash when holding Extra Utilities' Healing Axe.

## Industrial recipe consistency

- Updated the Alloy Blast Smelter's generated Steel and Aluminium recipes to
  match the corresponding EBF changes.
- Applied the Ruby Juice removals and the Endstone, Samarium, Raw Neutronium
  and Indium Comb changes to the corresponding industrial machine maps.
- Applied the longer Ilmenite Slag centrifuging time to its industrial recipe.

## Startup

- Narrowed the coremod transformer exclusion to remove
  `PACKAGE_TRANSFORMER_EXCLUSION` messages for Extra Nerf mixins.

## Certification exams

- The Steam and LV exams are part of the mod and use language keys for every question,
  option and message, so they can be translated like any other text. The
  `config/gtnhextranerf/*-exam.json` files are no longer read and can be deleted.
- Chat messages and command usage of telemetry, team tracking and the update check, and the
  startup error screen, use language keys too.

## Machine certification

- The Industrial Apiary requires certificates only up to HV instead of UV, which is the
  tier GregTech registers it at.

## Physical configuration circuits

- Machines and input buses below HV return the physical circuit in the reserved
  slot when broken, preserving its NBT and native explosion-drop handling.

## Lightning Rod

- Mast and clear-sky checks include the highest valid world layer.

## Main menu

- Custom Main Menu displays an amber pixel badge with `EXTRA NERF` and the
  mod's release version in the upper-right corner. It uses native CMM elements
  for every GUI scale and menu reload, without modifying the pack's JSON files.
  No badge is added when CMM is absent or on other menus.

## Recipe infrastructure

- Dependencies target one current upstream baseline: GT 5.09.54.261,
  NHCore 2.9.99 and NEI 2.8.171-GTNH. Legacy fusion coil tooltips now use GT's
  native getter-based description; the obsolete subclass tooltip injection
  that prevented client startup is removed and its output is audited.

- Load-complete rules now register ordered modifiers through explicit interfaces.
- Machine and crafting edits are staged per modifier, with validation and rollback
  across affected registries.
- Crafting factories use direct constructors; reflective construction is removed.
- Fixed the Diamond Sawblade's Blood Steel gear lookup through the GT++ material
  API. The previous generic lookup returned null in the pinned full pack.
- Crafting ingredients now use an immutable interface with separate item and
  OreDict implementations. Native constructor encoding is isolated in the
  factory; builder/definition storage no longer uses Object or runtime casts.
- Crafting rules now share one immutable builder definition for pattern,
  ingredients, output and named GT flags; replacements use `toBuilder()`.
- Machine modifications use `RecipeChange.builder()` and identify ingredients
  and outputs semantically, with guarded counts for repeated outputs. Rules no
  longer assign GT recipe fields or edit arrays by position.
- Machine signatures use `RecipeMatch.builder()` with named fields and explicit
  alternatives instead of predicate chains and positional signature helpers.
- Existing balance rules and audit contracts are retained. See
  [`docs/RECIPE_SYSTEM.md`](docs/RECIPE_SYSTEM.md) for the extension API.

## Power and machines

- Standard GT singleblock recipes, including Steam machines, lose their
  current batch if they cannot draw enough energy. Existing output and queued
  input stay; machines can start a new batch automatically. A missing machine
  certificate still pauses an in-flight batch without destroying it.

- All Void Miner tiers accept Neon, Krypton, Xenon and Oganesson with tiered
  consumption costs. MK1 favors Neon, MK2 Krypton and MK3 Xenon/Oganesson.
  Native boosts stay; insufficient gas is retained and the miner runs unboosted.

- Assembly Line and Advanced Assembly Line require an AE2 Spatial world. A
  hidden controller error appears only after the ordinary structure is complete;
  it asks for a more fluixy world.

- Lightning Rods generate only during thunderstorms, with a 10 million EU
  buffer, a minimum 16-fence mast and a length-based strike chance capped at
  25% for 32 fences. Controller height provides no bonus.

- GT singleblock transformers (normal, Hi-Amp, wet/advanced) and buck converters
  retain their recipes but cannot receive, transform or emit energy. Manual
  and Active Transformer multiblocks, same-voltage Energy Distributor and IC2
  transformers retain their functionality.

- GT machines and addon controllers explode before processing in Toxic
  Everglades, including disabled/unpowered machines and normally immune
  TecTech controllers.

- LV-UXV machine casings must be finished with Chisel after crafting or
  assembly. Native groups also define a MAX placeholder; ULV is unchanged.

- UU-Matter requires amplifier in singleblock Mass Fabricator and Matter
  Fabrication CPU. Both UU-producing Fluid Extractor routes are removed;
  amplifier production remains available.

- Ruthenium Tetroxide separation moves to a powered, output-protected Coolant
  Tower recipe map. Standard NEI discovery and passive Steam condensation stay.

- EV+ insulated cable production moves out of Assembler; native LATEX routes
  are required for every removed output. Lower-rated cables and bare wires stay.

- All eight Europium boule EBF variants consume two additional Neptunium
  Hexafluoride dust. Existing gas variants and wafer cutting are retained.

- DTPF requires Hypogen or better coils after full native structure validation.
  Lower coils produce a synchronized controller error, without an early hint
  in tooltips, construction or NEI.

- Chemical Plant Mutated Living Solder uses eight Nuclear Stars in place of
  Gravistars. The QFT route is retained; multi-amp UHV star production works.

- All three Bio Vat Raw Bio Catalyst Medium recipes consume one additional
  Radioactive Mineral Mix dust; cultures and native fluid amplification stay.

- PCB Factory wetware/bioware boards use Copper Plasma in place of Sulfuric
  Acid, at the same fluid amount. Ordinary board-production alternatives stay.

- Americium-doped boule in EBF requires Oganesson and two Protactinium dust.
  Other gas variants are removed; wafer cutting is retained.

- UV/UHV hulls require Kevlar insulation; UEV-UXV require Radox Polymer.
  Both Assembler and existing hand recipes are covered. Native recycling
  continues to exclude the extra insulation.

- Removed Flocculation Waste Liquid resource recovery from Distillation Tower;
  audits also reject a remaining distillation shortcut. Waste production stays.

- Ozonator admission now explodes above 1000000 mB ozone; the highest variant
  consumes that same amount and retains its 80% chance. Full ZPM hatches remain
  unsafe. Ozonator destruction handles hatch removal and an already destroyed
  controller without crashing.

- Legacy Fusion MK2-MK5 use BEC isolation casings as coils. MK1, Compact
  structures, ordinary casings and controller crafting remain available.

- Naquadahine, Enriched Naquadah Sulphate and Naquadria Sulphate conversion
  require the matching purified water grade in EBF and alternative reactor /
  electrolyzer routes. Already pure dust processing is unchanged.

- Steam, Diesel, Gas and GT++ Semi-Fluid generators use 20% efficiency at LV,
  30% at MV, 40% at HV and gain 5 percentage points per later tier.
- Plasma Generator progression restarts at 20% on EV, then uses 30% on IV,
  40% on LuV and gains 5 percentage points per later tier.
- Steam machines consume five times their vanilla steam. Single-block machines
  use 10 L per EU; Steam Water Pump and boiler production are unchanged.
- Steam Input Hatch capacity increases from 64,000 L to 640,000 L to reduce
  power failures on steam multiblocks with high per-tick demand.
- Benzene and Phenol fuel values are divided by four.
- Large Chemical Reactor overclocking changes to 3/4.
- Machines below HV use a physical Integrated Circuit slot; ghost selection
  starts at HV.

## World generation and nuclear

- Naquadah small-ore amount becomes 1.
- Glowstone breeder required pulse becomes 1,000.
- Thorium, Uranium and MOX radioactive-cell lifetime is divided by ten.

## Crafting and registration-time recipes

- UQV (Ultimately Questionable Voltage) adds eight fictional components with
  UEV appearance, distinct names and localized tooltips. They have no production
  recipes and are hidden from the Creative tabs and NEI item panel/search.
- UQV item tooltips show only the short tagline. UFO Assembler has one fictional
  NEI blueprint per component: UEV source + a characteristic unavailable part
  -> UQV result. UFO Assembly is the category icon/catalyst, with eight owned
  ingredient sprites and an animated beam. These are not gameplay recipes.
- UFO blueprints no longer appear in usages of original UEV components;
  UQV components, fictional ingredients and the UFO item retain their page links.
- Crafting Input Proxy's Space Assembler recipe requires a UQV Sensor instead
  of a UEV Sensor, making normal production unavailable while retaining its NEI
  recipe.
- Recipe edits and the final audit wait for GregTech's late recipe queue,
  including in dev launches where GT receives LoadComplete after Extra Nerf.

- Large Chemical Reactor recipes require EV rather than HV circuits.
- Flint and Steel uses full Steel gear and spring ingredients.
- TConstruct Crafting Slab requires an LV circuit.
- The LV Circuit Assembler crafting recipe uses LV instead of MV circuits.
- Good Circuit hand crafting is removed.
- Nano and Quantum processor item inputs are multiplied by six, capped at 64
  per input stack.
- Bronze mixing produces one dust instead of three.
- Iron LV motor crafting is removed; Steel LV motor crafting is dismantleable.
- Ring crafting uses long sticks.
- Selected cable recipes require PPS foil, excluding MV and HV cables.
- Dense Netherite plate requirement becomes 64.
- Extreme Heat Exchanger crafting requires Tritanium, a ZPM hull,
  MAR-Ce-M200 and Diamond Reactor Heat Switches.
- Slimeball, Gelatinous Slime, slime-block and Slimy Leaves Extractor recipes
  are removed.
- Slimeball, slime-block and Slime Mud recipes are removed from both the
  single-block and GT++ multiblock Centrifuge maps.
- Sticky Pistons, Coated Circuit Boards and Resistors keep their Sticky Resin
  routes, but their slime variants are replaced by EMT Glue variants.

## Steel production

- Primitive Blast Furnace Steel production keeps 7 Coke-based recipes and
  removes 39 variants using other fuels.
- The direct MV Iron + Carbon + Oxygen to Steel route is removed.
- The Cast Iron to Steel route now processes batches of 3 using 6,000 L
  Oxygen over 45 seconds and returns 2,500 L Carbon Dioxide.
- A new MV route converts 3 Iron dust, circuit 1 and 2 Any Carbon dust into 3
  Cast Iron ingots.
- The bulk 50-ingot EV Steel route remains unchanged.

## Aluminium, Ilmenite and Titanium

- The direct Aluminium Oxide + Carbon blast-furnace route is removed.
- Aluminium/Cryolite, Nitrogen and no-gas routes take longer; seven alternative
  gas variants are removed.
- TConstruct Aluminium receives corresponding duration/heat changes and loses
  seven alternative gas routes, but keeps its no-gas route.
- Ilmenite chemical baths and the Ruby Juice processing chain are removed.
- Ilmenite blast-furnace batching doubles and adds Ilmenite Slag.
- Ilmenite Slag centrifuging takes longer without adding Gallium.
- Titanium processing returns less Magnesium Chloride.

## Chemistry and ore processing

- Indium concentrate requires twice as much Aluminium.
- Plutonium decay loses one Plutonium and gains a circuit-10 Helium route.
- Silicon routes return less Salt, Hydrochloric Acid or BartWorks byproduct.
- Silicon boule cutting loses its Water and Distilled Water routes.
- LV Lubricant distillation consumes twice as much Light, Medium or Heavy Oil
  for the same output.
- Endstone centrifuging returns less Tungstate and Helium.
- Quantum Stars require four times as much Radon.
- Tellurium nugget chance becomes 0.1%.
- Platinum sludge routes return less Chlorine/Calcium Chloride.
- Samarium output chances are heavily reduced.

## Circuits, components and casings

- Elite circuits require Soul Infused Medium in a cleanroom.
- PPIC processing requires 64 NPIC wafers.
- Four diode assembler shortcuts are removed.
- Machine-casing hand and Assembler recipes use eight double plates for ULV
  through UMV. UXV uses one Dense MHDCSM Plate and four Double Magmatter Plates.
- Diamond Sawblade uses Blood Steel instead of Cobalt Brass.
- Superconductor Coil Casing requires 128 LuV 2x wires and 384 Mica foils.
- Integrated Logic, Good and Advanced circuits require more preceding-tier
  circuits or wafer chips.
- Cupronickel Coils require 8x wire and twice as much insulation.
- MV Energy Hatches require four ULPIC chips instead of two.

## Endgame and optional Forestry routes

- Raw Neutronium requires 1,440 L Helium plasma.
- Charged Certus Quartz uses Mysterious Crystal rather than Redstone.
- Forestry-dependent Indium comb, centrifuge and GT++ vacuum-furnace outputs
  are reduced.

## Removed historical claims

The current source does not contain the previously documented shears patch,
circuit-9 small Plutonium recipe, or removal of the TConstruct no-gas
Aluminium route.

## Audit lifecycle refactor

- Replaced pooled global audit units with independently testable sessions and
  one-use handles; definitions no longer read global results.
- Typed expectations generate validation and diagnostic descriptions together.
- Reports retain explicit failed/skipped/missing outcomes and all operation
  counts after finalization. Failed callbacks cannot pass through matching counts.
- Development tolerance is restricted to recipe validation exceptions; original
  failures survive finalization errors. Mixins use outcome-aware callbacks.

- Client audit failures now open Forge's startup error screen with an English
  support message using `SUPPORTED_GTNH_VERSION = "2.9.0"`.
- Added dev-only error-screen preview via `runClient -PforceAuditErrorScreen=true`
  or the Minecraft JVM property `gtnhextranerf.forceAuditErrorScreen=true`.
