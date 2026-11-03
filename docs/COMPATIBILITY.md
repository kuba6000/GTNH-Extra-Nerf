# Compatibility

## Supported baseline

| Component | Current project baseline |
| --- | --- |
| Minecraft | 1.7.10 |
| Forge | 10.13.4.1614 |
| GT5-Unofficial dependency and target runtime | 5.09.54.261 |
| NewHorizonsCoreMod dependency and target runtime | 2.9.99 |
| NotEnoughItems dependency and target runtime | 2.8.171-GTNH |
| EnhancedLootBags development dependency and supported runtime | 1.3.5 |

The build produces Java 8-compatible mod bytecode even though modern JDKs are
used by Gradle.

Compilation, reduced dev runs and full-pack validation target this single
upstream baseline. Older GT versions are not a supported compatibility target;
there are no version-selected alternative mixins. In this GT version, legacy
fusion tooltips use the actual casing/coil getters in the shared parent, so
the mod only changes those getters and audits the resulting native tooltip.

Large Chemical Reactor descriptions use GregTech's Markdown tooltip loader.
Extra Nerf points that machine's Markdown resource at its own description,
preserving the machine information and describing the existing 3/4 overclock.
The mixin requires the current `addMarkdown(ResourceLocation)` call; it does
not also target the removed `addPerfectOCInfo()` call from older GT versions.

Use `check build` for unit and build verification and the Horizon-QA client
suite for native recipe, loot, progression, machine and GUI behavior. A valid
scenario run includes consistent JSON/JUnit evidence and a clean process exit.
See [Building](BUILDING.md) for commands and selectors.

## Intended environment

This project is intended for the complete GregTech: New Horizons pack matching
the pinned dependencies. It directly interacts with content from:

- GregTech;
- NewHorizonsCoreMod/Dreamcraft;
- BartWorks;
- Good Generator;
- GT++;
- GTNH Lanthanides;
- TConstruct;
- Thaumcraft and Botania for the MV motor's Arcane Workbench ingredient;
- Extra Utilities and HarvestCraft when present;
- Forestry-dependent recipes when Forestry is present.

The Forge annotation currently lists GregTech, Dreamcraft and BartWorks as
required. That metadata does not fully express every class referenced by the
implementation, so a reduced custom installation is not a supported target.

## Optional behavior

- Chromatic Tooltips 1.0.36-GTNH is compile-only. The full pack's Chromatic
  Tooltips Compat 1.0.39-GTNH replaces NEI and ModularUI2 item rendering, so the
  material image uses a native Chromatic tooltip section. Its listener is loaded
  only on clients with Chromatic present; reduced clients retain the legacy hooks.

- Custom Main Menu 1.14.1 is a compile-only dependency. When installed on the
  client, the mod adds a native Extra Nerf badge to its main menu before GUI
  initialization. CMM remains optional, is not bundled or required by servers,
  and the vanilla menu is untouched. The badge is not written into config files.

- EnhancedLootBags integration is enabled when ELB is present and requires version
  1.3.5. Unexpected controller definitions and corrupt saved global claims fail closed.
  Back up the whole world, including `data/gtnhextranerf/loot`, when moving saves.

- HarvestCraft soybean crop changes activate when HarvestCraft is loaded. The
  replacement item is registered during common pre-init.
- The Hungry Axe behavior activates when Extra Utilities is loaded.
- TConstruct aluminium blast-furnace changes run only when the TConstruct
  class is available.
- Indium comb, Praseodymium centrifuge and GT++ vacuum-furnace changes in the
  core recipe module run only when Forestry reports loaded.
- Dreamcraft crafting replacements are guarded by the Dreamcraft mod check,
  although the mod itself is also declared required.
- Slime processing removal adjusts its expected recipe count when TConstruct
  is absent. Multiblock Centrifuge removal additionally requires GT++. EMT
  Glue crafting replacements run only when Electro-Magic Tools is loaded;
  their Dreamcraft crafting-table variants additionally require
  NewHorizonsCoreMod.

## Saved ghost circuit state

When a gated pre-HV machine first opens its GUI, the server clears any saved
zero-size phantom circuit while creating the physical circuit slot. The player
must then insert a real Integrated Circuit before a circuit-dependent recipe
can run. Physical circuits already stored with a positive stack size remain
usable.

## Updating pack components

Exact recipe signatures and some mixin injection points are intentionally
tied to the current upstream snapshot. A newer GT5-Unofficial or
NewHorizonsCoreMod version may fail fast during startup or may require updated
mixin selectors.

Do not bypass the integrity check to force compatibility. Port the affected
patch and verify a complete GTNH server instead.

The declared supported pack release is **GTNH 2.9.0**, configured by
`GTNHExtraNerf.SUPPORTED_GTNH_VERSION` and displayed on the startup error screen.
The audit validates recipe contracts; it does not infer the pack version from
the individually installed mods.
