"""en_us.json: every name, description, message and page of lore."""
import json

import data_advancements
import lore
from jsonout import *

ROMAN = ["I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"]

BLOCKS = {
    "palimpsest_stone": "Palimpsest Stone", "deepslate_palimpsest_stone": "Deepslate Palimpsest Stone", "scraped_stone": "Scraped Stone",
    "cinnabar_ore": "Cinnabar Ore", "deepslate_cinnabar_ore": "Deepslate Cinnabar Ore", "candlewort": "Candlewort",
    "faded_grass_block": "Faded Grass Block", "spent_torch": "Spent Torch", "spent_wall_torch": "Spent Torch", "tear": "Tear",
    "rubric_chalk": "Rubric Chalk Mark", "rubric_ward": "Rubric Ward", "scriptorium_desk": "Scriptorium Desk", "rubric_altar": "Rubric Altar",
    "reading_stand": "Reading Stand", "vellum_bricks": "Vellum Bricks", "vellum_brick_stairs": "Vellum Brick Stairs",
    "vellum_brick_slab": "Vellum Brick Slab", "vellum_brick_wall": "Vellum Brick Wall", "rubricated_vellum_bricks": "Rubricated Vellum Bricks",
    "scraped_vellum_bricks": "Scraped Vellum Bricks", "undertext_veil": "Undertext Veil", "gall_iron_block": "Block of Gall Iron",
    "illumine_block": "Block of Illumine", "ruled_vellum": "Ruled Vellum", "vellum_soil": "Vellum Soil", "inkstone": "Inkstone",
    "cobbled_inkstone": "Cobbled Inkstone", "polished_inkstone": "Polished Inkstone", "inkstone_bricks": "Inkstone Bricks",
    "inkstone_brick_stairs": "Inkstone Brick Stairs", "inkstone_brick_slab": "Inkstone Brick Slab", "inkstone_brick_wall": "Inkstone Brick Wall",
    "illumine_ore": "Illumine Ore", "blotwood_log": "Blotwood Log", "stripped_blotwood_log": "Stripped Blotwood Log",
    "blotwood_planks": "Blotwood Planks", "blotwood_stairs": "Blotwood Stairs", "blotwood_slab": "Blotwood Slab", "blotwood_fence": "Blotwood Fence",
    "blotwood_door": "Blotwood Door", "blotwood_trapdoor": "Blotwood Trapdoor", "blotwood_leaves": "Blotwood Leaves",
    "blotwood_sapling": "Blotwood Sapling", "blotberry_bush": "Blotberry Bush", "erased_grass": "Erased Grass", "scrawl": "Scrawl",
    "binding_thread": "Binding Thread", "illumined_lantern": "Illumined Lantern", "sealed_door": "Sealed Door", "folio_stand": "Folio Stand",
    "rubric_pillar": "Rubric Pillar", "stitched_tome": "Stitched Tome", "blank": "Blank",
}

