# Next progression nerfs: preliminary research

Date: 2026-10-11. Branch: `codex/next-progression-nerfs`, based on merged
`master` (`dd45620`). Implementation was authorized on 2026-10-11. The status
table below distinguishes implemented changes from deferred decisions. For the
current behavior, see [the gameplay catalog](NERFS.md).

The follow-up decisions below were agreed during the design review.
Compact Fusion, the ozone variant, the specified Naquadah intermediate
conversion recipes, UV+ hull insulation, the Bio Vat input and the Nuclear
Star replacement in solder are now designed. DTPF's final-validation check
and joke are agreed, as are both boule additives, the EV+ assembler cable
restriction and moving Ruthenium separation into Coolant Tower. Points
**14, 15 and 18 are deferred** at the user's request. Point 17 was resumed
with the operation-only singleblock restriction below. The Toxic Everglades
machine ban, the exact UU-Matter removals and native two-member Chisel groups
for tiered raw/finished casings are also recorded below. The original review
called for checking progression dependencies and alternative production paths
before implementation.

The original source review used the then-prepared full-pack server runtime:

- [GT5-Unofficial 5.09.54.205][gt], including the merged GT++, BartWorks,
  GoodGenerator and TecTech code.
- [NewHorizonsCoreMod 2.9.84][core].

The UU-Matter and carving follow-up checked CropsNH 2.0.134 and Chisel
2.17.34-GTNH from that runtime, alongside the client's CropsNH .137. Source
links retain the original review revisions. The full-pack server fixtures and
aggregate audit were rerun during that work on GT **5.09.54.221**, NHCore
**2.9.88** and NEI **2.8.159-GTNH**. See [compatibility](COMPATIBILITY.md) for
the currently supported baseline.

Development dependencies matched that target at the time; no older-GT mixin
variants were maintained. The reduced dev policy for missing optional content remains.
Sources were downloaded from official
GitHub tags into ignored `build/research/next-nerfs/`; `SOURCE.json` records
the commit for each source tree. This review does not claim a complete scan
of every mod's runtime recipes or a successful gameplay test of these changes.

## Implementation status

New tests use the user-approved boundaries: native GT recipe maps and audit
reports, real server machines/structures and client-visible status, and native
Chisel groups/crafting. Each stage is verified before its implementation commit.

| Point | Change | Status |
| --- | --- | --- |
| 1+ | Tiered water for named Naquadah intermediates | Implemented; native full-pack checks passed |
| 2+ | BEC coils in legacy fusion MK2-MK5 | Implemented; native structures checked |
| 3+ | Local ozone limit and highest variant | Implemented; native boundaries and explosions checked |
| 4+ | Remove flocculation waste recycling | Implemented; exact removal and distillation absence audited |
| 5+ | Kevlar/Radox hull insulation | Implemented; 6 Assembler + 6 hand recipes, native lookup/crafting/consumption passed |
| 6+ | Oganesson-only Americium boule | Implemented together with 12; native gas/input/consumption checks passed |
| 7+ | Copper plasma for wet/bio PCBs | Implemented; native plasma/acid admission and ordinary alternatives checked |
| 8+ | Mineral Mix in Bio Vat | Implemented; native culture-sensitive lookup and consumption checks passed |
| 9+ | Nuclear Star in Mutated Living Solder | Implemented; native star consumption, QFT route and UHV power bootstrap passed |
| 10+ | DTPF Hypogen gate and final-structure error | Implemented; native incomplete/low/Hypogen/Eternal states and error packet checked |
| 11+ | Neptunium Hexafluoride in Europium boule | Implemented; all eight native lookup/consumption variants and cutting checked |
| 12+ | Protactinium in Americium boule | Implemented together with 6; native input/consumption checks passed |
| 13+ | EV+ cable assembler removal | Implemented; 1866 native variants removed, all output LATEX routes verified, HV widths retained |
| 14, 15 | Solar salt ideas | Deferred by user |
| 16+ | Ruthenium separation in Coolant Tower | Implemented; native map, passive Steam, powered cycle and output rejection checked |
| 17+ | Disable singleblock GT transformers and buck converters | Implemented; all registered tiers/modes, unsided injection, NBT and real receiver checked; native Manual up/down conversion retained |
| 18 | GT Printer | Deferred by user |
| 19+ | Amplifier-only UU-Matter | Implemented; native map removals and singleblock unpaid/paid admission checked |
| 20+ | Native Chisel finishing for raw casings | Implemented; isolated groups, native hand container, GT consumption and NEI checked |
| 21+ | GT machine explosions in Everglades | Implemented; native ticks, NBT reload, immune TT controller, terrain setting and certification checked |

## Review of the requested changes

### 1. Tiered water for Naquadah

User intent: require purified water late in the chemical processing line,
not in ore washing. Agreed mapping: **Naquadah -> Grade 1, Enriched Naquadah
-> Grade 2, Naquadria -> Grade 3**. Each specified recipe consumes **1000 mB
per batch producing one hot ingot or one pure-metal dust**. Alternative
machines charge the same water amount for the same metal yield.

The specified scope is reduction/decomposition of Naquadahine,
Enriched Naquadah Sulphate and Naquadria Sulphate to the pure metals. Its
EBF reductions are:

| Product | Existing input -> output | Recipe power / heat | Planned water |
| --- | --- | --- | --- |
| Naquadah | 3 Naquadahine dust + 1 Carbon dust -> 1 hot ingot + 1000 mB CO2 | IV / 5000 K | 1000 mB Grade 1 |
| Enriched Naquadah | 11 Enriched Naquadah Sulphate dust + 2 Zinc dust -> 1 hot ingot + 12 Zinc Sulphate dust | IV / 7500 K | 1000 mB Grade 2 |
| Naquadria | 11 Naquadria Sulphate dust + 2 Magnesium dust -> 1 hot ingot + 12 Magnesium Sulphate dust | ZPM / 9100 K | 1000 mB Grade 3 |

These recipes currently have no fluid input, so the added water fits the
existing EBF map's single fluid-input slot. Add the same requirement to
their alternative pure-dust production routes:

| Intermediate | Existing alternative conversion | Maps to change | Water per batch |
| --- | --- | --- | --- |
| 3 Naquadahine dust | + 4 Sodium dust -> 1 Naquadah dust + 6 Sodium Oxide dust | Chemical Reactor and LCR | 1000 mB Grade 1 |
| 11 Enriched Naquadah Sulphate dust | -> 1 Enriched Naquadah dust + 2 Sulfur dust + 8000 mB Oxygen | Electrolyzer and Industrial Electrolyzer | 1000 mB Grade 2 |
| 11 Naquadria Sulphate dust | -> 1 Naquadria dust + 2 Sulfur dust + 8000 mB Oxygen | Electrolyzer and Industrial Electrolyzer | 1000 mB Grade 3 |

The Naquadahine chemical recipe uses `UniversalChemical`, so both the
singleblock `chemicalReactorRecipes` and `multiblockChemicalReactorRecipes`
entries must change. Leaving the smaller reactor untouched would bypass
the LCR water requirement.

Both sulphates explicitly enable electrolysis in their Werkstoff material
definitions. BartWorks' `DustLoader` generates the decomposition recipes
from their composition (one metal, two Sulfur, eight Oxygen); they are not
written explicitly in `NaquadahRecipeLoader`. GT++ then copies them into
`electrolyzerNonCellRecipes`, used by both current and legacy Industrial
Electrolyzers. Modify both final tables after that generation and maintain
their lookup indexes. Naquadahine explicitly disables electrolysis; no new
Naquadahine electrolyzer recipe is proposed.

The source-defined required set is **nine entries**: three EBF, two chemical,
two singleblock electrolyzer and two non-cell electrolyzer entries. Confirm
the final loaded cardinality before setting the production audit contract.
Preserve all original metal/byproduct amounts, time, EU/t and heat requirements.

