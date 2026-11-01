# Progression certificates

Steam and electrical exams award two activation items: `gtnhextranerf:steam_certificate` (High Pressure
Operator Certificate) and `gtnhextranerf:lv_certificate` (Electrical Safety
Certificate). Basic Bronze/Steel Steam machines require Steam certification.
Basic electric GregTech machines from LV upwards require both certifications.
From MV upwards they also require every paid certification up to their tier:
MV through MAX. Each is earned by payment without an exam. Higher-tier
basic machines require all registered gates up to their tier. LuV through MAX
currently use provisional item fees; MAX remains available with its MHDCSM fee.
Energy and dynamo hatches from LV upwards require the same chain of certificates
up to their own voltage tier, including multi-amp, laser and wireless variants.
Multiblock overclocking and parallel rules remain native; certification does not
impose a separate recipe-voltage cap.
ULV electric machines, generators, boilers and multiblock controllers are outside
the current gate. The legacy electrical item/tier IDs stay `lv_certificate`/`lv`
for save compatibility; its player-facing category is Electricity Gate.
Existing saves retain all earned certifications. New tiers default locked when
absent from the save.

An uncertified machine displays a certificate with an exclamation mark in its
normal error-status area. Hovering shows the exact message requested for this
feature:

> You are not OSHA certified to handle electricity, click here to get your license

Left-clicking the icon opens the earliest missing certificate's NEI page (Steam
before electrical, then the paid voltage tiers from MV through MAX). Certificate
recipe and usage queries also open this page. It describes activation and has
a button opening the [electrical exam](CERTIFICATION_EXAM.md). Passing the server-graded
exam awards a certificate. Each ten-question exam requires eight
correct answers. There is no survival crafting recipe
or loot entry. Certificates are hidden from the NEI item panel
and search; players discover the restriction through the machine's error icon.
The information page still opens from that icon or queries on a held certificate.
Placing a locked energy/dynamo hatch sends the placing player a warning with an
**Open certification** chat link. Right-clicking it with an empty hand repeats
the warning. Open chat with T and click the link to view the earliest missing
certificate's NEI page; opening it never charges a fee. Screwdrivers, wrenches,
covers and other native interactions are not intercepted or cancelled.
No per-player notification history is persisted.
The item remains available in creative or through commands for testing.
Ordinary power-failure and venting errors retain their original behavior once
certification is no longer missing.

## Fees

The admission window shows the server's fee and requires explicit confirmation.
Opening NEI or the window itself is free. Only the 36 main inventory/hotbar slots
are charged; the server requires the canonical GT-unified item and metadata.
There is no partial deduction when the total is insufficient.

| Certification | Fee | Result |
| --- | --- | --- |
| Steam | 1000 GregTech Bronze ingots | One exam attempt |
| Electrical (LV) | 1000 single Tin cables (`cableGt01`) | One exam attempt |
| MV | 556 Steel blocks (5004 ingots) | Immediate world certification, no exam |
| HV | 1112 Aluminium blocks (10008 ingots) | Immediate world certification, no exam |
| EV | 1112 Stainless Steel blocks (10008 ingots) | Immediate world certification, no exam |
| IV | 1112 Titanium blocks (10008 ingots) | Immediate world certification, no exam |
| LuV | 670 TungTungTungstensteel blocks (6030 ingots, finished material) | Immediate world certification, no exam |
| ZPM | 1112 Rhodium-Plated Palladium blocks (10008 ingots) | Immediate world certification, no exam |
| UV | 1112 Iridium blocks (10008 ingots) | Immediate world certification, no exam |
| UHV | 1112 Osmium blocks (10008 ingots) | Immediate world certification, no exam |
| UEV | 1112 Neutronium blocks (10008 ingots) | Immediate world certification, no exam |
| UIV | 1112 Infinity blocks (10008 ingots) | Immediate world certification, no exam |
| UMV | 1112 Transcendent Metal blocks (10008 ingots) | Immediate world certification, no exam |
| UXV | 1112 Space-Time blocks (10008 ingots) | Immediate world certification, no exam |
| MAX | 1112 MHDCSM blocks (10008 ingots) | Immediate world certification, no exam; placeholder |