ITEMS = {
    "vellum_scrap": ("Vellum Scrap", "A curl of scraped page. Four make a sheet."),
    "blank_vellum": ("Blank Vellum", "Scraped clean, ready to be written on. Something was on it before."),
    "oak_gall": ("Oak Gall", "A wasp's nest in an oak leaf. The black of iron-gall ink."),
    "iron_gall_ink": ("Iron-Gall Ink", "The only ink the lower writing will take."),
    "cinnabar": ("Cinnabar", "Heavy red crystal from deep, hot stone."),
    "vermilion": ("Vermilion", "The rubricators' red. It sits on the page and does not scrape."),
    "undertext_fragment": ("Undertext Fragment", "A flake of the lower writing, still wet. Warm to the touch."),
    "lampblack": ("Lampblack", "Soot of burnt blotwood."),
    "raw_illumine": ("Raw Illumine", "Gilt from the lower page, dull until it is heated."),
    "illumine_leaf": ("Illumine Leaf", "Gold leaf for the capitals. It gives off a little light, and ink hates it."),
    "gall_steeped_iron": ("Gall-Steeped Iron", "Iron soaked black. Blast it."),
    "gall_iron_ingot": ("Gall Iron Ingot", "Iron that has drunk ink. It remembers how to cut it."),
    "foxed_dust": ("Foxed Dust", "The brown freckles of old paper, shaken off a moth."),
    "black_quill": ("Black Quill", "A quillcrow's feather. Cut to a nib, it writes on the lower page."),
    "pale_antler": ("Pale Antler", "Light as paper. Hollow."),
    "blot_residue": ("Blot Residue", "What is left of a blotling. Stains everything."),
    "redaction_strip": ("Redaction Strip", "A bar of pure black, cut from a Redacted. Nothing shows through it."),
    "bookbinder_needle": ("Bookbinder's Needle", "Long enough to sew a building shut."),
    "rasure_shard": ("Rasure Shard", "A sliver of the knife. It is still sharp on every side."),
    "spine_key": ("Spine Key", "Unpicks the stitched door. It is warm, and it has vertebrae."),
    "torn_folio": ("Torn Folio", "In a cartographer's neat, worried hand."),
    "faded_page": ("Faded Page", "A page from the first draft. Most of the words are gone."),
    "last_folio": ("The Last Folio", "The last page of the first draft. Your name is on it."),
    "commonplace_book": ("Commonplace Book", "Your notes. They seem to write themselves."),
    "misprint": ("Misprint", "This should not exist."),
    "iron_rasorium": ("Iron Rasorium", "A scribe's scraping knife."),
    "rasure_edge": ("Rasure Edge", "Forged from the knife that scraped the first draft."),
    "reading_lens": ("Reading Lens", "There is lower writing ground into the glass."),
    "margin_compass": ("Margin Compass", "It points where the page is thin."),
    "dowsing_quill": ("Dowsing Quill", "It twitches toward the lower writing."),
    "bookmark": ("Bookmark", "Keeps your place."),
    "folio_of_descent": ("Folio of Descent", "Read aloud at a red frame, after dark."),
    "seal_of_closing": ("Seal of Closing", "Red wax for a torn page."),
    "ink_bomb": ("Ink Bomb", "Blinds what it splashes. Shows what hides."),
    "illumined_censer": ("Illumined Censer", "Its smoke is light made thick."),
    "scriveners_quill": ("Scrivener's Quill", "The pen that wrote this world. It still works, for small things."),
    "rubric_chalk": ("Rubric Chalk", "Draws lines that ink cannot cross."),
    "gall_iron_sword": ("Gall Iron Sword", None), "gall_iron_pickaxe": ("Gall Iron Pickaxe", None), "gall_iron_axe": ("Gall Iron Axe", None),
    "rubric_hood": ("Rubric Hood", None), "rubric_robe": ("Rubric Robe", None), "rubric_leggings": ("Rubric Leggings", None),
    "rubric_boots": ("Rubric Boots", None),
    "circlet_of_the_first_draft": ("Circlet of the First Draft", "You see in the dark. The dark sees you too."),
    "blotberries": ("Blotberries", "Tastes the way ink smells."),
    "blotberry_tart": ("Blotberry Tart", "Stains the teeth for a day."),
    "candlewort_tea": ("Candlewort Tea", "Settles the page, a little."),
    "sealing_wax": ("Sealing Wax", "Chewed, it stops Erasure. Tastes of candles."),
    "pale_venison": ("Pale Venison", "It does not bleed."),
    "cooked_pale_venison": ("Cooked Pale Venison", "Better cooked."),
    "scribes_ration": ("Scribe's Ration", "Bread and meat wrapped in vellum, for long copying."),
    "music_disc_lower_writing": ("Music Disc", "Unknown - Lower Writing"),
    "spent_torch": ("Spent Torch", None),
}
USES = {
    "last_folio": "Lay it on the Folio Stand.",
    "commonplace_book": "Use to read. [J] also opens it.",
    "iron_rasorium": "Use on palimpsest stone, vellum or signs to scrape them.",
    "rasure_edge": "Use to erase a creature from the page. Long cooldown.",
    "reading_lens": "Use to reveal inkborn and read what the stone says.",
    "margin_compass": "Use to take a bearing.",
    "dowsing_quill": "Hold to feel for palimpsest stone, gates and tears.",
    "bookmark": "Above: mark your place. Below: return to it.",
    "folio_of_descent": "Use on a complete Folio Gate frame at night.",
    "seal_of_closing": "Use on a Tear or a Blank.",
    "illumined_censer": "Use to swing: scalds inkborn, reveals the hidden.",
    "scriveners_quill": "Use at night to write the morning; in rain, to write it clear.",
    "rubric_chalk": "Use on the ground to draw a line.",
}
ENTITIES = {
    "foxing_moth": "Foxing Moth", "blotling": "Blotling", "smudge": "Smudge", "margin_crawler": "Margin Crawler", "quillcrow": "Quillcrow",
    "pale_stag": "Pale Stag", "inkhound": "Inkhound", "rubricator": "The Rubricator", "knocker": "Knocker", "copyist": "Copyist",
    "longhand": "Longhand", "redacted": "Redacted", "fair_copy": "Fair Copy", "erratum": "Erratum", "palehand": "The Hand",
    "bookbinder": "The Bookbinder", "rasure": "The Rasure", "ink_bomb": "Ink Bomb", "binding_needle": "Binding Needle",
}
BIOMES = {"scraped_expanse": "Scraped Expanse", "blotwood": "Blotwood", "the_gutter": "The Gutter", "inkwell_sea": "Inkwell Sea",
          "rubric_wastes": "Rubric Wastes", "marginalia": "Marginalia"}

