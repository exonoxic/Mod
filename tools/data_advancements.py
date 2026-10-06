"""The advancement tree. Most criteria are granted from code (minecraft:impossible)."""
from jsonout import *

A = []  # (id, parent, icon, frame, hidden, criteria)


def adv(id, parent, icon, frame="task", hidden=False, criteria=None, title=None, desc=None):
    A.append((id, parent, icon, frame, hidden, criteria or {"granted": {"trigger": "minecraft:impossible"}}, title, desc))


def has(*items):
    return {"has": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": [rl(i) for i in items]}]}}}


# id, parent, icon, frame, hidden, criteria, title, description
adv("root", None, "blank_vellum", title="Palimpsest", desc="Something on this page has been written before.")
adv("marginalia", "root", "commonplace_book", criteria=has("commonplace_book"), title="Marginalia", desc="Begin keeping notes. You will need them.")
adv("under_the_surface", "root", "undertext_fragment", title="Under the Surface", desc="Scratch the surface and it scratches back.")
adv("wray_was_here", "marginalia", "torn_folio", title="Wray Was Here", desc="Someone else noticed first.")
adv("cartographer", "wray_was_here", "torn_folio", "goal", title="The Cartographer", desc="Read the last of Idris Wray's folios.")
adv("vermilion", "under_the_surface", "vermilion", criteria=has("vermilion"), title="Vermilion", desc="Red ink does not scrape away.")
adv("the_desk", "marginalia", "scriptorium_desk", criteria=has("scriptorium_desk"), title="A Steady Hand", desc="Every copy begins at a desk.")
adv("three_knocks", "root", "minecraft:oak_door", title="Three Knocks", desc="Don't open it. It leaves at dawn.")
adv("opened_it", "three_knocks", "minecraft:iron_door", "challenge", True, title="You Had to Know", desc="Open the door.")
adv("fourteen_cows", "root", "minecraft:leather", title="Fourteen Cows", desc="Count them again.")
adv("dont_blink", "root", "minecraft:ender_eye", title="Don't Blink", desc="It only moves when you aren't looking. So look.")
adv("bleed_through", "root", "scrawl", "goal", title="Bleed-through", desc="The lower writing is coming through.")
adv("rubrication", "vermilion", "rubric_altar", title="Rubrication", desc="Is this really a good idea?")
adv("many_hands", "rubrication", "reading_stand", title="Many Hands", desc="Read a rite with someone at your side.")
adv("the_open_page", "rubrication", "folio_of_descent", criteria=has("folio_of_descent"), title="The Open Page", desc="Write the Folio of Descent.")
adv("read_aloud", "the_open_page", "rubricated_vellum_bricks", "goal", title="Read Aloud", desc="Open a Folio Gate.")
adv("the_undertext", "read_aloud", "ruled_vellum", "goal", title="The Undertext", desc="The lower writing. The first draft.")
adv("keep_your_place", "the_undertext", "bookmark", title="Keep Your Place", desc="Use a Bookmark to come home.")
adv("the_rubricator", "the_undertext", "vermilion", title="The Last Rubricator", desc="Meet the scribe whose headings survived.")
adv("illuminated", "the_undertext", "illumine_leaf", criteria=has("illumine_leaf"), title="Illuminated", desc="Gold for the capitals. Fire for the ink.")
adv("gall_iron", "vermilion", "gall_iron_ingot", criteria=has("gall_iron_ingot"), title="Iron That Drank Ink", desc="Forge gall iron.")
adv("the_tear", "bleed_through", "seal_of_closing", "goal", title="The Tear", desc="The page is tearing.")
adv("a_tear_in_the_page", "the_tear", "seal_of_closing", title="Mending", desc="Seal a Tear.")
adv("station_four", "wray_was_here", "minecraft:iron_door", title="Station Four", desc="Their logs stop on the same day.")
adv("written_twice", "root", "minecraft:oak_planks", "task", True, title="Written Twice", desc="The second writing didn't quite cover the first.")
adv("the_bindery", "the_undertext", "binding_thread", title="The Bindery", desc="A building sewn together with red thread.")
adv("spine", "the_bindery", "spine_key", "challenge", title="Spine", desc="He was a cartographer, once.")
adv("the_last_folio", "spine", "folio_stand", "goal", title="The Last Folio", desc="Past the stitched door.")
adv("the_last_reading", "the_last_folio", "spine_key", "goal", title="The Last Reading", desc="Call the knife.")
adv("rasure", "the_last_reading", "rasure_shard", "challenge", title="Rasure", desc="Blunt the knife that scraped the first draft.")
adv("ending_sealed", "rasure", "scriveners_quill", "challenge", title="A Fair Copy", desc="Write your name over the tear.")
adv("ending_read", "rasure", "circlet_of_the_first_draft", "challenge", title="Read It Aloud, Forever", desc="Keep the first draft open.")
adv("ending_unwritten", "rasure", "rasure_edge", "challenge", True, title="Unwritten", desc="Scrape your own name off the page.")
adv("which_one", "the_tear", "minecraft:player_head", "task", True, title="Which One of You", desc="Meet the Fair Copy.")
adv("that_wasnt_there", "bleed_through", "misprint", "task", True, title="That Wasn't There Before", desc="Meet an Erratum.")
adv("the_hand", "the_tear", "minecraft:bone_block", "challenge", True, title="The Hand", desc="It is still writing.")
adv("overwritten", "the_tear", "blank_vellum", "goal", True, title="Overwritten", desc="Reach the final stage.")
adv("lower_writing", "the_undertext", "music_disc_lower_writing", "task", True, criteria=has("music_disc_lower_writing"),
    title="Lower Writing", desc="A lullaby from the first draft.")
adv("inkbane", "gall_iron", "gall_iron_sword", criteria=has("gall_iron_sword", "iron_rasorium", "illumined_censer", "rasure_edge"),
    title="Inkbane", desc="Carry something that cuts ink.")
adv("warded", "rubrication", "rubric_ward", criteria=has("rubric_ward"), title="Red at the Threshold", desc="Make a Rubric Ward.")


def gen():
    for (id, parent, icon, frame, hidden, criteria, title, desc) in A:
        obj = {"display": {"icon": {"item": rl(icon)}, "title": {"translate": f"advancements.palimpsest.{id}.title"},
                           "description": {"translate": f"advancements.palimpsest.{id}.description"},
                           "frame": frame, "show_toast": True, "announce_to_chat": not hidden, "hidden": hidden},
               "criteria": criteria, "requirements": [[k] for k in criteria.keys()]}
        if len(criteria) > 1:
            obj["requirements"] = [list(criteria.keys())]
        if parent:
            obj["parent"] = f"{NS}:{parent}"
        else:
            obj["display"]["background"] = f"{NS}:textures/block/vellum_bricks.png"
            obj["display"]["announce_to_chat"] = False
        data(f"advancements/{id}.json", obj)


def lang():
    out = {}
    for (id, parent, icon, frame, hidden, criteria, title, desc) in A:
        out[f"advancements.palimpsest.{id}.title"] = title
        out[f"advancements.palimpsest.{id}.description"] = desc
    return out
