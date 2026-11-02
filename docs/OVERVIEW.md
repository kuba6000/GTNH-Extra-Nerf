# Project overview

GTNH Extra Nerf is a difficulty add-on for GregTech: New Horizons on Minecraft
1.7.10. It makes power generation less efficient, increases material costs,
slows or removes selected processing routes, and tightens several early- and
late-game progression shortcuts.

It is intentionally made for a specific GTNH dependency snapshot rather than
as a general-purpose GregTech configuration mod.

## What the mod changes

The current project contains:

- reduced efficiency for steam, gas, diesel, plasma and semi-fluid power;
- a harsher steam conversion ratio;
- a 640,000 L Steam Input Hatch buffer for steam multiblocks;
- shorter nuclear fuel-rod lifetime;
- lower Naquadah small-ore generation;
- restricted early Steel production and reworked MV blast-furnace routes;
- modified aluminium, ilmenite, titanium, silicon, platinum, samarium and
  Indium processing;
- more expensive circuits, boards, machine casings and endgame components;
- physical Integrated Circuits below HV and built-in ghost selection from HV
  onward;
- changed crafting requirements for selected machines and utility items;
- removed shortcut recipes;
- removed slime processing and EMT Glue replacements for selected slime-bound
  crafting routes;
- PPS foil requirements for selected cable assembler recipes;
- safety certification gates for steam and electric machines and hatches, earned with
  an in-game exam or by payment (see [`PROGRESSION.md`](PROGRESSION.md));
- a prototype Industrial Brainrotter multiblock and a server-wide limit on ME
  Controllers from lootbags.

The mod also registers eight unavailable UQV (Ultimately Questionable Voltage)
components with UEV appearance. They are hidden from NEI search and Creative
tabs and can be used as exact recipe ingredients. See
[`COMPONENTS.md`](COMPONENTS.md) for the catalog and extension points.

The complete values are listed in [`NERFS.md`](NERFS.md).

## How it works

Some changes must happen while GregTech or another mod is registering its
content. These use mixins to alter constructor arguments, recipe builders or
machine methods.

Other changes run after all mods finish registering recipes. GTNH Extra Nerf
locates the exact expected recipe, removes it, mutates it and registers the
replacement.

The mod checks its own work during startup. If a required target is missing,
appears more times than expected, or refuses the replacement, a production
launch stops with a `GTNH Extra Nerf integrity check failed` message. A
deobfuscated development launch logs the missing patches and continues, which
allows reduced local environments to boot without weakening production checks.

## Configuration

There are currently no per-nerf options. The generated Forge configuration
file contains no gameplay settings. The balance package is applied as one
fixed ruleset.

## Project status

The project builds and its automated tests pass against the pinned dependency
snapshot. Because many changes depend on exact upstream recipes and method
structure, updating GTNH components requires a deliberate compatibility port
and a full server test.

Historical documents used to mention a shears change. It does not exist in the
current source and is not part of the active mod behavior. The Steel production
overhaul is implemented and active.