SUBTITLES = {
    "ambient.undertext.loop": "The lower page breathes", "ambient.undertext.additions": "Something far off", "ambient.undertext.mood": "The page shifts",
    "music.undertext": "Music plays", "music.rasure": "The Rasure's theme", "music_disc.lower_writing": "Music disc plays",
    "event.knock": "Knocking", "event.footsteps": "Footsteps", "event.whisper": "Whispering", "event.scream": "A distant scream",
    "event.stinger": "Something strikes", "event.heartbeat": "Heartbeat", "event.tinnitus": "Ringing", "event.breath": "Breathing",
    "event.door_creak": "Door creaks", "event.page_turn_distant": "A page turns", "event.scrape_distant": "Scraping, far off",
    "event.silence_break": "Something snaps",
    "item.rasorium.scrape": "Rasorium scrapes", "item.page_turn": "Page turns", "item.quill_scratch": "Quill scratches",
    "item.bookmark.use": "Bookmark used", "item.lens.focus": "Lens focuses", "item.censer.swing": "Censer swings",
    "block.tear.ambient": "Tear crackles", "block.tear.seal": "Tear sealed", "block.veil.ambient": "Veil whispers",
    "block.veil.travel": "The page turns", "block.gate.open": "A gate opens", "block.ritual.begin": "A rite begins",
    "block.ritual.complete": "A rite is read", "block.ritual.fail": "The ink will not take", "block.ward.hum": "Ward hums",
    "block.blank.erase": "Something is erased", "block.sealed_door.open": "Stitches snap",
    "entity.blotling.squish": "Blotling squelches", "entity.blotling.hurt": "Blotling hurts", "entity.blotling.death": "Blotling dies",
    "entity.smudge.ambient": "Smudge murmurs", "entity.smudge.hurt": "Smudge flinches", "entity.smudge.death": "Smudge fades",
    "entity.margin_crawler.ambient": "Margin Crawler chitters", "entity.margin_crawler.hurt": "Margin Crawler hurts",
    "entity.margin_crawler.death": "Margin Crawler dies", "entity.quillcrow.ambient": "Quillcrow caws", "entity.quillcrow.hurt": "Quillcrow hurts",
    "entity.quillcrow.death": "Quillcrow dies", "entity.quillcrow.flap": "Wings flap", "entity.pale_stag.ambient": "Pale Stag calls",
    "entity.pale_stag.hurt": "Pale Stag hurts", "entity.pale_stag.death": "Pale Stag dies", "entity.inkhound.ambient": "Inkhound growls",
    "entity.inkhound.howl": "Inkhound howls", "entity.inkhound.hurt": "Inkhound yelps", "entity.inkhound.death": "Inkhound dies",
    "entity.foxing_moth.ambient": "Moth flutters", "entity.rubricator.ambient": "Rubricator mutters", "entity.rubricator.yes": "Rubricator agrees",
    "entity.rubricator.no": "Rubricator declines", "entity.rubricator.hurt": "Rubricator hurts", "entity.rubricator.death": "Rubricator dies",
    "entity.knocker.ambient": "Heavy breathing", "entity.knocker.lunge": "Knocker screams", "entity.knocker.hurt": "Knocker hurts",
    "entity.knocker.death": "Knocker dies", "entity.copyist.reveal": "Something tears open", "entity.copyist.ambient": "An animal calls, wrongly",
    "entity.copyist.hurt": "Copyist hurts", "entity.copyist.death": "Copyist dies", "entity.longhand.ambient": "Wood creaks",
    "entity.longhand.move": "Something moved", "entity.longhand.hurt": "Longhand splinters", "entity.longhand.death": "Longhand falls",
    "entity.squeeze.crack": "Bones crack", "entity.squeeze.drag": "Something drags itself",
    "entity.knocker.tap": "Knuckles test the wall", "entity.knocker.scratch": "Nails drag along the wall",
    "entity.redacted.ambient": "Static hisses", "entity.redacted.attack": "Redacted slashes", "entity.redacted.hurt": "Redacted hurts",
    "entity.redacted.death": "Redacted is deleted", "entity.erratum.ambient": "Stone grinds", "entity.erratum.reveal": "Erratum shrieks",
    "entity.palehand.ambient": "Something vast moves", "entity.fair_copy.vanish": "Someone leaves",
    "entity.bookbinder.ambient": "Needles click", "entity.bookbinder.needle": "Needle thrown", "entity.bookbinder.rebind": "Thread tightens",
    "entity.bookbinder.hurt": "Bookbinder hurts", "entity.bookbinder.death": "Bookbinder comes apart", "entity.rasure.ambient": "The Rasure breathes",
    "entity.rasure.scrape": "The knife scrapes", "entity.rasure.phase": "The Rasure roars", "entity.rasure.hurt": "The Rasure hurts",
    "entity.rasure.death": "The Rasure dissolves",
}