**Implemented:** `ProgressionRecipeNerf` applies all nine changes under
`ADD_NAQUADAH_PURIFICATION_WATER`. The prepared .205/.84 full-pack server
confirmed that cardinality, passed the aggregate audit and native recipe
lookup/consumption checks, and shut down successfully. Before implementation,
the same fixture failed because the EBF conversion did not require water.
The implementation now builds and passes native tests on the single .221/.88 baseline.

**Explicit exclusion:** recipes whose starting material is already pure
Naquadah, Enriched Naquadah or Naquadria dust do not acquire water. Their
smelting, casting, hot-ingot cooling and intended recycling stay unchanged.
Select the changed recipes by the named intermediate input and conversion
output, rather than broadly matching every recipe producing the finished
metal. This charges water once when the metal becomes pure, not again
when pure dust is made into an ingot.

**Availability check:** the Clarifier (Grade 1) and Ozonation unit (Grade 2)
draw LuV recipe power; the Flocculation unit (Grade 3) draws ZPM recipe power.
Their controller crafting confirms this is not just a misleading NEI label:
the main plant, Clarifier and Ozonation controllers are LuV Assembly Line
recipes requiring LuV components and ZPM circuits. Flocculation is a ZPM
Assembly Line recipe requiring ZPM components, UV circuits and already
processed Naquadah/Naquadah Alloy. None of its listed controller inputs is
Naquadria itself. Power is supplied by the shared Purification Plant.

Thus Grade 1/2 are not available at the original **IV recipe tier** of the
first two EBF reductions. This design uses the existing waterline, so those
conversions move behind the LuV water installation even though their recipe
EU/t remains unchanged. No earlier water-production route has been requested.
Complete the bootstrap dependency check before enabling the gate, especially
for first LuV hardware and circuits.

Separate from the specified intermediates, Naquadah has eight gas-assisted
low-yield nugget routes from Naquadah Earth. The QFT lines for all three
materials end in Neutron Activator molten outputs. These remain paths to
review if the gate is to cover every ore-processing shortcut as well.
Gas-assisted EBF and plasma-fed activator routes already use their sole
fluid-input slot. Do not silently add a second fluid or widen every EBF's
fluid capacity: choose water cells with empty-cell output, a suitable
preceding step or removal if those paths are subsequently included.

Higher neutron accelerators already require purified water grades 5–8; those
are unrelated machine crafting requirements. Preserve the direct use of
chemical intermediates in doped-boule recipes unless separately requested.

The named intermediate conversions and their water cost are settled.
Remaining review: bootstrap dependencies and the separate Earth/QFT shortcuts;
their treatment is not implied by a blanket change to pure-metal smelting.

Sources: [final reductions and alternatives][naqline], [water recipes][water],
[waterline controller crafting][watercontrollers], [Clarifier power][clarifier],
[Ozonation power][ozone], [Flocculation power][flocculator],
[recipe-map capacities][maps], [sulphate material definitions][naqmaterials],
[automatic dust electrolysis][dustloader], [non-cell map generation][noncell].

### 2. Compact Fusion required from MK2

Agreed direction: replace the coil element of **legacy MK2–MK5** structures
with **Electromagnetically-isolated Casing**, keeping MK1 and all Compact
structures unchanged. This is not a replacement with the corresponding
Compact coil. Controllers remain craftable: Compact MK2 and MK3 consume
48 legacy controllers each, and later Compact upgrades use their own
legacy counterparts as ingredients.

The BEC casing is an existing block: `GregTechAPI.sBlockCasingsBEC`, metadata
1, registered as `ItemList.ElectromagneticallyIsolatedCasing`. Its BEC recipe
is **UIV**, takes 1200 s and requires Hypogen screws, a UEV Field Generator,
metamaterial shielding, quark-containment casings and entangled condensates.
It therefore moves the small reactor's structure requirement far beyond
ordinary MK2/MK3 progression. These legacy reactors will become buildable
again after acquiring the BEC casing; the design delays them, rather than
permanently forbidding them.

Implementation plan: override coil block and metadata for the four legacy
classes, using the normal structure validation, hints and survival builder.
Their shared structure resolves these getters lazily and caches by controller
class, so no recipe removal or independent structure cache is needed. Keep
the Compact class hierarchy outside this hook. Audit block identity and
metadata for the affected prototypes and unchanged MK1/Compact prototypes.
Verify existing structures reject old coils and the builder places BEC
casings. BEC registration exists in both production GT .205 and dev GT .149;
still retain normal dev handling if required content fails to register.

Implementation detail discovered during native verification: MK4 and MK5
resolve their ordinary casing block by calling `getFusionCoil()`. Preserve
their original GT++ casing blocks explicitly while changing the coil getter;
otherwise this would also replace their ordinary casings with an invalid BEC
metadata variant. MK2/MK3's ordinary casing getters are independent.

**Implemented:** the targeted getter/tooltip mixin and
`VERIFY_LEGACY_FUSION_COILS` audit enforce this for all four legacy prototypes.
The native full-pack fixture built and formed each structure with BEC coils,
rejected replacement by its old coil, and formed again after restoration.
MK1 and all five Compact coil identities remained unchanged. Build/check and
the complete dedicated-server fixture passed.

Sources: [legacy MK2 coils][fusion2], [shared structure and cache][fusionbase],
[Compact recipes][compact], [BEC block identity][becblock],
[BEC casing recipe][becrecipe].

### 3. Ozone storage / ZPM hatch capacity

User goal: a full ZPM input hatch must not automatically make ozone handling
safe. **Agreed variant:** change only the ozonator's explosion threshold to
**1000000 mB**, and the highest recipe's ozone consumption from 1024000 to
**1000000 mB**. Preserve its 80% base success chance and all other recipe
parameters. Leave hatch capacities unchanged.

| Variant | Ozone per 1000 mB Grade 1 water | Base chance | Grade 2 output |
| --- | --- | --- | --- |
| 1 | 128000 mB, unchanged | 20% | 900 mB |
| 2 | 256000 mB, unchanged | 40% | 900 mB |
| 3 | 512000 mB, unchanged | 60% | 900 mB |
| 4 | **1000000 mB** | **80%**, unchanged | 900 mB |

A normal ZPM input hatch remains 1024000 mB. Exactly 1000000 mB is safe;
strictly more is unsafe, so filling the hatch leaves 24000 mB above the limit.
Merely lowering the threshold while keeping the old top recipe would make
that recipe require an amount that always exceeds the threshold.

Keep the native trigger timing: the machine checks ozone when a recipe is
found at cycle admission, before recipe inputs are drained. This design does
not add a new per-tick explosion check. Only water I/O and power scale with
the unit's configured parallel count; ozone consumption does not.

Implementation plan: one targeted ozonator hook for the threshold and its
tooltip, one audited native-table edit for the top variant with lookup-index
maintenance. Refresh the tooltip's claim of an exact doubling for every
20% boost: the last cost is now 1000000 rather than twice 512000. Both client
and server must build the same native table, so NEI shows the actual cost.
Audit the top variant and verify all four chances/outputs are retained.

**Implemented:** the highest variant and the local threshold use the same
1000000 mB limit. `MODIFY_HIGHEST_OZONATION_RECIPE` requires one modification
and validates the three retained variants; `VERIFY_OZONE_SAFETY` checks the
required hooks and resulting native primary tooltip. The latter avoids
`getDescription()`, which polls the client keyboard and is unsafe on a server.
Tooltip edits operate on the constructed line for both Java 8 and Java 17.

The native dedicated-server fixture confirmed admission of the 80% variant
and consumption at exactly 1000000 mB, destruction at 1000001 mB and at the
unchanged full ZPM capacity. This exposed two upstream explosion problems:
world-removal callbacks mutate the iterated hatch lists, and a hatch explosion
can remove the controller before the final controller explosion call. The
Ozonator-only branch snapshots the list and skips already destroyed tiles,
preserving native destruction without swallowing exceptions. Both behaviors
and the aggregate audit passed after those repairs. A second fixture with
two overfull input hatches also verified that the native scan cannot attempt
a second explosion after the controller has already been removed. The relevant native
threshold and trigger code are also present in dev GT .149.

