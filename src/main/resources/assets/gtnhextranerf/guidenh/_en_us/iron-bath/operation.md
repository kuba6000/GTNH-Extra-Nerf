---
item_ids:
  - gregtech:gt.blockmachines 32411-32425
navigation:
  title: Power, tapping and automation
  parent: index.md
  icon: gregtech:gt.blockmachines:32411
  position: 30
---

# Power, tapping and automation

Use <Color color="#89DCEB">1 to 8 energy hatches</Color>.

## Scaling production

Higher-tier energy hatches give normal [EBF overclocks](gregtech:multiblocks/electric_blast_furnace.md): <Color color="#A6E3A1">twice the melting speed</Color> for <Color color="#F9E2AF">four times the power</Color>. More power at the same tier adds **parallel**, increasing throughput without increasing melting energy per item.

Coil bonuses use a fixed <Color color="#FAB387">1700 K</Color> melting requirement. Compare the coil's rated heat, plus <Color color="#FAB387">100 K</Color> per voltage tier above LV, against this requirement:

- Every full <Color color="#FAB387">900 K</Color> of excess heat reduces melting energy by <Color color="#A6E3A1">5%</Color>, with each reduction applied to the remaining cost
- Every full <Color color="#FAB387">1800 K</Color> replaces one available normal overclock with a **perfect overclock**: <Color color="#A6E3A1">four times the speed</Color> for <Color color="#F9E2AF">four times the power</Color>

These bonuses do not depend on the bath's current temperature or its temperature ceiling.

## Coil capacity

Cupronickel holds <Color color="#89DCEB">512 ingots</Color>. Each coil upgrade doubles capacity and raises the ceiling by <Color color="#FAB387">900 K</Color>.

| Coils | &nbsp;Total capacity, ingots | &nbsp;Metal limit, ingots | &nbsp;Temperature ceiling |
| --- | --- | --- | --- |
| Cupronickel | &nbsp;<Color color="#89DCEB">512</Color> | &nbsp;<Color color="#89DCEB">496</Color> | &nbsp;<Color color="#FAB387">2200 K</Color> |
| Kanthal | &nbsp;<Color color="#89DCEB">1024</Color> | &nbsp;<Color color="#89DCEB">992</Color> | &nbsp;<Color color="#FAB387">3100 K</Color> |
| Nichrome | &nbsp;<Color color="#89DCEB">2048</Color> | &nbsp;<Color color="#89DCEB">1984</Color> | &nbsp;<Color color="#FAB387">4000 K</Color> |
| TPV-Alloy | &nbsp;<Color color="#89DCEB">4096</Color> | &nbsp;<Color color="#89DCEB">3968</Color> | &nbsp;<Color color="#FAB387">4900 K</Color> |

### Slag reserve

Metal in the bath and heating queue can fill <Color color="#89DCEB">31/32 of the tank</Color>, leaving <Color color="#89DCEB">1/32</Color> for slag. Carbon can still be added at the metal limit. Drain slag to leave room for binding impurities.

## Heating and Calcite reserve

Admitted metal shares available heating power by item count. Metal in the bath and queue is limited by tank capacity. The controller shows the total number of melting items and groups items with identical data and progress.

The Calcite reserve holds <Color color="#89DCEB">180 binding units</Color> with Cupronickel coils, plus <Color color="#89DCEB">160 units</Color> per coil upgrade. Each Calcite Dust supplies <Color color="#89DCEB">20 units</Color>. The reserve refills from the input bus. If a coil downgrade leaves it above the new limit, stored units remain and refilling pauses until it falls below the limit.

## Temperature and restarting

The bath heats toward its coil ceiling. Cold charges absorb energy before joining it. Coil bonuses set melting throughput, while bath temperature controls liquid fraction, heat loss and natural carbon loss.

Metal and slag melt or freeze at <Color color="#FAB387">1700 K</Color>. Carbon burns off only from molten metal at this temperature or above. Restore power to reheat a frozen bath for tapping. Large stocks take more time and energy to reheat.

## Keeping the bath hot

At the coil ceiling, standby costs <Color color="#F9E2AF">30 EU/t</Color> with Cupronickel coils and doubles with each coil upgrade. Heat loss falls linearly as the bath cools toward ambient temperature. Reinforced walls reduce losses by up to <Color color="#A6E3A1">50%</Color>. More or higher-tier energy hatches do not increase standby costs, but more power heats the bath faster.

Active heating and melting lose <Color color="#F38BA8">10%</Color> of their working energy. Reinforced walls reduce that loss linearly to <Color color="#A6E3A1">5%</Color> at full coverage.

