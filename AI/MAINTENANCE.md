# Maintenance guide

## Before editing gameplay behavior

1. Identify the exact upstream owner and pinned dependency version.
2. Decide whether the value must change during registration or can be patched
   after registration.
3. Inspect all parallel recipe maps and optional-mod variants.
4. Define a unique semantic signature and expected cardinality.
5. Add or select one logical `AuditScope` and define its exact aggregate
   statistics.
6. Add or update behavior tests before weakening any existing contract.
7. Update `AI/PATCH_CATALOG.md`, `docs/NERFS.md` and summary documentation.

Do not port behavior from `CHANGES.md`, an old diff, or comments without
checking live source. Those artifacts can lag behind implementation.

## Adding a load-complete machine recipe patch

Use `RecipeItems.requireModItem(mod, name, amount, metadata)` for a required
external item. It preserves the recipe system's typed validation failure when
the item is missing; do not duplicate this lookup in individual modules.

Implement `RecipeModule.register(RecipeRegistry)` and register one
`RecipeModifier` per `AuditScope`. Use `RecipeContext.machine(map)` with
`modifyOne`, `removeExactly`, `retainExactly`, `requireExactlyOne` or `addOne`.
Construct exact matches with `RecipeMatch.builder()`, including every
relevant upstream value. Additions receive an unregistered builder and an
expected signature; never call `addTo` directly inside a modifier.

Prefer named builder fields (`itemInputs`, `fluidOutputs`, `duration`, `eut`,
`outputChances`, `specialValue`) to manual predicates. Empty varargs require an
empty slot array; omitted fields are unrestricted. Use `oreDictInput` for the
additional OreDict slot, `fluidInputsAnyOf` for complete fluid alternatives and
`RecipeMatch.anyOf` for complete recipe alternatives.

The context stages every related map in one transaction. Pass a `RecipeChange.builder()` definition to `modifyOne`, using
identity-based item/fluid operations rather than field assignments or slot indexes. Do not mutate
the live map or call `AuditManager` from module code. Only known recipe contract
mismatches should throw `RecipeValidationException`; programmer errors and
backend write failures must not be classified as tolerable validation. The registry owns active
scope selection, counts and the development skip policy.

## Adding a crafting recipe patch

Use `CraftingRecipeBuilder` to declare shaped/universal patterns and typed
bindings, or shapeless inputs, plus output. Pass built `CraftingDefinition`
objects to the crafting API; derive replacements with `toBuilder()`.
Use `CraftingIngredient.item` / `.ore` for reusable typed ingredients. Custom
implementations must be immutable and return fresh stacks. Keep native `Object`
conversion in the factory visitor, outside builder and definition storage.
Use `matchType` only when the old rule restricts the upstream class.
Named `removable` and `mirrored` flags configure GT construction. Use
`outputAmount` when preserving the original recipe's validators and alternatives
is required. The adapter derives probes and construction arguments internally.
Related operations in one modifier are validated and committed together.

Do not introduce reflection, recipe class-name strings or direct registry
mutations. The existing three accessor interfaces preserve the Minecraft/RFB
boundary. See [`../docs/RECIPE_SYSTEM.md`](../docs/RECIPE_SYSTEM.md) for examples
and the limits of transaction guarantees.

## Adding or changing a mixin

1. Add the class under `mixins/early` or `mixins/late`.
2. Register its simple name in `Mixin` with correct `TargetedMod` entries.
3. Add a semantic `AuditScope` when application alone is insufficient.
4. Define its activation and aggregate acceptance criteria in `AuditScope`.
5. Record only after the intended semantic target is confirmed.
6. Add explicit `require` cardinality where the injector supports it.
7. Prefer narrow injection over `@Overwrite`.
8. For `@Overwrite`, document the upstream method, version and intentionally
   replaced behavior in Javadoc.
9. Run a full-pack server, not only unit tests.

