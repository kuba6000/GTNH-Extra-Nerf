---
ore_ids:
  - ingotWroughtIron
  - dustWroughtIron
  - ingotCastIron
  - dustCastIron
  - ingotPigIron
  - dustPigIron
  - ingotOvercarburizedIron
  - dustOvercarburizedIron
navigation:
  title: Carbon, Calcite and products
  parent: index.md
  icon: minecraft:coal
  position: 20
---

# Carbon, Calcite and products

The bath accepts ingots and full dusts of the metals below. Magnetic Iron and Magnetic Steel count as Iron and Steel.

## Reading C/Fe

The controller shows carbon relative to iron:

- <Color color="#89DCEB">144 mB of metal</Color> counts as one iron ingot equivalent
- <Color color="#CBA6F7">100 carbon units</Color> count as one carbon ingot equivalent

For example, 100 Iron Ingots and 1 Coke give <Color color="#CBA6F7">1% C/Fe</Color> before carbon loss. At Steel's reference composition, casting <Color color="#89DCEB">144 mB</Color> removes <Color color="#CBA6F7">1 carbon unit</Color>.

## Composition bands

| Product | &nbsp;C/Fe shown by the controller | &nbsp;Carbon units carried by one output ingot |
| --- | --- | --- |
| Iron | &nbsp;<Color color="#CBA6F7">Exactly 0%</Color> | &nbsp;<Color color="#CBA6F7">0</Color> |
| Wrought Iron | &nbsp;<Color color="#CBA6F7">Above 0%, up to and including 0.2%</Color> | &nbsp;<Color color="#CBA6F7">0.2</Color> |
| Steel | &nbsp;<Color color="#CBA6F7">Above 0.2%, below 2%</Color> | &nbsp;<Color color="#CBA6F7">1</Color> |
| Cast Iron | &nbsp;<Color color="#CBA6F7">At least 2%, below 3.5%</Color> | &nbsp;<Color color="#CBA6F7">2.75</Color> |
| Pig Iron | &nbsp;<Color color="#CBA6F7">At least 3.5%, below 4.5%</Color> | &nbsp;<Color color="#CBA6F7">4</Color> |
| Overcarburized Iron | &nbsp;<Color color="#CBA6F7">At least 4.5%</Color> | &nbsp;<Color color="#CBA6F7">4.5</Color> |

Tapped metal carries its reference carbon content, so composition can drift. Larger baths change more slowly.

## Carbon additives

Use Coal and Charcoal items or full dusts, and registered Coke fuel items.

| Additive | &nbsp;Carbon units per item | &nbsp;Impurity units per item |
| --- | --- | --- |
| Coke | &nbsp;<Color color="#CBA6F7">100</Color> | &nbsp;<Color color="#F38BA8">1</Color> |
| Coal | &nbsp;<Color color="#CBA6F7">100</Color> | &nbsp;<Color color="#F38BA8">5</Color> |
| Charcoal | &nbsp;<Color color="#CBA6F7">80</Color> | &nbsp;<Color color="#F38BA8">10</Color> |
| Cactus Coke | &nbsp;<Color color="#CBA6F7">25</Color> | &nbsp;<Color color="#F38BA8">1</Color> |
| Sugar Coke | &nbsp;<Color color="#CBA6F7">25</Color> | &nbsp;<Color color="#F38BA8">1</Color> |

Carbon additives enter the bath directly from the input bus and don't require melting.

## Material reference batches

Ratios give **metal ingots : additive items**, using the smallest whole-item balance for each output. Feed large batches within capacity.

| Product | &nbsp;Coal or Coke | &nbsp;Charcoal | &nbsp;Cactus or Sugar Coke |
| --- | --- | --- | --- |
| Wrought Iron | &nbsp;<Color color="#89DCEB">500 : 1</Color> | &nbsp;<Color color="#89DCEB">400 : 1</Color> | &nbsp;<Color color="#89DCEB">125 : 1</Color> |
| Steel | &nbsp;<Color color="#89DCEB">100 : 1</Color> | &nbsp;<Color color="#89DCEB">80 : 1</Color> | &nbsp;<Color color="#89DCEB">25 : 1</Color> |
| Cast Iron | &nbsp;<Color color="#89DCEB">400 : 11</Color> | &nbsp;<Color color="#89DCEB">320 : 11</Color> | &nbsp;<Color color="#89DCEB">100 : 11</Color> |
| Pig Iron | &nbsp;<Color color="#89DCEB">25 : 1</Color> | &nbsp;<Color color="#89DCEB">20 : 1</Color> | &nbsp;<Color color="#89DCEB">25 : 4</Color> |
| Overcarburized Iron | &nbsp;<Color color="#89DCEB">200 : 9</Color> | &nbsp;<Color color="#89DCEB">160 : 9</Color> | &nbsp;<Color color="#89DCEB">50 : 9</Color> |

NEI shows Calcite and slag amounts.

## Calcite and slag

One Calcite Dust supplies <Color color="#89DCEB">20 binding units</Color>. At <Color color="#FAB387">1700 K or above</Color>, each unit binds one impurity and produces <Color color="#89DCEB">32 mB of slag</Color>.

Drain the [slag reserve](operation.md#slag-reserve) to leave room for binding.

| Additive | &nbsp;Calcite Dust consumed per item | &nbsp;Slag produced per item |
| --- | --- | --- |
| Coke | &nbsp;<Color color="#A6E3A1">0.05</Color> | &nbsp;<Color color="#89DCEB">32 mB</Color> |
| Coal | &nbsp;<Color color="#A6E3A1">0.25</Color> | &nbsp;<Color color="#89DCEB">160 mB</Color> |
| Charcoal | &nbsp;<Color color="#A6E3A1">0.5</Color> | &nbsp;<Color color="#89DCEB">320 mB</Color> |
| Cactus Coke | &nbsp;<Color color="#A6E3A1">0.05</Color> | &nbsp;<Color color="#89DCEB">32 mB</Color> |
| Sugar Coke | &nbsp;<Color color="#A6E3A1">0.05</Color> | &nbsp;<Color color="#89DCEB">32 mB</Color> |

Feed whole Calcite Dusts. Unused binding units remain in the reserve.

<strong>Metal tapping requires zero unbound impurities.</strong> Keep Calcite stocked and drain slag through a high tap. Cast <Color color="#89DCEB">144 mB</Color> into one Slag Dust in a Fluid Solidifier with circuit <Color color="#89DCEB">1</Color>.

## Lowering the carbon content

Each <Color color="#89DCEB">32 mB of Oxygen</Color> removes <Color color="#CBA6F7">1 carbon unit</Color> when it contacts liquid metal. The flow limit increases by <Color color="#89DCEB">20 mB/s</Color> for every full <Color color="#F9E2AF">60 EU/t</Color> of melting power.

At <Color color="#F9E2AF">120 EU/t</Color>, the limit is <Color color="#89DCEB">40 mB/s</Color>, removing up to <Color color="#CBA6F7">1.25 carbon units per second</Color>. Oxygen keeps flowing at the same rate after carbon runs out.