MESSAGES = {
    "message.palimpsest.codex_entry": "Your Commonplace Book has a new entry: %s",
    "message.palimpsest.insomnia": "You have not slept in days. Something has noticed.",
    "message.palimpsest.no_sleep_undertext": "You cannot sleep here. Something is reading over your shoulder.",
    "message.palimpsest.first_undertext": "The lower writing. The first draft. Everything is quiet the way a page is quiet.",
    "message.palimpsest.dream_warded": "You slept deeply. The red kept watch.",
    "message.palimpsest.sealed_door": "It is stitched shut. A key would have to unpick it.",
    "message.palimpsest.folio_stand_empty": "The stand is waiting for the last page.",
    "message.palimpsest.ritual.busy": "A rite is already being read here.",
    "message.palimpsest.ritual.no_sentence": "The offerings do not make a sentence.",
    "message.palimpsest.ritual.need_night": "The ink will not take in daylight.",
    "message.palimpsest.ritual.need_day": "This rite must be read by daylight.",
    "message.palimpsest.ritual.need_moon_0": "This rite must be read under a full moon.",
    "message.palimpsest.ritual.need_moon_4": "This rite must be read under a new moon.",
    "message.palimpsest.ritual.need_clear": "This rite needs a clear sky.",
    "message.palimpsest.ritual.need_rain": "This rite needs rain.",
    "message.palimpsest.ritual.need_thunder": "This rite needs a storm.",
    "message.palimpsest.ritual.need_overworld": "This rite can only be read on the upper page.",
    "message.palimpsest.ritual.need_undertext": "This rite can only be read on the lower page.",
    "message.palimpsest.ritual.not_ready": "You have not read enough to read this.",
    "message.palimpsest.ritual.need_last_folio": "This can only be read where the last page is kept.",
    "message.palimpsest.ritual.backlash.knocker": "Something heard you reading. It is at the door.",
    "message.palimpsest.ritual.backlash.blotlings": "The ink runs off the altar.",
    "message.palimpsest.ritual.backlash.longhand": "Something tall read it too.",
    "message.palimpsest.ritual.backlash.longhand_watch": "Far off, something tall has turned to face you.",
    "message.palimpsest.ritual.backlash.fair_copy": "The copy is not the only one.",
    "message.palimpsest.ritual.backlash.bleed": "The words are still in your head.",
    "message.palimpsest.ritual.backlash.lights_out": "The lights go out.",
    "message.palimpsest.ritual.backlash.tear": "The page tears.",
    "message.palimpsest.gate.disabled": "Folio Gates are disabled on this server.",
    "message.palimpsest.gate.incomplete": "The frame is not complete: rubricated vellum around an opening two wide and three high.",
    "message.palimpsest.gate.need_night": "The Folio will only read after dark.",
    "message.palimpsest.bookmark.marked": "Your place is kept.",
    "message.palimpsest.compass.spins": "The needle spins.",
    "message.palimpsest.compass.settles": "The needle settles.",
    "message.palimpsest.lens.stone": "There is older writing just under the surface.",
    "message.palimpsest.lens.scraped": "Scraped clean. The words are gone.",
    "message.palimpsest.lens.tear": "The lower page shows through. It is looking back.",
    "message.palimpsest.lens.blank": "Nothing here. Not yet.",
    "message.palimpsest.lens.nothing": "Nothing shows through.",
    "message.palimpsest.tea.calm": "The page settles.",
    "message.palimpsest.tea.too_soon": "Warm, but it does nothing more. Not yet.",
    "message.palimpsest.quill.morning": "%s writes: and then it was morning.",
    "message.palimpsest.quill.clear": "%s writes: and then the rain stopped.",
    "message.palimpsest.quill.nothing": "There is nothing here that needs writing.",
    "message.palimpsest.rubricator.say": "<%s> %s",
    "message.palimpsest.rubricator.killed": "The last rubricator is gone. The red headings will not be rewritten.",
    "message.palimpsest.bookbinder.rebind": "The Bookbinder draws itself back together. Cut the threads.",
    "message.palimpsest.bookbinder.death": "The Bookbinder comes apart. Among the pages: a cartographer's spectacles, and a folio.",
    "message.palimpsest.rasure.arrive": "The knife has come to finish the page.",
    "message.palimpsest.rasure.redaction": "It blanks the pillars. Ink them red again.",
    "message.palimpsest.rasure.exposed": "Its guard breaks.",
    "message.palimpsest.rasure.blank_page": "The page goes white. Listen.",
    "message.palimpsest.rasure.death": "The Rasure dissolves into pages. On the stand where it stood, the last page of the first draft is waiting.",
    "message.palimpsest.ending.sealed": "You write your name over the tear. The ink holds. When you look up, the page is clean, and it is morning.",
    "message.palimpsest.ending.read": "You read it aloud. The first draft opens around you like a book, and does not close. They can see you now. You can see them.",
    "message.palimpsest.ending.unwritten": "You scrape your own name off the page. It comes away easily. Nothing made of ink will ever find you again. Nothing much else will either.",
    "message.palimpsest.sign_undertext": "Beneath the paint, older letters: \"%s\"",
}