Sources: [ozone recipes][water], [ozonator check][ozone],
[input-hatch capacity][hatch], [parallel scaling and input consumption][unitbase].

### 4. Flocculation Waste Liquid must not be recyclable

Interpreted as removing recovery, rather than fixing a currently broken
recovery recipe. One directly registered distillation-tower recipe turns
10000 mB waste into 20 Aluminium dust, 30000 mB Oxygen and 30000 mB
Hydrochloric Acid, at EV in 1 s. The reviewed registration does not generate
distillery variants for this particular recipe. The flocculation unit
produces the waste through its own processing logic.

This has enough detail to implement removal. Keep waste production and
existing disposal available; verify that no other loaded recipe recovers
resources from the same fluid.

**Implemented:** `REMOVE_FLOCCULATION_WASTE_RECYCLING` removes the one
identified native recipe transactionally and requires that neither the
staged Distillation Tower table nor Distillery contains a remaining waste
input. Waste production and fluid-container handling are untouched. The
prepared full-pack audit confirmed one removal and no distillation shortcut;
the dedicated server and native regression fixtures completed successfully.
This small table edit uses the existing recipe-editor tests and startup
contract rather than an additional test duplicating the balance signature.

Source: [flocculation and recycling][water].

### 5. UV+ hull insulation: Kevlar, then Radox Polymer

UV and UHV casings currently use 8 Osmium / Neutronium plates. Their hull
assembler recipes use the casing, cables and 288 mB molten PBI. Both also
have crafting-table routes. NHCore adds UEV, UIV, UMV and UXV casings and
hulls; UXV hulls already use 576 mB molten Kevlar, while UEV–UMV hulls use
576 mB molten PBI. The historical GT name `Casing_MAX` / `Hull_MAX` here
means **UHV**, not the final MAX certificate tier.

**Agreed design:** replace the existing insulation in the **hull recipe**,
keeping the metal casing and the current cable inputs. Preserve the existing
insulation quantities, recipe time and EU/t.

| Hull tier | Existing assembler insulation | Planned insulation |
| --- | --- | --- |
| UV | 288 mB molten PBI | **288 mB molten Kevlar** |
| UHV | 288 mB molten PBI | **288 mB molten Kevlar** |
| UEV | 576 mB molten PBI | **576 mB molten Radox Polymer** |
| UIV | 576 mB molten PBI | **576 mB molten Radox Polymer** |
| UMV | 576 mB molten PBI | **576 mB molten Radox Polymer** |
| UXV | 576 mB molten Kevlar | **576 mB molten Radox Polymer** |

Apply the same material requirement to crafting alternatives. In the UV and
UHV crafting grid, the two PBI plates in the top corners become Kevlar plates;
the central top metal plate, casing and cables retain their current inputs.
NHCore's `GT_Loader_Machines` registers four higher hand recipes: UEV/UIV/UMV
use two double PBI plates; UXV uses two dense Kevlar plates. All four become
Radox Polymer in the same form. No new hand-crafting route
is introduced by this design.

This is six source-defined assembler edits plus the existing crafting
alternatives; verify the final loaded counts and resulting requirements
through the current audit system. In particular, include UXV's existing
Kevlar recipe in the Radox replacement. Bare casing recipes have no new
polymer requirement. Update/review recycling item data to match the changed
composition, and check the first Kevlar/Radox production dependencies before
turning the hull requirement into a mandatory progression gate.

Sources: [GT casings and hulls][assembler], [GT crafting alternatives][mtecraft],
[NHCore casings and hulls][coreassembler], [NHCore crafting][corecraft].

Higher hand recipes: [NHCore machine loader](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/f769c65f68a352160b1e35770f6f7c579716baa5/src/main/java/com/dreammaster/gthandler/GT_Loader_Machines.java#L68).

Implementation review: native recycling explicitly excludes additional hull
insulation to avoid a hand-crafting/Assembler duplication loop, so its casing
and cable composition is retained. Kevlar weaving and plate formation are
EV/LV steps; Radox polymerization is UV, with the existing MK2 Titanium/Oxygen
plasma routes. Bio Vat permits multiple energy hatches, and native processing
accepts recipes one voltage tier above the hatch when sufficient amperage is
available; its UEV Xenoxene recipe therefore does not require a UEV hull.
No new shortcut production is added. The late modifier audits twelve edits
and checks that each hull has exactly one hand-production recipe.

### 6. Americium-doped wafer may be baked only with Oganesson

The EBF produces an **Americium-doped boule**, which is subsequently cut
into 128 wafers. Its inputs are 64 Silicon SG blocks, 4 Gallium Arsenide
crystals, 4 Americium ingots and circuit 3; heat 9000 K, LuV.
One builder emits **eight gas variants**. The existing Oganesson variant
uses 1600 mB and lasts 315 s; the other variants use Nitrogen, Helium,
Argon, Radon, Neon, Krypton and Xenon. No gas-free variant is requested
by this builder.

Agreed change: remove the seven other gas variants, preserve the existing
Oganesson variant and add **2 consumed full Protactinium dust** to its item
inputs, as specified in point 12. The resulting EBF recipe keeps the original
Oganesson quantity, time, EU/t, heat, other ingredients and output.

Sources: [boule recipes][boules], [gas generator][emitter],
[gas multipliers][gases], [wafer cutting][cutting].

### 7. PCB Factory: copper plasma replaces sulfuric acid for wet/bio boards

Wetware and Bioware boards have normal, Silver-nanite and Gold-nanite
variants for each supported plastic tier. With the eight registered plastics,
this gives **12 Wetware and 9 Bioware recipes**. Their sulfuric-acid amounts
are `500 * sqrt(plasticTier - 4)` and `500 * sqrt(plasticTier - 5)`, truncated
to integer amounts. Preserve the other fluids, board yield, upgrade and
nanite requirements. Optical boards also use acid but are outside this request.

The replacement preserves the existing fluid amounts **1:1**. The reviewed sources contain
Copper plasma production in FoTG's Plasma Module (dust and molten routes,
UXV recipe EU/t), and no direct Copper-plasma output in the ordinary fusion
recipe loader. The user clarified that boards can be made outside PCB Factory:
this gate deliberately applies to the factory's wet/bio recipes only. Keep
those other board-production paths available, and use the existing late-game
Copper plasma source. Do not add an earlier fusion recipe for Copper plasma.

Verified alternatives: Circuit Assembler makes the base Wetware/Bio boards;
the chemical maps turn those into Extreme/Ultra boards using Iron(III) Chloride
or Sodium Persulfate. These routes require no Copper plasma and are retained.

Sources: [PCB variants][pcb], [plastic tiers][plastics],
[FoTG plasma recipes][godforge], [base board alternatives][boardalternatives]
and [chemical finishing alternatives][boardfinishing].

### 8. Radioactive Mineral Mix Dust in Raw Bio Catalyst Medium

Confirmed target: **all three Bacterial Vat recipes producing Raw Bio
Catalyst Medium** (`Materials.BioMediumRaw`), registered by NHCore's
`BacteriaRegistry`. Add **one consumed full Radioactive Mineral Mix dust**
to each recipe. This is an extra item input, preserving the existing inputs,
cultures, time, EU/t, glass tier, radiation requirements and cleanroom flag.

| Variant | Existing item inputs | Base fluid conversion | Added input |
| --- | --- | --- | --- |
| LuV, glass 6 | 64 Stemcell chips, 16 Unknown Crystals, 4 TCetiE Seaweed Extract, 4 Tritanium dust | 8 mB Raw Growth Medium -> 2 mB Raw Bio Catalyst Medium | 1 Mineral Mix dust |
| ZPM, glass 7 | 16 Stemcell chips, 16 Unknown Crystals, 8 TCetiE Seaweed Extract, 4 tiny Infinity Catalyst dust | 5 mB Raw Growth Medium -> 5 mB Raw Bio Catalyst Medium | 1 Mineral Mix dust |
| UV, glass 8 | 8 Biocell chips, 16 TCetiE Seaweed Extract, 4 Infinity Catalyst dust | 5 mB Raw Growth Medium -> 10 mB Raw Bio Catalyst Medium | 1 Mineral Mix dust |

