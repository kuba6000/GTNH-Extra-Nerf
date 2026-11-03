# Fictional components and UFO Assembler

UQV means **Ultimately Questionable Voltage**. These are separate component
items that borrow the complete appearance of UEV components.

The eight component identities remain `gtnhextranerf:uqv_motor`, `uqv_piston`,
`uqv_pump`, `uqv_conveyor`, `uqv_robot_arm`, `uqv_sensor`, `uqv_emitter` and
`uqv_field_generator`, all in the `gtnhextranerf` namespace. Registry names are
save data and must not be renamed after release without migration.

All fictional items have no production recipes, loot, Ore Dictionary aliases
or Creative tab. NEI hides all their damage/NBT variants from its item panel.
They remain visible in recipe pages and available to administrators through
commands. Components are inert and do not inherit UEV cover/machine behavior.

## UFO Assembler pages

**UFO Assembler** is an NEI-only recipe handler, not a playable machine. Each
UQV component has its own blueprint:

- the original UEV component is on the left;
- UFO Assembly floats above the center, with a cyan beam moving downward;
- the component's characteristic fictional ingredient sits inside the beam;
- the UQV component is on the right;
- arrows connect the source, ingredient and result, and the full tier name is
  displayed below the diagram.

The category's icon/catalyst is the registered item `gtnhextranerf:ufo_assembly`,
called **UFO Assembly**. Its tooltip is **Unidentified Fabrication Operation**.
R or U on this item opens all eight blueprints. R/U on a UQV component opens
its one blueprint; U on a fictional ingredient opens the matching blueprint.
UEV source components remain visible in blueprints but their usage queries do
not open UFO pages. Generic all/category queries do not expose the catalog. U on a UQV
component can additionally show real recipes consuming it, including Proxy.

The displayed ingredient/result relation exists only in NEI's cached recipe
objects. No crafting, assembler or other machine recipe is registered. The
handler disables overlay/favorite buttons and has no recipe-transfer rectangle.
In particular, its appearance does not make UQV components obtainable.

## Characteristic ingredients

Every blueprint shows one source component, one characteristic part and one
result. Parts have independent item identities and localized joke tooltips.

| Component | Ingredient | Registry id |
| --- | --- | --- |
| Motor | Unidentified Flying Rotor | `unidentified_flying_rotor` |
| Pump | Negative-Volume Chamber | `negative_volume_chamber` |
| Conveyor | Missing Return Belt | `missing_return_belt` |
| Piston | Immovable Telescopic Rod | `immovable_telescopic_rod` |
| Emitter | Unobserved Photon Source | `unobserved_photon_source` |
| Sensor | Uncertainty Detector | `uncertainty_detector` |
| Robot Arm | Unidentified Helping Hand | `unidentified_helping_hand` |
| Field Generator | Undefined Field Boundary | `undefined_field_boundary` |

These ids use the `gtnhextranerf` namespace. UQV component tooltips still contain
only **Guaranteed to raise questions**.

## Responsibilities and lifecycle

| Class | Responsibility |
| --- | --- |
| `ComponentType` | Stable component ids and lazy UEV appearance references. |
| `ComponentTier` | Cosmetic tier identity, abbreviation and appearance policy. |
| `ComponentRegistry` | Component identities, fresh stacks and read-only catalog. |
| `ComponentItem` | Localized component identity and borrowed vanilla appearance. |
| `ComponentItemRenderer` | Borrowed UEV custom rendering and material effects. |
| `FabricationIngredient` | One explicit ingredient definition per component. |
| `FabricationItem` | Hidden inert item using an owned texture and joke tooltip. |
| `UfoAssemblyRegistry` | UFO/ingredient identities, fresh stacks and inverse ingredient lookup. |
| `ComponentLoader` | Shared catalogs and common Forge pre-init registration. |
| `UfoAssemblerRecipeHandler` | Item query routing, independent blueprints and beam/arrow layout. |
| `NEIExtraNerfConfig` | Hidden item variants, handler/catalyst registration and page metadata. |

Constructing catalogs never resolves GT appearance items. Sources are resolved
on demand and copied. Forge registration runs on both sides; renderers and NEI
handler-info listeners are registered only in `ClientProxy.init`. NEI discovers
its plugin only when installed. Common registration has no NEI dependency.

Adding another cosmetic tier extends `ComponentTier` and localization; this
particular UFO handler deliberately handles only UQV. Adding a component type
also requires a matching `FabricationIngredient`, localized item text and sprite.
The fabrication catalog rejects incomplete or duplicate component mappings.

## Textures

UQV uses the exact UEV appearance, including custom material effects. The UFO
and eight fictional ingredients have hand-authored transparent 32x32 PNG sprites.
Their 16px base artwork is doubled with nearest-neighbor scaling to keep crisp
pixel edges. Assets are in `src/main/resources/assets/gtnhextranerf/textures/items`.

Run `python tools/draw_ufo_textures.py` (Pillow required) to reproduce them. The
script also writes `build/ufo-textures-preview.png`, a texture contact sheet,
not an in-game screenshot. The beam and arrows use native GUI drawing; the
page background and slots reuse GT textures.

## Recipe consumer and validation

```java
ItemStack sensor = ComponentLoader.components()
    .stack(ComponentTier.UQV, ComponentType.SENSOR, 1);
```

Use the transactional recipe system's exact matching and audited
`replaceItemInput` for consumers. `IntergalacticRecipeNerf` replaces one UEV
Sensor with one UQV Sensor in NHCore's Space Assembler Proxy recipe, preserving
all other data, including module-tier metadata. It runs after GT's late recipe
queue and audits one modification when IG, AE2 and AE2 Fluid Craft are loaded.

Tests query the handler through NEI's public R/U API, verify each source/part/
result relation, source metadata, usage routing and the UFO's complete catalog.
Unrelated and generic queries are rejected. Visual client checks should inspect
the beam, tooltips and pages using R/U, and confirm the items stay out of search.
