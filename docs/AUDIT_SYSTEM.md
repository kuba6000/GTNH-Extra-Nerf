# Audit system

## Responsibilities

- `AuditDefinition` describes an id, activation condition and count expectation.
  `AuditScope` implements it for the existing 64 named checks; it has no global
  statistics or acceptance lookup.
- `AuditExpectation` owns both count validation and its readable description.
  Exact builders default all three counts to zero. Minimum-modified and
  one-added-per-modification contracts preserve the original family rules.
- `AuditSession` owns handles, per-thread nesting, synchronization and aggregate
  observations. Tests and tools can create independent sessions with their own
  definitions and monotonic clock.
- `AuditHandle` represents one execution. It is never reused. Only its owner
  thread may update or close it, and scopes must close in LIFO order.
- `AuditResult` and `AuditReport` are immutable evidence after finalization.
- `AuditManager` is a small static bridge to the startup session for mixins.
  It does not decide whether an exception is tolerable in development.

## Defining expectations

```java
AuditExpectation exact = AuditExpectation.exact()
    .modified(2)
    .added(1)
    .build(); // also requires zero removals

AuditExpectation family = AuditExpectation.atLeastModified(1);
AuditExpectation paired = AuditExpectation.addedPerModification();
```

Dynamic optional-mod expectations are supplied by the definition and resolved
once at finish, after mod loading. An inactive definition's expectation is not
resolved. This preserves optional-mod activation without freezing it before
Forge has loaded the mod list.

## Recording execution

Mixin entry points use callbacks so normal returns and exceptions are visible:

```java
return AuditManager.auditResult(scope, audit -> {
    Result result = performOperation();
    audit.recipeModified();
    return result;
});
```

`AuditManager.run(scope, audit -> { ... })` is the void variant. Neither helper
suppresses exceptions. The original exception is rethrown; audit-protocol or
closure failures are secondary exceptions rather than replacements.
Helpers receive the handle explicitly instead of looking up a global current
scope. A callback marks normal completion as success unless it has set an
explicit outcome. `audit.skip(reason)` requires a reason and does not count as
a successful run of an active scope.

For manually managed execution:

```java
try (AuditHandle audit = session.open(definition)) {
    performOperation();
    audit.recipeModified();
    audit.success();
}
```

Closing without an explicit outcome records failure. Use `failure(cause)` when
manually recording a known cause; automatic callbacks retain it themselves.
Closed handles reject updates and cannot affect later executions. Reporting an
outcome ends counting for that execution, even before the handle is closed.

## Final report and lifecycle

`session.finish()` first rejects any active scopes, leaving the session usable.
It resolves definitions and snapshots results before changing session state.
A successful finish clears mutable accumulators and thread references, preserves
the immutable report, and rejects new work. Repeated finish returns that report.

Each result includes observed added/modified/removed counts, successful/failed/
skipped execution counts, exclusive elapsed time, diagnostics, activation,
expectation and acceptance. Aggregate status is:

| Status | Meaning |
| --- | --- |
| `SUCCESS` | Executions completed and the active count contract passed. |
| `FAILED` | A callback failed, a handle lacked an outcome, or counts failed validation. |
| `SKIPPED` | Execution was explicitly skipped, or the definition is inactive. |
| `NOT_RUN` | An active definition was never executed. |

An active scope requires a successful run, no failed/skipped executions and
matching counts. Inactive, unexecuted definitions are accepted as skipped;
recorded failures still reject the report. Matching counts never erase a failure.

`CommonProxy` finishes the session even after recipe loading fails, retains the
original failure if finalization also fails, and logs all three operation totals
and scope statuses. Failed development audits are not printed as successful.
Production rejects incomplete reports. The report remains available through the
startup session after finalization instead of being discarded by cleanup.

## Recipe transactions and development

`RecipeRegistry` receives its session and `RecipeFailurePolicy` explicitly.
It records transaction counts in one batch only after all registry writes commit.
In `CONTINUE_ON_VALIDATION_FAILURE`, a known `RecipeValidationException` allows
the next modifier to run, while the audit keeps the failed status and reason.
Unexpected exceptions, backend write failures, concurrent registry changes,
invalid lifecycle operations and failed rollbacks still stop startup.

The audit observes work; it does not roll back registration-time mixin effects
or make all startup changes one transaction. Counts on a failed nontransactional
execution describe observations before the failure, not a guarantee of committed
or reverted changes. Earlier successful recipe modifiers remain committed.

## Performance and verification limits

Counters use primitive fields; incrementing a counter does not allocate an
immutable statistics object. Per-thread stacks are reused within the session,
but publicly exposed handles are not pooled. Callbacks capturing arguments may
allocate; they were chosen for outcome handling, not as a speed optimization.
The system is used during startup rather than game ticks.

Exclusive time subtracts nested scopes on the same thread. Summing parallel
scopes can exceed elapsed wall time, and these counters are not a benchmark of
all audit overhead. No claim of faster startup is made without a controlled
comparison.

Public API tests cover contracts, nesting/time, stale handles, thread ownership,
failed finalization, statuses, retained reports and recipe failure policy.
Full-pack startup checks mixin linkage and actual count contracts. Neither
replaces manually exercising every recipe in the client.

## Client startup error screen

Production client audit failures are logged with the complete cause and then
shown through Forge's `CustomModLoadingErrorDisplayException` screen. The
window remains open; the error screen provides no route to the main menu/world.
Dedicated servers retain the fatal FML error behavior. Development validation
warnings remain nonfatal unless a real fatal error occurs or preview is forced.

The English support message uses the single constant
`GTNHExtraNerf.SUPPORTED_GTNH_VERSION`, currently `2.9.1`:

> Extra Nerf is supported only on GT New Horizons (GTNH) 2.9.1.

This is the declared support target, not automatic detection of an installed
pack's release number. The actual failure trigger remains the recipe audit.

A client-only early mixin forwards a deferred loadComplete failure immediately
after `Loader.initializeMods()` inside FMLClientHandler's existing custom-error
catch block. A direct exception from loadComplete alone would be too late for
FML's last state-transition check. No reflection or replacement main-menu GUI
is used; text is rendered on Forge's existing non-dismissable error screen.

### Preview in development

From Gradle:

```powershell
.\gradlew.bat runClient -PforceAuditErrorScreen=true
```

Or add this VM option to an IDE Minecraft run configuration:

```text
-Dgtnhextranerf.forceAuditErrorScreen=true
```

The default is off. The JVM flag is honored only when Forge reports a
deobfuscated development client. The Gradle property forwards it to the forked
`runClient` JVM. Other run configurations can use the VM option directly.
The preview shows a yellow development marker and intentionally blocks entry.
Remove the option and restart to return to normal loading. It requires the
client to reach Extra Nerf initialization; it does not bypass missing mods or
unrelated startup errors. A real fatal error takes priority over a queued preview.
