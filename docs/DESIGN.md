# Palimpsest: design notes

> **Full spoilers.** This covers every creature, secret, boss and ending. If you're going to
> play, stop here.

## 1. Identity

| | |
|---|---|
| Name | **Palimpsest** |
| Mod id | `palimpsest` |
| Tagline | *This world was written over another.* |
| Core metaphor | A scraped and re-used manuscript page. The Overworld is the upper writing. Underneath is the **First Draft**, a world that was rubbed out so this one could be written. The scraping wasn't perfect. |
| Tone | Quiet, literate, uncanny. The dread comes from things being slightly *wrong* rather than from gore. Every horror maps to something real from bookmaking: foxing, rubrication, erasure, redaction, errata, fair copies, marginalia. |
| Palette | Vellum cream, iron-gall ink (blue-black to brown), vermilion red. Red is the only colour the First Draft can't erase, so red is safety. |

### Terminology

| Term | Meaning |
|---|---|
| **The Bleed** | How far the older text has seeped into a particular player's life (0–1000). |
| **The Undertext** | The dimension: what's left of the First Draft. |
| **Inkborn** | Hostile things from the First Draft (entity tag `palimpsest:inkborn`). |
| **Inkbane** | Anything that hurts inkborn properly: vermilion-edged weapons, gall iron, the censer, the Rasure Edge (item tag `palimpsest:inkbane`). |
| **Rubric** | Red ink. Rubric chalk lines, rubric wards and rubric armour keep inkborn off. |
| **Folio** | One of Idris Wray's twelve torn journal pages, the main story thread. |
| **Faded page** | A fragment of First Draft writing (ten of them). Lullabies, lists, rules. |

### Story

Idris Wray was a cartographer who mapped a northern reach of the Overworld and noticed the
map didn't match the land. Stones had writing under their surface. There was one more cow in
his field than there should have been. He learned to make iron-gall ink and vermilion,
learned the knocking was a rule rather than an animal, and eventually built a gate from red
brick and read himself through it into the First Draft. His folios are scattered in the order
he lost them.

In the Undertext he found the **Rubricator**, the last scribe of the First Draft. Red ink
kept him from being erased. He also found the **Bindery**, where he tried to sew the tear
shut from the inside and was bound into the thing that guards it: **the Bookbinder**. At the
bottom of everything is **the Rasure**, the knife that did the scraping, still working. Behind
it is the **Last Folio**, the only page of the First Draft that was never scraped.

A survey team (the A.T.S., "Station Four") followed Wray later. Their logs trace the same
escalation from the outside, and they bricked up their gate before it was finished.

## 2. The Bleed

Per-player capability (`bleed/BleedData`), saved with the player, copied on death, synced to
that player only.

| Stage | Name | Threshold | What changes |
|---|---|---|---|
| 0 | Clean Page | 0 | Nothing. |
| 1 | Faint Trace | 50 | Footsteps behind you, whispers, a crow that watches, a torch that goes out. |
| 2 | Ghosting | 150 | Signs change text, knocking, doors opening, false sounds, chests re-sorted, glimpses of the Smudge and the Pale Stag. |
| 3 | Bleed-through | 300 | The Knocker visits, Copyists join the herd, the Longhand is seen at distance, fog banks, "the other page", faces in the vignette. |
| 4 | Running Ink | 500 | Colour drains, ink rain, total silence, lights-out, chat messages nobody typed, Errata. |
| 5 | The Tear | 700 | Tears open near you and leak inkborn, the sky shows the First Draft, the Palehand, your Fair Copy. |
| 6 | Overwritten | 900 | Blocks scrape away around you and come back. Can be disabled (`enableFinalStage`). |

**Gain**: a slow passive drift every 30 s (0.4 below Ghosting, 0.15 below Bleed-through,
nothing after; ×1.5 at night under the open sky or below y=0 without sky). That's roughly
Faint Trace an hour after the grace period and Ghosting a few hours later for a player who
ignores everything. Past Bleed-through it only rises for a reason: reading a folio (+12
once each) or faded page (+6), visiting a structure (+8 Overworld / +5 Undertext, once each),
seeing a creature for the first time (+3), events themselves, working rites, travelling
through a gate (+20), entering the Undertext for the first time (+80), the Undertext itself
(+1 per 30 s), standing near a Tear, killing a quillcrow (+15) or a Rubricator (+40), and
breaking palimpsest stone (+2).

