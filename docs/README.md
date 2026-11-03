# GTNH Extra Nerf documentation

This directory is written for players, pack maintainers and human
contributors. You do not need to understand mixins or Forge internals to read
the gameplay catalog.

## Start here

- [`OVERVIEW.md`](OVERVIEW.md) — what the mod is and how it works at a high
  level.
- [`NERFS.md`](NERFS.md) — complete list of all current gameplay changes.
- [`COMPATIBILITY.md`](COMPATIBILITY.md) — supported environment and optional
  integration conditions.
- [`BUILDING.md`](BUILDING.md) — how to compile, test and run the project.
- [`TECHNICAL_OVERVIEW.md`](TECHNICAL_OVERVIEW.md) — short architecture
  explanation for curious contributors.
- [`DEVELOPMENT.md`](DEVELOPMENT.md) — contribution and validation workflow.
- [`RECIPE_SYSTEM.md`](RECIPE_SYSTEM.md) — recipe modifier interfaces, registration,
  transactions and extension examples.
- [`TROUBLESHOOTING.md`](TROUBLESHOOTING.md) — startup integrity failures and
  useful logs.

The root [`README.md`](../README.md) is the concise project landing page.
Agent-oriented implementation documentation lives separately in
[`../AI`](../AI/README.md).

- [Audit system](AUDIT_SYSTEM.md): sessions, outcomes, count contracts and reports.
- [Progression and certificates](PROGRESSION.md): tiers, certification, revocation and safety gates.
- [Certification exams](CERTIFICATION_EXAM.md): exam banks, configuration and the exam flow.
- [Components](COMPONENTS.md): fictional component tiers and the registry.
- [AE2 progression](AE2_PROGRESSION.md): Applied Energistics 2 changes tied to progression.
- [Telemetry](TELEMETRY.md), [team tracking](TEAM_TRACKING.md) and [update check](UPDATE_CHECK.md): what the mod sends, how to opt out and how to test it.
- [Mixin review](MIXIN_REVIEW.md), [mixin refactor plan](MIXIN_REFACTOR_PLAN.md) and [audit refactor plan](AUDIT_REFACTOR_PLAN.md): design history.
- [Brainrot materials and multiblock](BRAINROT_MATERIALS.md): material identities, prototype Brainrotter, registration and compatibility.
- [Next progression nerfs: preliminary research](NEXT_NERFS_RESEARCH.md): historical source findings, implementation status and deferred decisions.

## Lootbag architecture and implementation

These documents describe the native ELB integration and its verification boundaries:

- [`LOOTBAG_SYSTEM_DESIGN.md`](LOOTBAG_SYSTEM_DESIGN.md) — EnhancedLootBags research,
  audited edits of native tables, cache handling and hidden global allowances.
- [`LOOTBAG_IMPLEMENTATION_PLAN.md`](LOOTBAG_IMPLEMENTATION_PLAN.md) — implementation
  implemented native hooks, persistence compatibility and verification status.

## Documentation policy

The source code is the final authority. Gameplay documentation must be updated in
the same change as any nerf, recipe, dependency or mixin behavior.