SCREENS = {
    "screen.palimpsest.ending.title": "The Last Folio",
    "screen.palimpsest.ending.text": "Here the first draft ends, mid-sentence. Below the last line, in a hand you know, is the first line of the next page:\n\n%s\n\nThe tear is open. The knife is blunt. Something has to be written here.",
    "screen.palimpsest.ending.seal": "Write your name over the tear",
    "screen.palimpsest.ending.seal.desc": "Close the tear for good. Your page will be clean: no more Bleed, no more visitors. You keep the pen.",
    "screen.palimpsest.ending.read": "Read it aloud",
    "screen.palimpsest.ending.read.desc": "Keep the first draft open around you forever. The lower writing will never stop showing through. You wear its crown.",
    "screen.palimpsest.ending.scrape": "Scrape your own name away",
    "screen.palimpsest.ending.scrape.desc": "Take the knife to yourself. What is not written cannot be found.",
    "screen.palimpsest.ending.confirm": "It cannot be unwritten",
    "screen.palimpsest.ending.chosen": "Chosen: %s",
    "codex.palimpsest.empty": "Nothing noticed here yet.",
    "codex.palimpsest.category.the_page": "The Page",
    "codex.palimpsest.category.observations": "Observations",
    "codex.palimpsest.category.creatures": "Creatures",
    "codex.palimpsest.category.places": "Places",
    "codex.palimpsest.category.craft": "Craft & Rites",
    "codex.palimpsest.category.folios": "Folios",
    "key.palimpsest.open_codex": "Open Commonplace Book",
    "key.categories.palimpsest": "Palimpsest",
    "itemGroup.palimpsest": "Palimpsest",
    "effect.palimpsest.erasure": "Erasure",
    "effect.palimpsest.warded": "Warded",
    "effect.palimpsest.inkblind": "Inkblind",
    "item.palimpsest.torn_folio.numbered": "Torn Folio (%s)",
    "item.palimpsest.margin_compass.attuned": "Bound to the Last Folio",
    "item.palimpsest.bookmark.marked": "Marked at %s, %s, %s",
    "item.palimpsest.rubric_armor.desc": "Inked with rubrics: each piece slows the Bleed by a tenth.",
    "entity.minecraft.villager.palimpsest.rubricator": "The Rubricator",
    "palimpsest.configuration.title": "Palimpsest",
}