These are the source recipe's base fluid quantities, before native Bio Vat
fluid amplification. `MTEBioVat.recipeWithMultiplier` scales fluid input
and output, leaving item inputs unchanged. The new dust follows the same
item-consumption semantics as the existing ingredients; do not introduce a
separate per-mB charge or change that native amplification behavior. The
recipes need 5, 5 and 4 normal item inputs after the addition, within the
map's existing six-input capacity. The Petri dish remains in the special slot.

Use the existing recipe transaction and an audit expecting three source
variants. Match the target medium plus the full recipe identity and metadata,
not every use of a similarly named catalyst. The Raw Intelligence Catalyst
assembler recipe is outside this request and keeps its current inputs.

The dust already exists as
`MaterialsOres.DEEP_EARTH_REACTOR_FUEL_DEPOSIT.getDust(1)`. Its ore forms are
registered by Toxic Everglades, but **ore registration alone does not prove
natural generation**. The reviewed production-version Everglades vein list
(`OreMixes.GTPP1` through `GTPP19`) contains no reference to this material.
No corresponding Mineral Mix entry was found in the inspected full-pack
`GregTech/WorldGeneration.cfg` either. Do not promise that the current
Everglades supplies it without validating an actual additional worldgen route.

Confirmed alternative production exists in the Cyclotron and BeamCrafter:
one Plutonium-238 dust, eight Unknown particles and 1000 mB Ender fluid give
a 25% chance of one dust. Both are ZPM; the Cyclotron recipe takes 15 minutes,
the BeamCrafter recipe takes 2 seconds plus its required neutron beams.
The proposed Everglades machine ban does not remove these recipes in other
dimensions. If the intended catalyst gate specifically requires mining
Mineral Mix in Everglades, its worldgen must be settled separately.

Sources: [three Bio Vat recipes][biomedium], [native fluid multiplier][biovat],
[Bio Vat map capacity][biovatmap], [material registration][evergladesores],
[Everglades ore veins][evergladesveins], [Cyclotron production][gtpprecipes],
[BeamCrafter production][mineralbeam].

### 9. Mutated Living Solder: Gravistar -> Nuclear Star

The Chemical Plant recipe currently consumes 64 Biocell chips, 8 Gravistars,
2 Infinity Catalyst dust, Tin and Bismuth plasma and Cryotheum, using an
Infinite Mutation Catalyst. It produces 40320 mB solder in 800 s.
A separate QFT route produces 18432 mB solder together with Biocells and
growth media from Stemcells and Infinity Catalyst dust. Changing only the
Chemical Plant leaves that QFT route available.

**Agreed ingredient:** `ItemList.NuclearStar`, the next Gravistar upgrade.
Replace **8 consumed Gravistars with 8 consumed Nuclear Stars** in the
Chemical Plant recipe, retaining the current amount. This replaces the item
input, not the Infinite Mutation Catalyst. Preserve the Biocells, Infinity
Catalyst dust, all fluid inputs, 40320 mB output, 800 s duration, 3842160 EU/t
and casing tier 7.

Nuclear Star is an existing GT item. Its main Autoclave recipe consumes
**16 Gravistars + 288 mB molten Infinity -> 1 Nuclear Star**, at UEV in
24 s. A later UIV alternative uses 64 Nether Stars and 576 mB Infinity
plasma. Eight Nuclear Stars through the main route therefore represent
128 Gravistars and 2304 mB molten Infinity, making the material upgrade
substantial even with the same item count in the solder recipe.

Audit the one Chemical Plant replacement through the existing recipe system.
The QFT alternative has no Gravistar input to replace; this specified
one-for-one substitution does not add a new star input to that recipe.
Keep its independent route visible in the progression review.

Bootstrap review: the UEV Energy
Hatch recipe itself consumes 2880 mB Mutated Living Solder. Do not infer
either a hard deadlock or a guaranteed UEV unlock solely from the star
recipe's EU/t label. The Multi Autoclave uses native processing with
fluid-pipe-dependent energy discounts and multiblock power handling. Check
whether an attainable UHV/multi-amp setup can produce the first stars
without this solder, or whether an earlier star route is needed. No changes
to Nuclear Star crafting or QFT costs are part of this ingredient decision.

Implementation verification: native `ProcessingLogic` produces a Nuclear
Star from the existing Autoclave recipe at 4 A UHV. It uses the registered
Autoclave prototype for output limits; Multi Autoclave inherits the same
multi-hatch voltage/amperage handling and additionally discounts energy by
fluid-pipe tier. No changed star recipe is needed. This checks processing
power and output, not the physical Autoclave structure.

Sources: [Chemical Plant solder][solder], [QFT solder][skips],
[Nuclear Star crafting][nuclearstar], [UEV Energy Hatch][uevhatch],
[Multi Autoclave processing][multiautoclave].

### 10. DTPF: discover the Hypogen minimum after building the structure

DTPF currently validates that coils exist and obtains heating capacity from
their level. Hypogen is `HeatingCoilLevel.UXV`, 12601 K; Eternal is 13501 K.
**User intent:** require Hypogen or a higher coil level, but reveal the new
requirement only after the player has built the complete multiblock. Its
controller must display a humorous, useful error for lower-level coils.

Agreed implementation is one additional check **at the end of normal
structure validation, only if its error list is empty**. First let GT validate
the entire physical structure, coil consistency, casing count and hatches.
Once those checks pass, append a translatable structure error if the installed
coils are below Hypogen. The normal GT invalid-structure handling then stops
the machine before recipes can start and shows the error in the controller.
No input items, catalyst or fuel should be consumed by that rejection.

| State | Controller behavior |
| --- | --- |
| Incomplete structure or other normal structural errors | Native building errors; no new Hypogen/stinky-coils message |
| Complete otherwise-valid structure, coils below Hypogen | Custom stinky-coils error; machine cannot operate |
| Complete otherwise-valid structure, Hypogen or higher coils | Normal recipe and heat validation |

Agreed English controller text:

> These coil blocks are too stinky! Use better ones.

Use this accepted wording in the English localization. Use the existing
`StructureErrors.of(...)` translatable error and native structure-error
synchronization/display, plus our English localization file.
Compute the validation outcome on the logical server; the client renders
the synchronized error. No custom error class, GUI framework or network
packet is required. This path also works without first supplying recipe
inputs, because the coil error belongs to the completed structure check.

**Do not reveal the new minimum through advance building information.**
Leave the coil structure element, hints, survival builder and heating-coil
channel as the native generic coil mechanism. Keep the existing controller
tooltip's generic `Heating Coil` entry. Do not add a Hypogen-minimum note
to item tooltips, NEI pages, quests or the hologram, or replace all recipe
heat metadata with the new machine minimum. The controller error may name
Hypogen after the otherwise-complete structure has been checked. This is a
gameplay discovery rule, not a claim that the threshold is secret from code
or translations.

Retain native per-recipe heat requirements, hatch rules, energy costs and
efficiency mechanics. Existing placed DTPFs below the minimum fail the next
normal structure validation too. Removing a block or creating another
structural error must remove the custom minimum-coil error on revalidation;
the native error list is rebuilt each time. Structures with missing or mixed
coils keep their original errors instead of getting an early Hypogen hint.

Audit the coil-gate hook and localization availability through the existing
audit system. Future native checks should exercise incomplete, complete
low-coil and complete accepted-coil structures and the synchronized controller
error, including clearing it when the structure becomes incomplete. The
implementation now uses one common late mixin and native error serialization.
Dev GT .149 and production
GT .205 both expose the same end-of-validation error-list seam.

Sources: [DTPF structure validation][dtpf], [coil levels][coils],
[native translatable errors][structureerrors], [controller error display][structuregui].

### 11. Europium boule requires Neptunium Hexafluoride dust