Switching off the controller stops heating, intake and tapping. Unreplaced heat loss removes twice the heat needed for powered standby.

## Natural carbon loss

Liquid metal loses carbon according to molten volume and temperature. Coil and voltage tiers do not change the rate per molten ingot at a given temperature.

| Bath temperature | &nbsp;Carbon units lost per molten ingot per second |
| --- | --- |
| <Color color="#FAB387">1700 K</Color> | &nbsp;<Color color="#CBA6F7">0.001</Color> |
| <Color color="#FAB387">1800 K</Color> | &nbsp;<Color color="#CBA6F7">0.002</Color> |
| <Color color="#FAB387">2200 K</Color> | &nbsp;<Color color="#CBA6F7">0.004</Color> |
| <Color color="#FAB387">Above 2200 K</Color> | &nbsp;<Color color="#CBA6F7">0.004 times sqrt(temperature / 2200)</Color> |

Multiply by molten ingots for total loss. Faster production reduces the time each batch loses carbon.

## Tapping

Each Bath Tapping Hatch covers one of five equal vertical bands, each spanning 20% of the tank. Metal sits below slag.

| Height above controller | &nbsp;Tank band |
| --- | --- |
| 3 blocks | &nbsp;<Color color="#89DCEB">0-20%</Color> |
| 4 blocks | &nbsp;<Color color="#89DCEB">20-40%</Color> |
| 5 blocks | &nbsp;<Color color="#89DCEB">40-60%</Color> |
| 6 blocks | &nbsp;<Color color="#89DCEB">60-80%</Color> |
| 7 blocks | &nbsp;<Color color="#89DCEB">80-100%</Color> |

If any liquid slag overlaps the hatch's band, it selects slag. Otherwise it selects liquid metal in that band. It waits if neither is present. As the layers move, the same hatch can switch between slag and metal.

The hatch pours a batch in steps. Each step draws up to 10% of the hatch tier's maximum batch size, rounded up to the next mB. The first step can start immediately. Later steps are at least five ticks apart, even across batches.

Drain the outlet completely to allow the next step. Metal already in the hatch keeps its type. When the outlet clears, the hatch draws whichever liquid is now at its height. One batch can therefore contain several fluids. If no liquid is at the hatch's height, the rest of the batch is cancelled.

## Amount and redstone controls

Set **Amount** in the hatch GUI, from <Color color="#89DCEB">1 mB</Color> to the tier limit per discharge:

| Hatch tier | &nbsp;Maximum amount per discharge |
| --- | --- |
| LV | &nbsp;<Color color="#89DCEB">144 mB</Color> |
| MV | &nbsp;<Color color="#89DCEB">576 mB</Color> |
| HV | &nbsp;<Color color="#89DCEB">2304 mB</Color> |
| etc. | &nbsp;<Color color="#89DCEB">×4 per tier</Color> |

<Color color="#89DCEB">**On pulse**</Color> starts one batch on a rising edge. Extra pulses during that batch are ignored. <Color color="#A6E3A1">**Always**</Color> repeats batches, and <Color color="#F9E2AF">**Powered**</Color> pauses when the redstone signal drops. <Color color="#BAC2DE">**Stopped**</Color> cancels the remaining amount.

A screwdriver cycles amounts through <Color color="#89DCEB">1, 16, 144 and the tier maximum</Color>. Sneak to cycle modes. The GUI allows exact amounts and direct mode selection.

## Bath Measurement Hatch

<ItemImage id="gregtech:gt.blockmachines:32425" />

The LuV Bath Measurement Hatch can replace any refractory casing on the chimney. Connect it to an OpenComputers Adapter. The **iron_bath_measurement** component's **read()** call returns a table for the formed bath.

| Reading | &nbsp;Unit or meaning |
| --- | --- |
| metalMb, slagMb | &nbsp;Total stored material, mB |
| liquidMetalMb, liquidSlagMb | &nbsp;Molten portion, mB |
| carbonUnits, impurityUnits, calciteUnits | &nbsp;Internal material and binding units |
| temperatureK | &nbsp;Bath temperature, kelvin |
| capacityMb | &nbsp;Total bath capacity, mB |
| pendingMetalMb | &nbsp;Metal in the heating queue, mB |
| meltingItems | &nbsp;Number of items in the heating queue |
| melting | &nbsp;True while the heating queue contains items |
| chargeGroups | &nbsp;Array of groups with item count and progress from 0 to 1 |
| product | &nbsp;Current metal product name |

A disconnected or unformed hatch returns nil and an error.
