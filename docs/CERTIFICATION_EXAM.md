# Certification exams

Steam and electrical certificates are exam rewards, with no crafting recipe. Players reach
the exam from the certificate's NEI information page, discovered through the
warning on a blocked basic machine or the clickable chat warning of a blocked
energy/dynamo hatch. All certificate items remain hidden in NEI. The same window
supports payment-only certification from MV through MAX.
Using the awarded item retains the existing global world/server unlock.

## Paid admission

Before any questions are sent, the server displays the required material and
quantity. An explicit confirmation requests admission without sending a client
price, material or tier. The factory chooses the tier; the server checks the
live container, world flags and entire main inventory before taking payment.
Steam costs 1000 GT Bronze ingots per attempt. LV costs 1000 single Tin cables
(`cableGt01`) per attempt. MV costs 556 Steel blocks (5004 ingots); HV costs
1112 Aluminium blocks (10008 ingots). EV costs 1112 Stainless Steel blocks and
IV costs 1112 Titanium blocks (10008 ingots each). Paid tiers grant persistent
global certification directly, with no question bank or exam.
The block quantities round upwards to a multiple of nine ingots.

The later payment-only certificates currently use these provisional fees:

| Tier | Canonical block material | Blocks | Ingot equivalent |
| --- | --- | --- | --- |
| LuV | TungTungTungstensteel (`Materials.TungstenSteel`, finished material) | 670 | 6030 |
| ZPM | Rhodium-Plated Palladium | 1112 | 10008 |
| UV | Iridium | 1112 | 10008 |
| UHV | Osmium | 1112 | 10008 |
| UEV | Neutronium | 1112 | 10008 |
| UIV | Infinity | 1112 | 10008 |
| UMV | Transcendent Metal | 1112 | 10008 |
| UXV | Space-Time | 1112 | 10008 |
| MAX | Magnetohydrodynamically-Constrained Star Matter (`MHDCSM`) | 1112 | 10008 |

LuV is set directly to 670 blocks; the later tiers round up from 10000 ingots. Raw
Tungstensteel does not satisfy the LuV payment. Every tier has a stable certificate
item, GUI factory, client status ID and world-save ID, including MAX as a placeholder.
The current machine gate continues to apply only to GT basic machines. Energy
hatches and multiblocks are not gated by this change; later progression rules and
these provisional fees can be revised independently of the saved certification flags.

When reporting is enabled, the mod sends only the highest available progression tier,
not individual certificate records or exam answers. See [telemetry](TELEMETRY.md).

Every reopened Steam/LV attempt requires another payment. Failing or closing a
paid attempt never refunds its fee. Incomplete answers can be fixed within the
same paid attempt. Disabled exams do not charge. Unpaid submissions cannot
issue certificates. Repeated requests cannot charge the same panel twice;
serialized server tasks also prevent simultaneous requests for the same paid tier from charging
a second player after the first one certifies the world. Prior Steam/LV
revocations preserve later paid certifications while pausing dependent machines.

## Paper and drawing

ModularUI 2 owns the window, buttons, scrolling, container and network lifetime.
A custom client widget uses Minecraft's font and a pixel paper background.
An introduction page precedes ten questions. Each question page has four
answer boxes, A-D. Previous/next controls
retain each question's pencil raster. Left mouse draws, right mouse erases.
The raster detects any ink inside an answer box; no shape recognition is needed.
Multiple marked boxes and empty answers cannot complete a single-choice exam.
Long questions/answers first expand the centered window vertically to fit the
current page, up to the client's available height after GUI scaling, with a
small top/bottom margin. Only content beyond that limit needs the scrollbar.
Page changes and window/GUI-scale changes recalculate the height; short pages
retain the normal base height. Footer controls stay anchored to the bottom,
and the paper background keeps fixed-size pixel borders/header/footer while
its central area expands. After grading, the paper displays the complete
supplied passing/failure message.

The canvas explicitly enables font texturing after the viewport stencil. The
pencil foreground uses absolute GUI mouse coordinates, matching MUI2's
untransformed foreground pass. Changing pages updates the viewport's content
length and scrollbar layout, then resets its scroll position to the top.

## Authority and threads

The server owns the answer key, pass threshold and one attempt per open exam
panel. It sends the question and option texts, as language keys, to the client,
which shows them in the player's language. The answer key is never part of that
message.

Only submitting the complete paper sends choices back: one four-bit A-D mask
per question. Strokes, mouse motion, eraser actions and page changes stay local.
ModularUI binds the submission to the player and open panel. Payload sizes and
choices are bounded. The network callback copies the answer data into a pending
task; grading and reward delivery run during the server tick, after confirming
the exam container is still open. Client network callbacks publish immutable
display data for the GUI thread.

Passing issues one current certificate for that exam to the player's inventory, or drops it if
the inventory is full. A completed attempt is idempotent: resending a packet
cannot issue another certificate. A failed exam can be retried by reopening it.
Closing the paper abandons its drawings and unfinished attempt. Passing alone
does not activate world certification; using the certificate does that.
The reward records the server's current certification epoch. Cable fires or GT
machine explosions invalidate previously issued spares; a new passing attempt
after revocation produces a valid replacement. Steam pipe/exhaust injuries similarly
invalidate Steam spares. The electrical factory/file/tier names remain LV for
compatibility. Separate server factories choose the reward tier; JSON cannot
redirect an exam to another certificate.

## The questions and their translation

Both exams ship with the mod: `BundledExams` holds the answer key and the pass
threshold (**8/10**) of each, and `en_US.lang` holds every text. The LV exam is
**B, C, A, D, B, C, A, D, C, B** and the Steam exam **B, D, A, C, B, A, D, C, A, B**,
ten questions each. Nothing is copied to the configuration directory and nothing is
read from it: an `lv-exam.json` or `steam-exam.json` left by an earlier version is
ignored (a log line says so) and can be deleted. The answer key is part of the
public source, so it is not hidden from players.

Every text is a language key. The server sends the keys, and each client looks them up
in its own language, falling back to English for a missing key:

| Key | Text |
| --- | --- |
| `gtnhextranerf.exam.<tier>.introduction` | Shown before the first question. |
| `gtnhextranerf.exam.<tier>.passed` / `.failed` | Result message. |
| `gtnhextranerf.exam.<tier>.qNN.prompt` | Question `NN` (`01` to `10`). |
| `gtnhextranerf.exam.<tier>.qNN.a` to `.d` | The four options, in A-D order. |

`<tier>` is `lv` or `steam`. To translate the exams, add these 106 keys to the language
file, for example `zh_CN.lang`. A backslash followed by `n` in a value is a line break,
because a language file cannot hold a real one. `ExamDefinitionTest` checks that every
key an exam refers to exists in `en_US.lang`.

Adding an exam or changing a question means changing `BundledExams` and the language
file together. Supported question counts are 1-32 and each question has exactly four
options.

## Verification boundaries

Agreed behavior tests cover pencil/eraser answer detection, server grading,
disabled-bank behavior, exact all-or-nothing inventory fees and one reward per completed attempt. Protocol tests
check bounded submissions and that changing the server key never changes the
client paper payload. The full-pack fixture exercises the exam panel on a
dedicated server, rejects unpaid/closed requests, charges each new attempt,
checks payment replay and simultaneous paid-tier purchases, and processes a queued submission before activating the
certificate through the existing item use path.
