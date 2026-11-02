# Technical overview

GTNH Extra Nerf is packaged as both a normal Forge mod and an FML coremod.

## Startup phases

1. The coremod loader selects three early crafting accessors and two early
   gameplay mixins for ore and nuclear-item construction, plus a client-only
   hook for the Forge startup error screen.
2. GTNH Mixins selects late mixins for generators, machines and recipe
   registration.
3. At Forge `loadComplete`, modules register ordered recipe modifiers. Each
   active modifier stages its crafting/machine changes in one transaction, then
   commits through registry adapters and records its audit counts.
4. A central audit verifies every expected mixin and semantic patch.
5. Production/full-pack launches fail when the audit is incomplete;
   deobfuscated development launches warn, skip missing recipe patches and
   continue.
6. An immutable audit report records added/modified/removed counts, failed,
   skipped and missing scopes, and exclusive time. Programmer and backend-write
   errors remain fatal even in development.

## Why exact recipe matching matters

A recipe is not selected only by its output. The patcher normally checks the
complete item/fluid input and output sets, amounts, duration, EU/t and relevant
chance or heat values. Exactly one recipe must match.

This makes accidental modification of a similarly named recipe less likely
and turns upstream incompatibility into an explicit startup error.

See [Recipe modification system](RECIPE_SYSTEM.md) for the public interfaces,
transaction boundary, direct recipe factories and extension examples.

## Build tooling

The repository includes a pinned fork of GTNHGradle as a Git submodule. It
provides the normal GTNH mod toolchains and tasks that can assemble a complete
client or dedicated server containing the locally built mod.

GitHub Actions runs ordinary compilation, tests and style checks automatically
without starting the incomplete standalone development server. A separate
manual workflow runs the complete server smoke test on demand. Its harness
waits for a successful ready message, shuts the server down cleanly, rejects
audit integrity failures and uploads logs/crash reports.

For implementation-level class maps and contracts, see the separate
[`AI` documentation](../AI/README.md).