Block fees round the requested ingot counts upwards to the nearest multiple of
nine. Every new Steam/LV attempt costs the full fee, including after failure or
closing a paid paper. An incomplete paper can be corrected in the same attempt.
Repeated payment/submission packets cannot charge or reward that attempt twice.
An already certified world is not charged again, including simultaneous
requests for the same tier by different players. Paid tiers do not issue additional activation items.
Their hidden registered certificate items remain available for creative testing.

## Activation and saved state

Right-clicking while holding a current certificate grants its certification to the
entire server world, including other players, existing/new machines and other
dimensions. First activation consumes one certificate in survival; creative
retains the item. Reusing a certificate after certification does not consume it.

`ProgressionData` is stored through the overworld's vanilla `MapStorage` in
`<save>/data/gtnhextranerf_progression.dat`. The `certifiedTiers` compound uses
stable tier IDs (`steam`, `lv`, `mv`, `hv`, `ev`, `iv`, `luv`, `zpm`, `uv`, `uhv`,
`uev`, `uiv`, `umv`, `uxv`, `max`). Changes call `markDirty()` and participate
in normal world saving. There is no static unlocked-world cache that could leak to another save.

## Revocation after accidents

A successful native GregTech wire/cable fire or native GT machine explosion
revokes this certification globally. The cable hook observes
`BaseTileEntity.setToFire` and snapshots the live `MTECable` before replacement;
it records only an accepted fire replacement, including fires that immediately
extinguish. Fluid-pipe fires do not qualify. The machine hook observes the
valid-machine path in `BaseMetaTileEntity.doExplosion`, immediately before the
meta entity's `onExplosion` callback. A generic explosion elsewhere in the world
is not itself a revocation trigger.

Both observations queue work through `ProgressionServerTasks`; revocation and
player notification execute on the logical server tick. On the next processing
tick, every gated machine pauses intact, including other players/dimensions.
Open machine GUIs synchronize the missing-certificate status. Notifications
are emitted only when an active certification is actually revoked.

Revocation increments the corresponding saved `certificateEpochs` value and marks world
data dirty. Exam rewards carry their issue epoch in item NBT. A pre-revocation
spare cannot restore the licence; the player needs a newly issued certificate.
Legacy untagged items represent epoch zero and retain compatibility until the
first revocation. An expired item is rejected without being consumed. Passing
the exam again issues the current epoch, and using that certificate resumes
processing. Repeated incidents while already uncertified do not advance the
epoch or repeat the global notification.

Steam certification is revoked when a server player actually receives native
GT fluid-pipe contact/leak damage (heat or frost), or a Bronze/Steel Steam
machine's exhaust damage. The hooks wrap the native utility calls and require
an accepted hit plus reduced health or absorption. Immune/protected players,
cancelled/no-op damage and mobs do not revoke certification. Other heat sources
such as hot inventory items do not qualify. Merely venting never revokes a
licence. `SteamSafety` queues the incident; the
server tick revokes Steam, invalidates Steam spares and notifies everyone.
Electrical certification is retained, but its machines pause because Steam is
a prerequisite. Revoking Steam or LV changes only its own flag and issue epoch. Later earned
certifications, including all paid tiers, persist through revocation and save reloads.
Machines still require every earlier prerequisite. Renewing the missing
prerequisites resumes processing without paying for retained later tiers again.

## Runtime and UI

- `ProgressionCertificates` registers and identifies the certificate items.
- `ProgressionTier` retains the internal LV entry and supports Steam and paid tiers through MAX; `ProgressionData`
  selects the earliest missing prerequisite independently of activation order.
- `MachineProgression` selects eligible machines and reads the authoritative
  overworld data. `certify(world, tier)` is the server API for future reward logic.
- `ElectricalSafety` observes accidents and queues authoritative revocation.
  The fire/explosion mixins expose distinct marker interfaces for auditing.
- `SteamSafety` observes actual player damage from fluid pipes and steam exhausts.
- `MixinProgressionTickGate` wraps GT's `BaseMetaTileEntity.doPostTick` dispatch,
  including subclass overrides. Locked machines do not process recipes or spend
  processing energy. Existing in-flight work is paused intact and resumes after
  certification. Waiting inputs are rechecked immediately on the first unlocked
  tick. Basic-machine energy reception and inventory access remain available.
