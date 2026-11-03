# AE2 progression

New rules apply to recipes producing items registered by Applied Energistics 2:

- Titanium ingredients become TPV in the same form and quantity, including
  plates, screws and molten metal. Other alloys such as Niobium-Titanium remain
  separate materials. Addon-owned outputs are outside this rule.
- ME Controller has no crafting or Assembler recipe. It is produced in the
  Assembly Line after scanning a 64k ME Storage Cell.
- The Blank Pattern crafting recipe uses three Sunnarium plates instead of three
  Glowstone plates, preserving its Quartz Glass, Certus Quartz and Aluminium.

## ME Controller Assembly Line recipe

The NHCore 2.9.62 Assembler recipe makes one Controller from four Titanium
plates, two HV circuits, two Engineering Processors and one Fluix Block, using
configuration circuit 2, 100 ticks and 480 EU/t. It has no fluid inputs.

Assembly Line inputs:

| Input | Amount |
| --- | ---: |
| TPV plate | 4 |
| Any HV circuit | 2 |
| AE2 Engineering Processor | 2 |
| Fluix Block | 1 |
| Molten Soldering Alloy | 576 L |

Output: one ME Controller. Processing: 30 seconds at 30,720 EU/t
(LuV, `TierEU.RECIPE_LuV`). The configuration circuit is not carried over as a
consumed component.

Research item: one 64k ME Storage Cell, scanned for 60 seconds at 480 EU/t (HV)
with a Data Stick. Standard GregTech scanning consumes the cell and writes the
Controller research to the Data Stick. It ignores cell NBT: a cell containing
items is also accepted and consumed with its contents. No empty-cell check or
additional Scanner mixin is installed.

## Spatial Assembly Lines

Both ordinary and Advanced Assembly Lines require AE2 Spatial Storage worlds.
Their 3-by-4 cross section rules out the 2-cubed cell; the next 16-cubed cell
fits the standard 5-slice structure and supports up to 16 slices. A compact
Spatial network can remain within the controllerless 8-channel limit: each
connected pylon cluster counts as a single multiblock channel. Thus the
Controller's Assembly Line recipe is not itself required to enter Spatial.

The restriction is discovered through the controller only after the complete
structure validates.

## Implementation and verification

`Ae2RecipeNerf` owns the load-complete transactions. A shared material resolver
handles native crafting arguments and GT item/fluid inputs. Three wrappers cover
the NHCore AE2 script's shaped, shapeless and direct GT crafting registration;
there are no per-recipe ordinals. Missing TPV forms fail validation.
The Controller Assembly Line recipe is registered through GregTech's native
builder, including its Scanner and NEI entries, in a separate audited step.

The Forge integration fixture inspects final crafting and machine recipes,
checks the real Assembly Line list and resolves the Scanner's resulting Data
Stick back to the Controller recipe. It checks that Redstone Certus recipes remain,
and verifies that replacing a fluid preserves its amount, chances, other fluid
alternatives, duration and voltage. It also retains all previous recipe tests.

The implementation targets GT5 5.09.54.261, NHCore 2.9.99 and NEI 2.8.171-GTNH.
Only NHCore's current GT crafting overload with recipe flags is supported;
there is no fallback handler or compatibility group for the old overload.