The EBF boule uses 32 Silicon SG blocks, 2 Gallium Arsenide crystals,
2 Europium ingots and circuit 3, at IV and 6484 K. Its base duration is
900 s before the same eight gas variants are generated; cutting produces
96 wafers. The requested fluoride is an existing GT++ material with
nuclear dust forms registered by `ModItems`.

**Agreed change:** add **2 consumed full Neptunium Hexafluoride dust** as an
extra item input to **all eight gas variants**. Do not replace Silicon SG,
Gallium Arsenide, Europium or the circuit. Preserve every variant's existing
gas, gas quantity, duration, EU/t, heat and boule output. The added stack fits
the EBF's existing input slots. Source cardinality is eight modifications;
verify that count against the final loaded table before defining its audit
contract. Keep the subsequent wafer-cutting recipes unchanged.

Sources: [boule recipes][boules], [dust registration][moditems].

### 12. Protactinium dust in Americium-doped wafer

Protactinium dust already exists. Reactor blanket fuel reprocessing can
produce it as a chance output in the Reactor Processing Unit at IV.

**Agreed change:** add **2 consumed full Protactinium dust** as an extra
input when baking the **Americium-doped boule in EBF**, together with the
Oganesson-only requirement in point 6. Preserve Silicon SG, Gallium Arsenide,
Americium and circuit inputs; the current Oganesson quantity, heat, time,
power and boule output also stay the same. This adds no separate wafer step
or changes to cutting. The intended final table has one Oganesson recipe
with the additive and no remaining alternative-gas recipes for this boule.

Source: [fuel reprocessing][nuclear].

### 13. Force LATEX

`L.A.T.E.X.` means **Laminate Application and Thermal Enclosure eXocoater**:
it is a multiblock **cable coater**, not a latex/raw-rubber extractor.
It uses `cableRecipes`, with rubber discounts based on item-pipe casing tier.
Cable insulation also has assembler routes, and some early cables can be
made with rubber plates in a packager or crafting grid.

**Agreed direction:** remove assembler production of cables **after HV**,
using **EV-rated cables and above** as the starting point. Retain existing
assembler recipes for cables rated HV and below. Remove all matching
assembler variants and cable widths, while keeping their native Cable Coater
map recipes for LATEX.

Determine tier from the output cable's **rated voltage**, using its registered
`MTECable` prototype (`mVoltage`, plus insulated-cable identity). Do not filter
by recipe EU/t, material-processing tier or cable amperage: many insulation
recipes for higher-voltage cables cost only ULV recipe power, and 16x width
changes current capacity rather than voltage tier. Bare-wire recipes and
machine recipes that consume cables remain unaffected.

LATEX's two native controller crafts consume an HV Assembler, two HV pumps,
two EV circuits and dense SBR or Silicone Rubber plates. This supports the
chosen transition out of HV; verify its complete structure/material access
when implementing the cable restriction. Inventory the final assembler table
to count every affected recipe, including dielectric/rubber variants and
addon cables. Audit removal and continued LATEX production rather than
hardcode an assumed count from material names.

The requested mutation is to the assembler map. Keep earlier crafting,
packager and alloy-smelter paths intact. Audit/report any additional way to
produce EV+ insulated cables from wire so a separate pathway does not
silently bypass the intended transition. Do not widen this into a restriction
on already-produced cables without another instruction.

Sources: [LATEX][latex], [cable production alternatives][wire],
[LATEX controller crafting][latexcraft], [cable voltage identity][cableidentity].

### 14. Use Hot Solar Salt as a requirement

**Deferred by the user.** This remains a future idea and is outside the
current implementation batch.

Hot Solar Salt already exists and has functional energy-recovery uses; it
does not need to be registered. The Thermal Boiler consumes 100 mB hot salt
and water to return 100 mB cold salt plus 100000 mB superheated steam.
The Large Heat Exchanger coolant registry and Extreme Heat Exchanger also
support hot/cold solar salt.

Future decision: which new recipe or progression stage should consume it, and
whether cold salt is a byproduct or the hot salt is permanently consumed.
This note does not yet identify a recipe to change.

Sources: [Thermal Boiler][gtpprecipes], [LHE coolant registry][lhe],
[Extreme Heat Exchanger][compact].

### 15. Coolant Tower cools Solar Salt

**Deferred by the user.** Do not add Solar Salt cooling or remove the
existing salt/energy-recovery routes as part of the Ruthenium change below.

The current Coolant Tower has **no ordinary recipe map**. It drains Steam
from input hatches, outputs distilled water at 160:1 and processes on a
20-tick cycle. Adding salt cooling requires an extension to its processing,
not moving a `GTRecipe` into an existing tower map.

Future decisions would be whether this is optional cooling or a mandatory
replacement for the boiler/exchanger cycle, plus salt amount/time and energy
recovery. The map added for point 16 can be reused if this idea is resumed;
no salt recipes are part of the current plan.

Source: [Coolant Tower processing][tower].

### 16. Ruthenium Tetroxide cooled in Coolant Tower instead of distillation

The actual distillation input is **Hot Ruthenium Tetroxide Solution**:
9000 mB becomes 7200 mB Ruthenium Tetroxide, 1800 mB Water and 6 Salt dust,
at HV in 75 s. This is separation with an item byproduct, rather than a
simple hot-fluid-to-cold-fluid conversion. The present Coolant Tower has
fluid hatches, but no output bus for that salt.

**Agreed change:** move this entire recipe **one-to-one** into Coolant Tower,
including the Salt dust and Water output. Do not introduce a separate cooling
intermediate. Preserve all amounts, output chances/metadata, the **75 s base
duration and HV power cost**. Remove the old Distillation Tower entry after
successfully registering its replacement. The reviewed direct registration
does not create distillery copies of this particular recipe; audit the final
loaded maps for any additional bypass.

Extend the existing tower with **one ordinary GT recipe map**, using a normal
recipe backend/frontend and GT's standard NEI recipe handler. Expose that map
from the controller and register the Coolant Tower item as its recipe catalyst
through the usual GT mechanism. Searching recipes for Ruthenium Tetroxide or
uses for the hot solution should lead to the Coolant Tower page. No custom
NEI layout, animation or separate handler framework is needed for this change.

The minimum physical/processing additions are:

- Allow an **Output Bus** in the existing hatch positions for the six Salt
  dust, and **Energy Hatches** to preserve the HV electrical cost.
- Keep fluid input hatches and route both fluid outputs to available output
  hatches, checking capacity before consuming the hot solution.
- For the powered map recipe, require the necessary bus/power connections
  and use normal GT processing for input consumption, progress, energy drain,
  output protection and completion. A missing bus, insufficient energy or
  blocked output must reject processing rather than lose resources.
- Preserve existing **passive Steam -> Distilled Water at 160:1** behavior
  and its 20-tick cycle without requiring those new powered-mode connections.
  New hatches belong to the powered recipe's requirements, not an unconditional
  prerequisite that breaks existing steam-only towers.

`MTECoolantTower.checkProcessing_EM` currently handles Steam directly and its
`onRunningTick` always returns true. Connecting a new map alone would not
consume energy or correctly process its Salt output. The machine patch must
connect mapped processing and running energy flow as well as its hatch
validation, while retaining the native steam path. Its `TTMultiblockBase`
parent already supports a ProcessingLogic path and native power handling;
prefer reusing those facilities over another general recipe engine.

Keep this extension focused on the existing tower and one new map. Register
the map before NEI builds its recipe caches and apply the recipe move after
GT's late queue, using the existing transaction, index maintenance and audit
system. Source expectations are one old recipe removed and one new recipe
added, with identical recipe contents and discoverability under the tower.
Solar Salt remains deferred. Native processing/UI verification belongs to
the later implementation stage.

Sources: [Ruthenium processing][ruthenium], [Coolant Tower][tower],
[native TT processing bridge][ttprocessing].

### 17. Force Manual Transformer

**Resumed and agreed:** disable operation of all GT singleblock voltage
transformers and buck converters, at every tier and in both directions.
Preserve their registration, crafting, machine recipes and use as ingredients
for later machines. Existing placed blocks and NBT-restored blocks must obey
the same restriction. The user explicitly chose to leave IC2 transformers
working after reviewing that possible alternative route.