def gen():
    L = {}
    for k, v in BLOCKS.items():
        L[f"block.palimpsest.{k}"] = v
    for k, (name, desc) in ITEMS.items():
        L[f"item.palimpsest.{k}"] = name
        if desc:
            L[f"item.palimpsest.{k}.desc"] = desc
    for k, v in USES.items():
        L[f"item.palimpsest.{k}.use"] = v
    for k in ("foxing_moth", "blotling", "smudge", "margin_crawler", "quillcrow", "pale_stag", "inkhound", "rubricator", "knocker",
              "copyist", "longhand", "redacted", "fair_copy", "erratum"):
        L[f"item.palimpsest.{k}_spawn_egg"] = f"{ENTITIES[k].replace('The ', '')} Spawn Egg"
    for k, v in ENTITIES.items():
        L[f"entity.palimpsest.{k}"] = v
    for k, v in BIOMES.items():
        L[f"biome.palimpsest.{k}"] = v
    for k, v in SUBTITLES.items():
        L[f"subtitles.palimpsest.{k}"] = v
    L.update(MESSAGES)
    L.update(SCREENS)
    for i, m in lore.STAGE_MESSAGES.items():
        L[f"message.palimpsest.stage.{i}"] = m
    for i, m in lore.STAGE_DESCRIPTIONS.items():
        L[f"codex.palimpsest.stage.{i}"] = m
    for i, m in enumerate(lore.DREAMS):
        L[f"message.palimpsest.dream.{i}"] = m
    for i, m in enumerate(lore.RUBRICATOR_LINES):
        L[f"message.palimpsest.rubricator.{i}"] = m
    for i, m in enumerate(lore.SIGN_UNDERTEXT):
        L[f"message.palimpsest.sign_undertext.{i}"] = m
    for k, (title, text) in lore.CODEX.items():
        L[f"codex.palimpsest.{k}.title"] = title
        L[f"codex.palimpsest.{k}.text"] = text
    for i, (title, text) in lore.FOLIOS.items():
        L[f"lore.palimpsest.folio.{i}.title"] = f"Folio {ROMAN[i - 1]}: {title}"
        L[f"lore.palimpsest.folio.{i}.text"] = text
        L[f"codex.palimpsest.folio_{i}.text"] = text
    for i, (title, text) in lore.FADED.items():
        L[f"lore.palimpsest.faded.{i}.title"] = title
        L[f"lore.palimpsest.faded.{i}.text"] = text
    L.update(data_advancements.lang())
    asset("lang/en_us.json", dict(sorted(L.items())))
    return L