Avoid new global ordinals. If an ordinal is unavoidable, validate the builder
or arguments before mutation so ordinal drift fails rather than modifying a
different recipe.

## Updating GT5 or NewHorizonsCoreMod

Treat a dependency update as a port, not a routine version bump.

1. Update one upstream dependency at a time.
2. Compile and inspect all mixin annotation-processor diagnostics.
3. Compare each target method used by redirects, modifies and overwrites.
4. Compare every exact recipe signature in the recipe modules.
5. Update expected cardinalities only after explaining why the upstream count
   changed.
6. Run `test`, `check`, and a full-pack server.
7. Inspect NEI/final recipe maps for ordinal-based patches.
8. Regenerate the human nerf catalog from actual final behavior.

Never weaken `requireExactlyOne`, an `AuditScope` expectation or production
verification merely to get a new upstream version to start. Development
tolerance is selected centrally from FML's deobfuscated-environment flag.

## Updating the GTNHGradle submodule

The submodule is an independent repository. Before changing its pointer:

1. read its changelog/commits and local fork design documents;
2. run its `test` and `functionalTest` suites;
3. run the root `check` through the included build;
4. prepare both full-pack client and server if task behavior changed;
5. verify cache, overlay and safe-extraction behavior;
6. update `AI/BUILD_AND_TESTS.md` if toolchains or task names changed.

Do not copy generated submodule build outputs into the root repository.

## Diagnosing startup failures

### `PACKAGE_TRANSFORMER_EXCLUSION` for Extra Nerf mixins

Transformer exclusions match class-name prefixes. Keeping the coremod exclusion
scoped to its entry class avoids these messages for the mod's mixin classes.
In the observed GTNH runtime, the error was logged even for mixins that were
subsequently applied. Check the later mixin logs and behavior before concluding
that an exclusion prevented a particular mixin from applying.

### `expected exactly 1, observed 0`

The mixin did not apply, its semantic branch no longer matched, or the
upstream recipe signature changed. Inspect upstream before adjusting the
expectation.

### `expected exactly 1, observed 2`

The selector became too broad, upstream duplicated a target, or the same
registration ran twice. Do not convert it to `expectAtLeast` without proving
duplicates are intended.

### `recipe registration returned false` / `builder added no recipes`

The target was found, but the replacement was rejected or collided with an
existing recipe. Inspect recipe-map collision rules and duplicate outputs.

### Recipe patch exception

The message names the active `AuditScope` and recipe map/operation. Production
execution is fail-fast. Development execution logs and skips a failed scope,
then continues after leaving/restoring that modifier's registries unchanged. A
failed rollback is always fatal. A development launch with skipped scopes is
not compatibility evidence. Fix the exact signature or missing dependency.

## Documentation update checklist

For every gameplay change:

- update exact values and conditions in `AI/PATCH_CATALOG.md`;
- update the player-facing entry in `docs/NERFS.md`;
- update overview documents if a feature category is added/removed;
- update `CHANGES.md` summary;
- remove documentation for behavior deleted from source;
- distinguish nerfs, buffs, removals and requirement changes accurately.

For every file move or architecture change:

- update `AI/PROJECT_MAP.md`;
- update `AI/FILE_RELATIONSHIPS.md`;
- validate every relative Markdown link.

## Pre-merge gate

Minimum:

```text
gradlew check
python .github/scripts/test_run_full_pack_server.py
```

For gameplay, mixin, dependency, full-pack or build-tool changes, also require
a successful complete GTNH server startup with no audit integrity failure and no
crash report.

## Audit changes

Use the public session/handle/expectation/report APIs described in
[`../docs/AUDIT_SYSTEM.md`](../docs/AUDIT_SYSTEM.md). Keep definition ids and
activation stable when refactoring. Replace duplicated expectation text/lambdas
with typed expectations. Prefer outcome-aware `run` / `auditResult` callbacks
for mixins; manual handles require an explicit outcome. Never restore pooled
handles or make a development failure appear as a successful report.