BartWorks already supplies `MTEManualTrafo`, a variable-height transformer:
one winding layer per voltage tier converted. It exposes direct upstep and
downstep; its two tapped modes are currently disabled. Other routes include
singleblock transformers, Hi-Amp transformers and TecTech Active Transformer.

One targeted common tile mixin closes sided/unsided energy ports, network
participation and legacy energy offers/demands for native Transformer,
Hi-Amp, wet/advanced and Buck Converter families. This prevents stored EU
from being emitted and RF being pulled by Transformer post-tick behavior.
It does not remove saved energy or change the working/upstep switch.
BartWorks Energy Distributor only redistributes voltage unchanged, so it is
excluded. Manual Transformer, TecTech Active Transformer, energy hatches,
generators and other machines retain their native behavior. The startup audit
verifies the hook, registered families and the Manual Transformer exemption.

Native full-pack fixtures verified every registered target in both working
modes, all faces and unsided injection, saved energy after ticks and zero
delivery to a real Energy Hatch. NBT-restored blocks obey the same rule.
The native Manual Transformer hand craft, constructed structure, upstep and
downstep processing still consume input energy and fill the Dynamo Hatch.

Source: [Manual Transformer][trafo].

### 18. GT Printer

**Deferred by the user.** No printer recipe or progression changes are part
of the current batch.

The existing printer has recipes for punch cards, printed pages, maps and
its manual, plus dynamic dye recipes and NBT copied from data sticks.
The registry also defines an Industrial Printer. These machines already
exist; the note does not yet specify the progression item or recipe they
should be required to produce.

Future decisions if resumed: desired printed product, inputs, tier and alternatives to remove.

Sources: [printer recipes][printer], [dynamic printer behavior][printerbackend].

### 19. UU-Matter only with amplifier

**Confirmed scope:** remove production without amplifier in GT's singleblock
Mass Fabricator and GT++'s multiblock Matter Fabricator (Matter Fabrication
CPU), plus both UU-Matter output recipes in the Fluid Extractor. Retain
amplified production and recipes producing amplifier.

| Path | Removal | Retained behavior |
| --- | --- | --- |
| Singleblock Mass Fabricator | Unamplified fake recipe and actual admission without sufficient amplifier | Native amplified processing and amplifier consumption |
| Matter Fabrication CPU | Circuit 1: 16 mB UU, and circuit 3: 256 mB UU, both without fluid inputs | Circuits 2 and 4, consuming the corresponding 16 / 256 mB amplifier |
| Fluid Extractor | One UU-Matter Berry -> 4 mB UU-Matter | UUA Berry -> 4 mB amplifier |
| Fluid Extractor | One Space Flower -> 4 mB UU-Matter | Other Space Flower recipes |

The extraction routes are registered by **CropsNH**, rather than either
mod's main Fluid Extractor loader. Both take 128 ticks at 4 EU/t in CropsNH
2.0.134 and .137. The Space Flower route registers when Galacticraft Core
is present, as in the full pack. Identify their actual registered stacks
and `Materials.UUMatter` fluid, rather than item display names.

The singleblock's `checkRecipe()` does not look up recipes in its fake NEI
table. Removing that entry alone would leave server-side production intact.
Use one targeted early guard that returns the native unmet-requirements
result when the input contains less than the required native amplifier
amount. Run it before output preparation and overclock calculation, then
let existing GT code consume amplifier and process an accepted batch.
Do not blindly flip `requiresUUA`: GT postload omits `nonUUARecipe` when
that setting is enabled, but native processing selects that object before
its later rejection check. The early guard also prevents this null path.

Use the existing audited recipe transactions to remove the singleblock's
unamplified fake entry, the two unamplified multiblock entries, and the two
extraction entries after all late registrations. Check the actual startup
configuration: an already enabled native `requiresUUA` setting means the
fake entry is already absent. Audit that no matching UU output entry remains
in the Fluid Extractor and that every remaining CPU UU-producing recipe
consumes amplifier. Verify retention of amplified recipes, scrap/scrapbox
amplifier production, and the UUA Berry route. Maintain recipe lookup
indexes and NEI visibility through the existing transaction machinery.

This instruction does not extend the ban to native IC2 machines, loot or
unrelated mods' production paths. There is no new global fluid interceptor
or custom fabrication system. Future behavior verification should cover
insufficient amplifier, a normal paid batch and the real CPU recipe map.
Implemented: exact native map removals and singleblock missing/insufficient/paid
admission passed in the disposable full-pack server. Native paid processing
consumes the configured amplifier fee; amplifier-producing routes remain.

Sources: [singleblock processing][mass], [GT fake recipes][postload],
[multiblock recipes][massmulti], [UU-Matter Berry and amplifier Berry][uumberry],
[Space Flower extraction][uumspace], [CropsNH timing helper][croptiming],
[newer client Berry recipe][uumberryclient] and
[newer client Space Flower recipe][uumspaceclient].

### 20. Chisel: raw casing to finished casing

**Confirmed design:** normal Chisel registration from **LV upward**. Each
tier gets a separate group containing exactly two variants: our new raw
machine casing and GT's existing finished machine casing. A hand chisel
must work; standard reverse conversion is allowed. Do not create a special
one-way machine recipe backend.

The registry contains **14 finished tiered casings from LV through MAX**:
LV, MV, HV, EV, IV, LuV, ZPM, UV, UHV, UEV, UIV, UMV, UXV and MAX. Of these,
the reviewed GT/NHCore loaders provide normal crafting and assembler
production for **13 tiers, LV through UXV**. MAX (`Casing_MAXV`) is registered,
but no production recipe was found in those sources. Include its raw variant
and isolated pair as a placeholder, without inventing a production cost.
GT's older `Casing_MAX` name means **UHV**, not MAX. ULV remains outside the
new finishing step. Other casings in the same GT blocks, such as heat-proof
casings or superconductor coils, are unrelated to this tiered set.

Keep this small: **one new raw-casing block with 14 metadata variants** and
its normal subtype ItemBlock, plus one compact tier-to-finished-stack mapping
used by registration and the existing recipe edits. Register the block in
pre-init; resolve GT's finished variants and register groups after the other
mods' registrations. Preserve each finished block's original registry ID,
metadata, use in hull recipes and native structure identity. Raw casings are
distinct blocks and must not satisfy finished-casing recipe ingredients or
structure checks. Do not register them under a finished casing's ore key.
There is no new TileEntity, class per tier or independent progression system.

Use `CarvingUtils.getChiselRegistry().addVariation(...)` with an explicit
block and metadata for each member and a namespaced group name for each tier.
The API can create a missing ordinary group automatically. Do not attach a
broad ore-dictionary group or combine tiers: that could expose other block
variants or permit cross-tier conversion. Confirm the **effective** group
lookup returns only the intended pair for either input, because native
Chisel checks ore-dictionary groups before direct block lookup.

Our existing modifiers already raise single plates to double plates in both
production routes for ULV-UHV:

- Extend [the assembler modifier](../src/main/java/pl/kuba6000/gtnhextranerf/loader/recipes/CoreModRecipeNerf.java)
  to also replace the output with the corresponding raw casing for LV-UHV.
- Extend [the hand-crafting modifier](../src/main/java/pl/kuba6000/gtnhextranerf/loader/recipes/CraftingNerf.java)
  in the same way. Keep its existing double-plate cost and wrench pattern.
- Add the four NHCore UEV-UXV production pairs to the output redirection.
  The initial finishing change preserved their original costs; the later plate
  nerf makes UEV-UMV require eight double plates and UXV one Dense MHDCSM plus
  four Double Magmatter Plates. ULV keeps its double-plate recipe and finished
  output. Match MAX production separately if it appears in a newer runtime.

Thus the LV example becomes **8 double Steel plates + wrench/circuit 8 ->
1 raw LV casing -> Chisel -> 1 existing finished LV casing**. Hull and machine
crafting continue to require that finished casing. Changing the original
output prevents the ordinary crafting-grid route from bypassing finishing.
Source signatures currently give 13 assembler and 13 hand-crafting outputs
to redirect; confirm runtime cardinality and any additional production
alternatives before establishing the audit contract.

