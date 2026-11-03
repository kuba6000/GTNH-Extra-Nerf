# Agent documentation

This directory is the technical source of truth for coding agents and
maintainers working on GTNH Extra Nerf. Gameplay values must still be checked
against source code when the implementation changes.

## Reading order

1. [`PROJECT_MAP.md`](PROJECT_MAP.md) — repository inventory and ownership.
2. [`ARCHITECTURE.md`](ARCHITECTURE.md) — loading lifecycle and patch model.
3. [`FILE_RELATIONSHIPS.md`](FILE_RELATIONSHIPS.md) — dependencies and call
   relationships between implementation files.
4. [`PATCH_CATALOG.md`](PATCH_CATALOG.md) — exact current gameplay changes and
   their implementation locations.
5. [`BUILD_AND_TESTS.md`](BUILD_AND_TESTS.md) — toolchains, commands, CI and
   verification boundaries.
6. [`MAINTENANCE.md`](MAINTENANCE.md) — safe procedures for changing or porting
   patches.

Human-facing documentation is in [`../docs`](../docs/README.md). The concise
project entry point is [`../README.md`](../README.md).

## Sources of truth

When documents disagree, use this precedence order:

1. Runtime behavior in `src/main/java`.
2. Mixin configuration in `src/main/resources`.
3. Dependency and build declarations in Gradle files.
4. Executable tests and CI workflows.
5. `AI/` technical documentation.
6. `docs/`, root `README.md`, and `CHANGES.md`.

Update the relevant documentation in the same change whenever behavior,
dependencies, lifecycle, patch cardinality, or file ownership changes.

## Non-negotiable project invariants

- The mod targets Minecraft 1.7.10 and the pinned GTNH dependency snapshot.
- Recipe patches must fail closed when their exact upstream target is absent
  or ambiguous.
- Every active `AuditScope` must satisfy its aggregate recipe/value contract.
- Registry-driven mixin families must record at least one semantic hit when
  their criteria require it.
- A failed patch must prevent a usable game startup through `AuditManager`.
- New behavior must not be documented from an old diff or changelog alone;
  inspect the live source and upstream signature.
- `gradle/GTNHGradle` is a pinned Git submodule and a separate codebase. Do not
  edit it casually as if it were ordinary root-project code.
- Generated files, build outputs, and runtime directories are not source of
  truth.

## Implementation

Mixins are registered through the `Mixin` enum. Load-complete recipe modules
use an ordered modifier registry. The build uses a pinned GTNHGradle fork.

No gameplay configuration switches currently exist. `Config` only opens and
saves the Forge configuration file if Forge reports a change.
