# Development guide

## General rule

Every gameplay patch should own one precise piece of upstream behavior. If the
upstream target is absent or ambiguous, production startup must fail with
useful diagnostics instead of applying a guess. A deobfuscated development
launch logs and skips that patch so reduced local runtimes can still boot.

This also applies to known native loot-table mismatches during initial loading:
the native editor's changes are rolled back, the failed audit is retained, and
startup continues with lootbag opening unavailable in integrated dev worlds.
It is not a blanket exception handler: programming and rollback failures remain
fatal. Production and runtime loot reload validation remain strict.

## Before submitting a change

- Keep dependencies and the GTNHGradle submodule pinned.
- Add a behavior test where possible; avoid relying only on checks that search
  source text.
- Run `gradlew check`.
- Run the Python server-harness tests.
- For gameplay, mixin or dependency changes, run a complete GTNH server.
- Confirm there is no `AuditManager` integrity warning/failure and no generated
  crash report.
- Update [`NERFS.md`](NERFS.md) and the relevant summaries.

## Changing recipes

Use the exact original recipe as a compatibility signature. Include every
value needed to distinguish it from parallel recipes. Avoid broad removal by
output unless the upstream API provides no safer option and the result is
validated.

Register a `RecipeModifier` through a `RecipeModule`. Use the provided
`RecipeContext` for every related edit; one modifier is a transaction across
its machine/crafting registries. The registry owns activation, audit and
development failure handling. See [Recipe modification system](RECIPE_SYSTEM.md)
for API examples and compatibility guarantees.

## Changing mixins

Prefer argument/return modification, or WrapOperation when delegating an
operation, over a full method overwrite. Any new overwrite should explain what upstream implementation it
replaces and why a smaller injection was not suitable.

Ordinal-based injection is fragile. Validate the actual recipe builder or
arguments before mutating them, and exercise the final recipes in a full pack.

## Translations

Every text a player sees is a language key in `src/main/resources/assets/gtnhextranerf/lang/en_US.lang`;
add another language by creating `<locale>.lang` (for example `zh_CN.lang`) next to it. A missing key falls back
to English. Long machine descriptions live in `lang/<locale>/tooltip/*.md`. The certification exams are
translated the same way (106 keys, see [the exam contract](CERTIFICATION_EXAM.md)), and so are the chat messages and
command usage of telemetry, team tracking and the update check: the server sends a key with its arguments and
each client translates it. In a value, `%s` is an argument and a backslash followed by `n` is a line break where
the exam shows one. The startup error screen prefers the translation and falls back to English when language
files are not loaded yet. Do not add player-visible text as a Java string: use a key, and add it to `en_US.lang`.

## Updating documentation

The human catalog must describe actual final behavior, not an old diff. State
whether a change is a nerf, removal, new requirement or occasional buff.

Implementation-level mappings are maintained separately in
[`../AI`](../AI/README.md).

### Mixin recipe integration tests

Run registry and machine checks with
`python .github/scripts/run_horizon_tests.py --tests gtnhextranerftests:NativeScenarios`.
The pinned Horizon-QA runner starts its own scratch world, reports each scenario
in JSON and JUnit, and exits automatically. GUI crafting checks use
`--tests gtnhextranerftests:ArcaneWorkbenchScenarios`.

`horizonQaIntegration` includes the fixture sources for these launches. Ordinary
builds package the production mod. Unit tests remain under `src/test/java`.

Keep ProcessingWire's single semantic injector. Avoid ordinals and per-recipe
injection sites. Audit scope still includes recognition time inside callbacks.