Hand chisels, GT++ Auto Chisel, legacy Industrial Chisel and the newer GT
Industrial Chisel all consult the shared carving registry. The latter's
`ChiselBackend` generates machine recipes dynamically and builds a separate
NEI list lazily. Register groups on both client and dedicated server before
that first cache build; verify the final NEI list includes the new pairs.
Native groups supply the conversion and standard machine behavior, so no
custom GUI, NEI handler, conversion mixin or additional network packet is
needed for this step.

Audit variant registration, exact two-member effective groups, same-tier
conversion and redirected outputs, including absence of direct finished
production in the selected crafting/assembler routes. Preserve legitimate
finished-casing recycling and avoid unintended raw-to-finished ore matching.
Missing optional content in a reduced dev environment must use the existing
nonfatal validation policy; missing required integration in the full pack
must produce an audit failure. Future checks should use native hand/machine
conversion, crafting and placement, rather than source-text assertions.
Implemented with the existing casing modifiers, one raw block and native Chisel
groups registered in post-init. The full-pack maps, effective groups and lazy
NEI entries passed; hand crafting keeps the original costs and now yields raw.

Sources: [GT ULV-UHV block variants][tiercasings],
[UEV-MAX block variants][tiercasingshigh],
[NHCore UEV-UXV assembler routes][highcasingassembler],
[NHCore UEV-UXV crafting routes][highcasingcraft],
[native Chisel registration][chiselregistration],
[native group lookup][chiselgroups], [GT++ Auto Chisel][autochisel],
[legacy Industrial Chisel][legacychisel] and [GT recipe/NEI backend][chisel].

### 21. GT machines explode in Toxic Everglades

Added by the user during the recipe review. **Scope is GT and its addons**,
including GT++, BartWorks and TecTech. Other mods' machines and excavators
are outside this rule. The requested effect is native machine destruction
by explosion in the dimension, preventing GT miners and multiblocks from
operating there. Hand mining is not a machine and is not changed by this rule.

Design a single shared hook on the logical server in the native GT machine
tick, before ordinary processing. After a valid MetaTileEntity is available,
a machine in Toxic Everglades invokes `BaseMetaTileEntity.doExplosion` and
returns without running the rest of the tick. Cover both simple machines
and multiblock controllers independently of voltage, energy contents,
work-enabled state and structure formation. New placement, chunk reload and
machines moved into the dimension must all encounter the same check.
GT generators and hatch/bus tile entities share this base. Passive structure
casings are ordinary blocks; destroying the controller prevents the
multiblock from operating. Do not implement this as a placement-only handler
or a recipe-start restriction that existing/moved miners can bypass.

Identify the dimension from its registered Everglades provider/final runtime
identity. `DimensionEverglades.DIMID` starts from the configuration (default
227), but the mod replaces it with the next free ID when it is already
occupied. Never hardcode 227 or rely only on the initial config value. The
integration should activate only when Toxic Everglades is loaded/registered;
its absence in a reduced dev environment is not a fatal error.

Explosion and block removal run **only on the logical server thread**.
Let native world/block/sound updates reach clients; no new networking layer,
background world scan or separate thread is needed. Use native machine
explosion behavior with fixed LV voltage (32 EU, native strength 3), without
scaling the blast with a machine's tier. GT's existing machine-explosion configuration controls terrain damage;
the native default explosion implementation still removes its machine block.
The reviewed TecTech base and GT++ Solar Heater have empty explosion overrides.
The hook removes/blasts a tile surviving native explosion, using the same sound
and terrain setting as the ordinary base implementation.

**Existing certification interaction:** our hook on native
`BaseMetaTileEntity.doExplosion` records a machine explosion and revokes the
global electrical certificate. Forced Everglades machine explosions therefore
also trigger that existing consequence. No exception to certification
revocation has been requested.

Audit the registered dimension identity and hook activation using the
existing audit system. Future native behavior checks should cover machine
destruction before mining/processing, placed and reloaded controllers,
unformed multiblocks, safety in other dimensions, and no client-side world
mutation. Implemented with one common tick mixin and the existing audit system.
The disposable native server verified Steam and electric singleblocks,
generator, reloaded hatch, GT++, BartWorks and TecTech controllers. Overworld
machines and the vanilla furnace remain intact; disabled terrain damage still
removes the banned controller without harming its neighbor. The forced native
explosions also revoke the global electrical certificate.

Sources: [native GT tick and explosion][basemachine],
[native default block removal][machineexplosion], [dimension registration][evergladesdim],
[dimension-ID collision handling][evergladesid],
existing [electrical explosion hook](../src/main/java/pl/kuba6000/gtnhextranerf/mixins/late/MixinElectricalMachineExplosion.java).

## Implementation approach after agreeing the details

Use the existing recipe modules, transactions, lookup-index maintenance and
audit scopes. Add related recipe edits to an appropriate module, or one
focused progression module if needed. Do not introduce a new generic nerf
framework or one class per ingredient.

Run edits after all GT late recipe queues, as the current loader does.
For each agreed change, inventory the final source recipes, match their
actual signatures, require the expected number of matches, and audit the
resulting inputs/outputs and forbidden alternatives. Examples with known
source cardinality are the eight boule gas variants, 12 wet/9 bio PCB
variants, the three Bio Vat variants and two unamplified CPU recipes.
Establish final runtime counts before treating those source counts as
production audit contracts.

Reserve targeted mixins for behavior that maps cannot express: disabled
legacy reactors, coil minimum, local ozone limits, the Everglades machine ban,
Mass Fabricator admission and Coolant Tower processing.
Native Chisel groups handle raw-casing conversion without a lookup mixin;
only their registration order and NEI cache need coordination.
Check caches and NEI alongside actual server processing. Verify resource consumption and output
through native GT integration fixtures when implementing machine behavior;
do not replace those checks with tests of source strings or volatile costs.

The user subsequently approved sequential implementation and the native
recipe, server-machine and Chisel test boundaries. The status table above
tracks completed stages. Points 14, 15 and 18 remain deferred. PCB Factory
uses the existing late Copper-plasma source; ordinary board alternatives stay.
Points **14, 15 and 18 stay out of this batch**. All other agreed changes
are implemented and verified through the approved native boundaries. The
headless server checks do not claim client visual QA. The production build
excludes the opt-in integration fixture classes.