- `EnergyHatchCertification` identifies native input/output hatch families,
  observes placement/empty-hand interaction and queues warnings on the server
  tick. Its client-only command opens the existing NEI page; it cannot pay or unlock.
- `MixinEnergyPorts` closes hatch network ports and buffer transfer APIs while
  locked. `MixinMetaTileEntity` hides and freezes the buffer even for controllers
  reading/writing it directly. Native NBT keeps the real stored energy, restored
  after recertification. Hatch voltages and capacities remain unchanged.
- `MixinWirelessHatchCertification` rejects native receiver refills before any
  global-account withdrawal, including first-tick and manager-driven transfers.
- `MixinProgressionMachineStatus` preserves GT's existing error widget and syncers,
  adds a server-to-client missing-certificate ID, tooltip and clickable certificate drawable.
  The icon click is client-only navigation and cannot grant certification.
- `MixinProgressionMachineGui` handles the separate ModularUI 2 GUI path used by
  machines such as the Basic Wiremill. `CertificateStatusPanel` registers a
  server-to-client-only certificate ID in that panel's sync manager. Only the server
  getter reads world certification; client rendering and clicks use the received
  cache. The normal error widget is preserved under the certificate indicator.
- `CertificateRecipeHandler` supplies informational NEI pages and the exam entry point.
- `CertificationExams` registers and audits the bundled exams (`BundledExams`). The MUI2
  `ExamGui` and `ExamSyncHandler` synchronize public text, accept answer choices
  and grade one panel-scoped attempt during the server tick. `DrawingExamWidget`
  keeps pencil/eraser rasters on the client. See [the exam contract](CERTIFICATION_EXAM.md).

The common proxy has no NEI dependencies. Client navigation lives in the client
proxy; the same certificate registry is used on both sides. The missing certificate is
synchronized per open machine window, so other players' open windows update after
certification without requiring a reconnect.

The hatch gate is independent of controller GUI and recipe implementations.
Controllers may report their ordinary power failure when a hatch stops supplying
EU. Existing controller-owned buffers can continue supplying their stored energy;
machines using wireless power directly without any hatch require separate rules.

## Verification

`VERIFY_PROGRESSION_CERTIFICATES` audits registry identities and the machine UI
hook during load-complete. Required mixin injection counts also reject missing
or changed tick/UI targets at class transformation time.
`VERIFY_ELECTRICAL_SAFETY_HOOKS` separately verifies the common cable-fire and
machine-explosion hooks; both require exactly one native injection target.
`VERIFY_CERTIFICATION_COSTS` checks that every fee material exists, fits the main
inventory, and that Steam uses GregTech Bronze ingots.
`VERIFY_STEAM_SAFETY_HOOKS` verifies separate pipe-damage and exhaust-damage
markers, with strict native heat/frost injection counts.
`VERIFY_ENERGY_HATCH_CERTIFICATION` checks native port/buffer hooks and both
wireless receiver refill hooks. The hatch integration fixture exercises registered
input/output variants, saved-buffer restoration, cached and unsided ports, actual
dynamo/laser transfer, wireless account conservation and the MAX certificate gate.
Chat navigation is verified manually in-game; no notification-history state is added.

Unit tests cover saved-state round trips, world isolation, repeat activation,
eligible tiers and NEI routing. The opt-in full-pack fixture ticks real GT LV/MV/HV/EV/IV
furnace tiles, verifies paused progress and unchanged input/energy, activates a
certificate through native right-click, checks resumed processing and shared
unlocking across machines/dimensions, and checks duplicate item preservation.
It also places a real cable/fluid pipe/machine, checks fire and explosion
revocation, excludes fluid-pipe fires, rejects stale spare certificates, verifies
updated open-GUI status, and checks restored processing through IV after recertification.
The fixture grants certification only in its disposable integration world.

The MUI2 regression test transfers real sync-value payloads between separate
server/client threads, rejecting any attempt to read server state from the client.
The full-pack fixture also builds the actual Wiremill MUI2 GUI and verifies its
registered status before and after certification.
Steam fixtures exercise Bronze/Steel processing, actual player pipe/exhaust
damage, protected-player and mob exclusions, stale items and cross-dimension
revocation. Sync tests also switch Steam/electrical/none and verify click routing.
