---
item_ids:
  - gregtech:gt.blockmachines:32410
  - gtnhextranerf:iron_bath_casing:0
  - gtnhextranerf:iron_bath_casing:1
navigation:
  title: Electric Iron Bath
  parent: ../index.md
  icon: gregtech:gt.blockmachines:32410
  position: 10
  keywords:
    - steel
    - iron bath
    - furnace
    - carbon
---

# Electric Iron Bath

<ItemImage id="gregtech:gt.blockmachines:32410" />

The LV Electric Iron Bath produces molten Iron, Wrought Iron, Steel, Cast Iron, Pig Iron and Overcarburized Iron. It stores metal, carbon, heat and slag. Composition determines the metal tapped. It is the main early industrial source of Steel. The Primitive Blast Furnace provides a slower way to get started.

The structure is <Color color="#89DCEB">13 blocks wide, 12 high and 7 deep</Color>. The controller sits beside the raised feed conveyor. Leave access for maintenance and pipework.

<GameScene width="420" height="320" interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:32410" />
</GameScene>

Use the preview or StructureLib hints to inspect the shape. All coils must match.

## Parts and ports

| Part | &nbsp;Location or purpose |
| --- | --- |
| Electric Iron Bath controller | &nbsp;Side platform beside the conveyor |
| <Color color="#89DCEB">50</Color> matching heating coils | &nbsp;Chimney base and exposed coil bands |
| <Color color="#89DCEB">1</Color> input bus | &nbsp;Side platform, for metal, carbon additives and Calcite |
| <Color color="#89DCEB">1</Color> fluid input hatch | &nbsp;Side platform, for Oxygen |
| <Color color="#89DCEB">1</Color> maintenance hatch | &nbsp;Side platform |
| <Color color="#89DCEB">1</Color> to <Color color="#89DCEB">8</Color> energy hatches | &nbsp;Chimney base |
| <Color color="#89DCEB">1</Color> or more Bath Tapping Hatches | &nbsp;Narrow chimney walls beside the coils |
| Bath Measurement Hatch | &nbsp;Optional port behind the controller platform |

Start with Cupronickel coils and Steel-Bound Refractory Casings. Wrought Iron-Reinforced Casings reduce heat loss by <Color color="#A6E3A1">50%</Color> for the whole shell, or <Color color="#A6E3A1">25%</Color> for half.

<RecipesFor id="gregtech:gt.blockmachines:32410" />

## Example: making Steel

NEI lists reference recipes. Carbon burns away in a hot bath, so adjust the mixture as you run it.

For Steel:

1. Build with Cupronickel coils, repair maintenance, and install low metal and high slag taps
2. Connect two LV energy hatches for <Color color="#F9E2AF">120 EU/t</Color> and set taps to <Color color="#BAC2DE">**Stopped**</Color>
3. Add <Color color="#89DCEB">100 Iron Ingots or Iron Dusts</Color> and <Color color="#89DCEB">1 Calcite Dust</Color>. Wait until fully liquid
4. Add <Color color="#89DCEB">1 Coke</Color> to carburize the bath
5. When the highlighted product band reads Steel, enable the low tap and send molten Steel to a Fluid Solidifier with an Ingot Mold

Check composition in the controller or with output samples. Add carbon to raise it or oxygen to lower it.

Wrought Iron is used in Stainless Steel mixtures and reinforced bath casings, Cast Iron in Electrical Steel mixtures, and Pig Iron in Dark Steel mixtures.

Continue with [Carbon, Calcite and products](chemistry.md) or [Power, tapping and automation](operation.md).