[gt]: https://github.com/GTNewHorizons/GT5-Unofficial/tree/a3e1e11241a814c9fa0dd0973d5699548428f689
[core]: https://github.com/GTNewHorizons/NewHorizonsCoreMod/tree/f769c65f68a352160b1e35770f6f7c579716baa5
[wash]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/loaders/oreprocessing/ProcessingDirty.java#L70
[naq]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/goodgenerator/loader/NaquadahRecipeLoader.java#L398
[accelerators]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/goodgenerator/loader/RecipeLoader.java#L1246
[fusion2]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/common/tileentities/machines/multi/MTEFusionComputer2.java#L77
[compact]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/goodgenerator/loader/RecipeLoader2.java#L1300
[water]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/loaders/postload/chains/PurifiedWaterRecipes.java#L97
[ozone]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/common/tileentities/machines/multi/purification/MTEPurificationUnitOzonation.java#L228
[hatch]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/api/metatileentity/implementations/MTEHatchInput.java#L210
[assembler]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L2644
[mtecraft]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/loaders/load/MTERecipeLoader.java#L2233
[coreassembler]: https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/f769c65f68a352160b1e35770f6f7c579716baa5/src/main/java/com/dreammaster/gthandler/recipes/AssemblerRecipes.java#L805
[corecraft]: https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/f769c65f68a352160b1e35770f6f7c579716baa5/src/main/java/com/dreammaster/gthandler/GT_CraftingRecipeLoader.java#L105
[boules]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/loaders/postload/recipes/BlastFurnaceRecipes.java#L432
[emitter]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/api/util/GTRecipeConstants.java
[gases]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/api/util/BlastFurnaceGasStat.java#L14
[cutting]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/loaders/postload/recipes/CuttingRecipes.java#L98
[pcb]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/loaders/postload/chains/PCBFactoryRecipes.java#L410
[plastics]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/loaders/postload/PCBFactoryMaterialLoader.java#L10
[godforge]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/tectech/loader/recipe/Godforge.java#L130
[skips]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gtPlusPlus/xmod/gregtech/loaders/recipe/RecipeLoaderChemicalSkips.java#L374
[gtpprecipes]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gtPlusPlus/core/recipe/RecipesGregTech.java
[solder]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gtPlusPlus/core/item/chemistry/RecipeLoaderGenericChem.java#L151
[dtpf]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/common/tileentities/machines/multi/MTEPlasmaForge.java#L794
[coils]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/api/enums/HeatingCoilLevel.java#L23
[moditems]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gtPlusPlus/core/item/ModItems.java#L498
[nuclear]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gtPlusPlus/xmod/gregtech/loaders/recipe/RecipeLoaderNuclearFuelProcessing.java#L69
[latex]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/common/tileentities/machines/multi/MTELatex.java
[wire]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/loaders/oreprocessing/ProcessingWire.java#L232
[lhe]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/api/registries/LHECoolantRegistry.java#L60
[tower]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/goodgenerator/blocks/tileEntity/MTECoolantTower.java#L140
[ruthenium]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/bartworks/system/material/gtenhancement/PlatinumSludgeRecipes.java#L501
[trafo]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/bartworks/common/tileentities/multis/MTEManualTrafo.java#L159
[printer]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/loaders/postload/recipes/PrinterRecipes.java
[printerbackend]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/api/recipe/maps/PrinterBackend.java#L36
[mass]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/common/tileentities/machines/basic/MTEMassfabricator.java#L168
[postload]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/loaders/postload/GTPostLoad.java#L298
[massmulti]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gtPlusPlus/xmod/gregtech/registration/gregtech/GregtechIndustrialMassFabricator.java#L74
[chisel]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/api/recipe/maps/ChiselBackend.java#L52
[naqline]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/goodgenerator/loader/NaquadahRecipeLoader.java#L235
[watercontrollers]: https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/f769c65f68a352160b1e35770f6f7c579716baa5/src/main/java/com/dreammaster/gthandler/recipes/AssemblingLineRecipes.java#L1193
[clarifier]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/common/tileentities/machines/multi/purification/MTEPurificationUnitClarifier.java#L127
[flocculator]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/common/tileentities/machines/multi/purification/MTEPurificationUnitFlocculation.java#L419
[maps]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/api/recipe/RecipeMaps.java
[fusionbase]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/common/tileentities/machines/multi/MTEFusionComputer.java#L80
[becblock]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/common/blocks/BlockCasingsBEC.java#L17
[becrecipe]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/loaders/postload/recipes/BECRecipes.java#L340
[unitbase]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/common/tileentities/machines/multi/purification/MTEPurificationUnitBase.java#L313
[naqmaterials]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/goodgenerator/items/GGMaterial.java#L869
[dustloader]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/bartworks/system/material/werkstoff_loaders/recipe/DustLoader.java#L195
[noncell]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gtPlusPlus/xmod/gregtech/loaders/RecipeGenMultisUsingFluidInsteadOfCells.java#L55
[biomedium]: https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/f769c65f68a352160b1e35770f6f7c579716baa5/src/main/java/com/dreammaster/bartworksHandler/BacteriaRegistry.java#L180
[biovat]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/bartworks/common/tileentities/multis/MTEBioVat.java#L307
[biovatmap]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/bartworks/API/recipe/BartWorksRecipeMaps.java#L46
[evergladesores]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/toxiceverglades/GTPPEverglades.java#L107
[evergladesveins]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/api/enums/OreMixes.java#L1130
[mineralbeam]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/loaders/postload/recipes/beamcrafter/BeamCrafterRecipes.java#L178
[basemachine]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/api/metatileentity/BaseMetaTileEntity.java#L266
[machineexplosion]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/api/metatileentity/MetaTileEntity.java
[evergladesdim]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/toxiceverglades/dimension/DimensionEverglades.java#L15
[evergladesid]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/toxiceverglades/GTPPEverglades.java#L142
[nuclearstar]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/loaders/postload/recipes/AutoclaveRecipes.java#L201
[uevhatch]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/tectech/loader/recipe/ResearchStationAssemblyLine.java#L215
[multiautoclave]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/common/tileentities/machines/multi/MTEMultiAutoclave.java
[structureerrors]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/api/structure/error/StructureErrors.java#L14
[structuregui]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/common/gui/modularui/multiblock/base/MTEMultiBlockBaseGui.java#L357
[latexcraft]: https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/f769c65f68a352160b1e35770f6f7c579716baa5/src/main/java/com/dreammaster/gthandler/GT_CraftingRecipeLoader.java#L806
[cableidentity]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/api/metatileentity/implementations/MTECable.java
[ttprocessing]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/tectech/thing/metaTileEntity/multi/base/TTMultiblockBase.java#L267
[uumberry]: https://github.com/GTNewHorizons/CropsNH/blob/1d2936f53192b2648a853ab529df758fd40a4f20/src/main/java/com/gtnewhorizon/cropsnh/loaders/gtrecipes/CropRecipes.java#L942
[uumspace]: https://github.com/GTNewHorizons/CropsNH/blob/1d2936f53192b2648a853ab529df758fd40a4f20/src/main/java/com/gtnewhorizon/cropsnh/loaders/gtrecipes/CropRecipes.java#L1015
[croptiming]: https://github.com/GTNewHorizons/CropsNH/blob/1d2936f53192b2648a853ab529df758fd40a4f20/src/main/java/com/gtnewhorizon/cropsnh/loaders/gtrecipes/BaseGTRecipeLoader.java#L16
[uumberryclient]: https://github.com/GTNewHorizons/CropsNH/blob/a8f9898eefa9ea752bec50c3946d8c9052467dc7/src/main/java/com/gtnewhorizon/cropsnh/loaders/gtrecipes/CropRecipes.java#L948
[uumspaceclient]: https://github.com/GTNewHorizons/CropsNH/blob/a8f9898eefa9ea752bec50c3946d8c9052467dc7/src/main/java/com/gtnewhorizon/cropsnh/loaders/gtrecipes/CropRecipes.java#L1021
[tiercasings]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/common/blocks/BlockCasings1.java#L38
[tiercasingshigh]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gregtech/common/blocks/BlockCasingsNH.java#L30
[highcasingassembler]: https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/f769c65f68a352160b1e35770f6f7c579716baa5/src/main/java/com/dreammaster/gthandler/recipes/AssemblerRecipes.java#L805
[highcasingcraft]: https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/f769c65f68a352160b1e35770f6f7c579716baa5/src/main/java/com/dreammaster/gthandler/GT_CraftingRecipeLoader.java#L105
[chiselregistration]: https://github.com/GTNewHorizons/Chisel/blob/d08e48b73ba53d50817cccd9e655be659373be24/src/main/java/team/chisel/carving/Carving.java#L146
[chiselgroups]: https://github.com/GTNewHorizons/Chisel/blob/d08e48b73ba53d50817cccd9e655be659373be24/src/main/java/team/chisel/carving/Carving.java#L69
[autochisel]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gtPlusPlus/xmod/gregtech/common/tileentities/machines/basic/MTEAutoChisel.java#L152
[legacychisel]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gtPlusPlus/xmod/gregtech/common/tileentities/machines/multi/processing/MTEIndustrialChiselLegacy.java#L222
[boardalternatives]: https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/f769c65f68a352160b1e35770f6f7c579716baa5/src/main/java/com/dreammaster/gthandler/recipes/CircuitAssemblerRecipes.java#L818
[boardfinishing]: https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/f769c65f68a352160b1e35770f6f7c579716baa5/src/main/java/com/dreammaster/gthandler/recipes/ChemicalReactorRecipes.java#L191