**Loss**: candlewort tea (−25), sealing wax (−15), the Seal of Closing (−10), a full night's
sleep near a rubric ward (−25, with `bleedDecaysWhileResting`), the Rite of Quieting (−120, and the Longhand watches while you read it). The
Rite of Unmaking cleans the area around the altar instead.
Sleeping unwarded gives a dream instead, and dreams aren't restful.

**Modifiers**: the Warded effect ×0.5; each rubric armour piece −10%; the Circlet ×1.25;
UNWRITTEN ×0.5; SEALED ×0.

**Director** (`horror/HorrorDirector`): runs every second per player and picks from the
events whose stage, dimension, time and conditions fit, weighted, each with its own cooldown.
The base interval between events shrinks with stage (14000 ticks at stage 1 down to 2400 at
stage 6, and 2800 in the Undertext) with ±40% jitter. Major events get an extra ×1.5 spacing.
There's a grace period at the start of a world.

### The 34 events

- **Faint Trace**: `footsteps`, `whisper`, `watcher_crow`, `snuffed_torch`
- **Ghosting**: `the_sign`, `knocking`, `door_opens`, `false_sound`, `chest_shuffle`,
  `heartbeat`, `smudge_glimpse`, `stag_glimpse`
- **Bleed-through**: `knocker_visit`, `copyist`, `longhand_sighting`, `fog_bank`,
  `other_page` (you're shown a page of the First Draft layered over your view), `vignette_face`
- **Running Ink**: `fading`, `ink_rain`, `silence`, `lights_out`, `someone_typed`, `erratum`
- **The Tear**: `tear_opens`, `sky_glimpse`, `palehand`, `fair_copy`
- **Overwritten**: `scraping`
- **Undertext only**: `u_page_turn`, `u_whisper`, `u_rasure_line` (a red line telegraphs a
  scrape), `u_longhand`, `u_footsteps`

Design rules: world changes are either restored (`ServerScheduler`) or harmless. Block
entities are never erased. Fake block updates (`Glimpses`) are sent to one player only and
reverted. No event fires in the first seconds after login, and the same event never repeats
inside its cooldown.

## 3. Creatures

| | Where | Behaviour |
|---|---|---|
| **Foxing Moth** | Both | Ambient. Drifts toward light and drops foxed dust. |
| **Blotling** | Both | Slime-like drop of ink that splits when struck. |
| **Smudge** | Undertext | A rubbed-out person walking the same few steps. Stare at it and it follows politely. Harmless. |
| **Margin Crawler** | Undertext / Bindery | Small drolleries that go for your ankles. |
| **Quillcrow** | Both | Watches from high perches and leaves when you approach. Killing one raises your Bleed. |
| **Pale Stag** | Both | Faceless white deer. Grazes in herds in the Undertext; in the Overworld it's a sighting at the treeline. |
| **Inkhound** | Undertext | Blind. Hunts by sound (sprinting, jumping, breaking blocks). Crouch and keep still. |
| **Rubricator** | Undertext / summoned | The only friendly NPC. Trades, and speaks twelve lines. |
| **Knocker** *(major)* | Overworld doors | A stooped, too-tall figure in a torn burial shroud, an iron knocker-ring through its lip. Knocks three, presses its head to the door and listens, knocks three. Lunges, jaw dropped, if the door is opened or you step outside, bending double through the doorway and crawling through any gap a block high. Leaves at dawn or if warded. |
| **Copyist** *(major)* | Overworld herds | Disguised as a cow, pig, sheep or chicken. Tells: no shadow, a little too tall, never grazes, always faces you, a head that tilts slowly then snaps, low voice. The Reading Lens, food, damage or getting close reveals it: the animal flickers and a pale, too-long thing unfolds out of it, still wearing the animal's skin as a cloak and its head as a mask. Revealed, it stoops under two-high gaps and crawls through one-block holes. |
| **Longhand** *(major)* | Both | A tall single pen-stroke of wet ink with a steel nib for a head. Moves only when unobserved (darkness counts as unobserved), and is found in a different pose each time you look back. Only inkbane hurts it. Light, wards and chalk hold it back. Comes as a distant Watcher or a Hunter. Folds in half under a two-high gap and lies flat to get through a one-block hole; caught mid-squeeze, it holds that shape. |
| **Redacted** *(major)* | Undertext / Rasure fight | Censor-barred figure. Its hits apply Erasure, and it can black out an item name (cosmetic, configurable). Blinks when struck. |
| **Fair Copy** *(rare)* | Stage 5 | A clean copy of you, with your skin, name and held item. Keeps its distance, walks away around corners, and reflects damage. |
| **Erratum** *(rare)* | Stage 4 | A block in the wrong place, a pixel off the grid. Mimics the ground and creeps closer while unobserved, on six jointed legs over a toothed mouth. Stand near it and look away and an eye opens in its side; it half-closes when you stare back. |
| **Palehand** *(rare)* | Stage 5 | A hill-sized hand rising over the horizon, writing in the air, then sinking. Unreachable. It's scenery. |
| **The Bookbinder** *(boss)* | Bindery | See §5. |
| **The Rasure** *(final boss)* | Last Folio | See §5. |

## 4. The Undertext

Data-driven dimension (`data/palimpsest/dimension/undertext.json`) with its own noise settings
(custom density functions and splines), a multi-noise biome source, fixed time (13200), no
skylight, and custom `DimensionSpecialEffects`. The sky has faint writing and no sun. The
lightmap is desaturated toward sepia. The fog is dense and cream-coloured.

Biomes:

1. **Scraped Expanse**: pale, flat, eroded vellum plains with page pillars (stacks of ruled
   vellum, some toppled into lines).
2. **Blotwood**: forests of black blotwood trees with blotberries.
3. **The Gutter**: a deep channel, the fold of the book.
4. **Inkwell Sea**: still black liquid plains.
5. **Rubric Wastes**: red-veined badlands where the Rubricator's people held out.
6. **Marginalia**: dense, vertical, full of scrawl and crawlers.

Entry: build a **Folio Gate**, a 2×3 opening framed in rubricated vellum bricks (corners
optional). Stand at it holding a **Folio of Descent** and "read" for three seconds. The frame
fills with the Undertext Veil. The arrival point links 1:1 in x/z, and a gate is built on the
far side if none exists. Folios of Descent come from the Rite of Descent or, rarely, from the
Broken Gate.

## 5. Bosses

### The Bookbinder (240 HP, scaled for groups)

Found in the **Bindery** (Undertext).

- **Phase 1**: throws needles at range, lunges up close, shakes margin crawlers out of its
  pages.
- **Rebinding (at 50%)**: becomes untouchable and draws thread from three **Stitched Tomes**
  around the room. Break all three to cut the thread. It falls stunned for six seconds and
  takes 50% more damage.
- **Drops**: the Spine Key, two Bookbinder Needles, Folio XII, illumine leaf, binding thread.

### The Rasure (600 HP, scaled for groups)

Summoned inside the **Last Folio** arena by the *Rite of the Last Reading*. The catalyst is
the Spine Key, which also opens the Sealed Door there.

1. **The First Stroke** (100–66%): blade sweeps. Red lines are drawn on the floor, and a
   second later the floor along them is scraped into hollow Blank.
2. **Redaction** (at 66%): it rises out of reach behind a guard and blanks the four **Rubric
   Pillars**. Re-ink them with vermilion or rubric chalk while Redacted attack you. When all
   four are lit, the guard breaks and it falls.
3. **Exposed** (66–33%): faster, more lines.
4. **The Blank Page** (33–0%): the arena whites out. It's invisible unless close, lit by the
   Illumined Censer, or splashed with an Ink Bomb.

It drops Rasure Shards, the **Last Folio** and illumine leaf, and leaves a **Folio Stand**
where it died. Boss music: an original, streamed looping track.

## 6. Endings

Use the Last Folio on the Folio Stand:

- **Seal**: write your own name over the tear. Bleed goes to 0 and stays there, the
  Director stops, and you get the Scrivener's Quill.
- **Read**: the First Draft stays open around you. Bleed is locked high, and you get the
  Circlet of the First Draft (armour; strong, but it raises Bleed gain).
- **Scrape** (only offered if you carry the Rasure Edge): scrape your own name away.
  Inkborn stop targeting you, your name shows as ▒▒▒▒▒▒ in chat and the tab list, and Bleed
  gain is halved.

Each ending is per player and has its own advancement.

## 7. Structures (14)

**Overworld**: Wray's Cabin (3 variants; folios I–III, cellar with IV–V), Scraped Obelisk
(3 variants), Hollow Chapel (the Blank Hymnal, VI), Copying House (III, V, VI), Survey
Station Four (underground; the A.T.S. logs, VII–VIII), Crow Roost, the Doubled House (two
copies of the same house, one slightly wrong; the Misprint), and the Broken Gate
(underground, bricked up; VIII–IX, sometimes a Folio of Descent).

**Undertext**: Faded Village (2 variants; IX), Marginalia Spire (X, the music disc), Ink Well
(illumine), Scrap Shrine (3 variants), the Bindery (XI, the Bookbinder), and the Last Folio
(the arena, Sealed Door, Rubric Pillars).

Templates are generated in Python (`tools/gen_structures.py`) and placed as single-piece
jigsaws with processor lists (`decay_light`, `decay_heavy`, `undertext_fade`) so no two
copies look the same. The **Margin Compass** points to the nearest structure for its
dimension, or to the next story structure when attuned.

## 8. Crafting systems

1. **Vanilla recipes**: more than 90 (shaped, shapeless, smelting, blasting, stonecutting,
   smoking, campfire).
2. **Transcription** (Scriptorium Desk, custom `palimpsest:transcription` recipe type): a
   surface, an ink and a subject become a new item over time. Examples: rubric armour
   (keeps enchantments), the Margin Compass, the Dowsing Quill, the music disc, books from
   vellum.
3. **Rites** (Rubric Altar + Reading Stands within 3 blocks, custom `palimpsest:ritual`
   type): up to eight offerings on the stands and a catalyst on the altar. Conditions can
   include moon phase, day/night, weather, dimension and minimum stage. A rite costs Bleed and
   can backfire (the Knocker, blotlings, the Longhand, a Fair Copy, lights-out, a Tear). More
   players nearby lower the backfire chance. Rites include Descent, Recall, Closing,
   Unmaking, Quieting, Illumination, Binding, the Last Reading, Invitation, the Fair Copy
   (duplicates an enchanted book on a full moon, at a price) and Vermilion Dawn.

## 9. Secrets

- The Doubled House is two copies of the same house built through each other: doors that
  open onto walls, stairs into the ceiling, a window onto stone. Its chest can hold the
  Misprint.
- Scraping a sign with a Rasorium reveals a line of undertext beneath it.
- Faded Page VII ("Rules") is a survival guide written from the other side. The Rubricator
  quotes it.
- The music disc *Lower Writing* is a music-box lullaby with passages scraped out of it,
  found only in the Marginalia Spire.
- Several advancements are hidden until earned.

## 10. Architecture

- `bleed/`: capability, stages, discovery (structures and creatures, every 5 s), events
  (login, respawn, dimension change, sleep, kills).
- `horror/`: director, events (builder DSL), spot finding, reversible world alterations,
  client-only glimpses.
- `entity/`: 17 mobs and 2 projectiles. Bosses implement `PalimpsestBoss`.
  `apparition/` gives short-lived entities per-player visibility.
- `network/`: `SimpleChannel` with Bleed and codex sync, vision triggers (fog, sky, silence,
  ink rain, flash, shake, whiteout, page turn, vignette face), screen opening, ending choice.
- `client/`: renderers (generated `HierarchicalModel`s), dimension effects, sky renderer,
  fog and lightmap hooks, ink overlay, captions, boss music, screens (codex, lore, ending,
  desk).
- `world/`: dimension keys, gates (`FolioGate`, `GateLinks` SavedData, `GateTravel`),
  features, loot injection, biome modifier codec with config toggles, and `DryLandStructure`
  (a `palimpsest:dry_land` structure type that wraps a jigsaw and refuses to start on water).
- `test/`: GameTests. `client/dev/SmokeTest`: scripted screenshot run for CI.

Everything that isn't Java is generated by `tools/` from Python sources, and
`tools/check.py` cross-checks the two sides.
