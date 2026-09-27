"""
All player-facing writing for Palimpsest: names, descriptions, the Commonplace Book, the
folios, messages. Kept in one place so the voice stays consistent.
"""

FOLIOS = {
    1: ("The Northern Reach", "The survey of the northern reach is complete, save for one hill I cannot account for.\n\nI drew it on the Tuesday: a round green hill with a single oak on its shoulder. On the Thursday it was gone. Where it had stood the grass was pale and flat, as if something had been rubbed out with the heel of a hand, and the oak lay on its side with no stump.\n\nI have redrawn the sheet. I will not mention this to the Society.\n\n- I. Wray"),
    2: ("On the Grey Stone", "The masons call it palimpsest stone: the grey that shows letters when it is wet.\n\nThis morning I took a penknife to a vein of it, the way a scribe scrapes old vellum clean to use it again. A rasorium, they called the knife in the scriptoria. The surface came away in a curl, like wax.\n\nUnderneath there was writing. Not carving. Writing, in an ink that was still wet.\n\nThe flake I lifted is on my desk. It is warm."),
    3: ("Gall and Iron", "The lower writing will only take iron-gall ink. Soot ink smears off it like breath off glass.\n\nThe old recipe: oak galls (the little brown nuts wasps leave on oak leaves; knock enough leaves down and you will find them), a nail's worth of iron, and a clean bottle. Let it blacken.\n\nI keep a commonplace book now, as scholars used to: everything I notice, written down the day I notice it. A book, a gall for the ink, a feather to write with. I recommend it. Some days the book seems to know things before I write them."),
    4: ("The Knocking", "Something knocked at the cabin door at the third hour.\n\nThree knocks. A pause long enough to breathe in. Three more.\n\nI did not answer. At dawn there were no prints in the frost, only a long pale streak down the outside of the door, as if someone very tall had leaned the whole length of themselves against it and waited.\n\nI have learned this much: whatever knocks after midnight is not asking to be let in. It is asking you to open the door."),
    5: ("Vermilion", "The rubricators, the monks who wrote the red headings in old books, used vermilion, which is ground cinnabar. Cinnabar lies deep, where the stone is hot.\n\nIt is a mineral. It does not soak into the page; it sits on it. I think that is why, wherever the lower writing shows, the red letters have survived and the black have not.\n\nI drew a red line across my threshold in chalk made from it. The knocking stopped. I have not slept better in a year."),
    6: ("Rubrication", "Copying is not enough. The old writers had a way of making a page do something. They called it rubrication.\n\nA red altar at the centre. Stands around it, close, each holding one piece of the sentence. You hold the last word yourself, and read it at the altar.\n\nSome sentences only read at night. Some only under a new moon, or in a storm, or in the lower page itself. Every sentence costs you something to read. And some sentences, read wrong, read back."),
    7: ("The First Draft", "The stone says this world was written over another.\n\nSame hills. Same rivers. Same sea. The same seed, grown twice. The first draft was scraped away to make room for this one, as a scribe scrapes a page he means to reuse.\n\nIt was not scraped well. The people of the first draft are still there underneath, thin as breath, going about their days in a world with half its words gone. They call their place the Undertext.\n\nThey can see us. We are the upper writing. We are written on top of them."),
    8: ("The Door", "A door into the lower page must be framed in red: rubricated vellum, the opening two wide and three high.\n\nThe Folio of Descent is written at the altar, at night, above ground: four fragments of the lower writing, a sheet of blank vellum, iron-gall ink, a black quill, and a candlewort flower, read over a book. Read the Folio aloud at the frame, after dark.\n\nI have made one. The lamps went out when I finished writing it, all of them at once.\n\nI have not used it yet."),
    9: ("Below", "I went down.\n\nThe sky is the colour of old paper and lit from nowhere. There is a black sun that does not move. It is quiet the way a page is quiet.\n\nThe faded people walk the same few steps again and again, as if re-reading a line they cannot finish. If you watch one long enough, it notices. It follows. It means no harm.\n\nOne of them spoke: a scribe in red, whose name had been scraped away but whose headings had not. He trades. He wants the pages that fall from the faded ones, and the fragments of lower writing, and he gives what he kept. A bookmark from him brought me home."),
    10: ("The Tall One", "There is something tall down here that only moves when I look away.\n\nIn the dark I cannot tell whether I am looking at it or not, and neither, I think, can it. Carry a light.\n\nMy knife went through it like smoke. The red scribe says the gold the old illuminators laid on their capitals, illumine, burns it; so does iron that has drunk gall-ink. He says it will not cross red chalk, and will not stand near a lit ward.\n\nI am making a sword. I am also going to stop looking behind me."),
    11: ("The Rasure", "Tonight I heard it: a long scraping, far off, the sound a knife makes on vellum. Moving.\n\nThe red scribe says it is the Rasure: the knife that scraped the first draft, left behind when the writer moved on to the new page. It is still working. It does not know it is finished. It is working its way up.\n\nThere is a place where the tear between the pages is widest, where the last page of the first draft is kept. The Bindery guards the way to it.\n\nI am going to sew the tear shut from this side. If this is my last folio, the compass will show you the Bindery. Bring red."),
    12: ("The Bookbinder", "The thread holds. The needles hold. I hold.\n\nI can feel the books growing into my back. The spine of them is my spine. I think I have been here a very long time.\n\nIf you are reading this you have cut me loose, and I am grateful, and I am sorry.\n\nThe key is my spine: take it. The Last Folio lies past the stitched door. Bind your compass to it at the altar. When you face the Rasure, re-ink the pillars; it cannot abide the red. When the page goes white, listen for footsteps.\n\nAnd when it is done you will be given a choice. I do not know the right one. I only know what I would have chosen, and I cannot remember what it was."),
}

FADED = {
    1: ("A Lullaby", "hush now, the page is turning\nhush now, the §kink§r is dry\nhush, the §kwriter§r has gone to supper\nand left the §klamp§r to burn us by\n\nhush now, the §kknife§r is resting\nhush now, the §kmorning§r's gone\nwe'll be here in the margin\nwhen the §knext one§r's written on"),
    2: ("A Recipe", "Three cups of §kflour§r, a spoon of salt, warm water.\nKnead until it stops being §kstick§r.\nLeave it by the §kstove§r under a cloth until it has grown to twice its §k......§r.\n\nWe have not had §kbread§r since the east side was scraped. I write it down so that someone remembers the §ksmell§r."),
    3: ("A List of Names", "§kAldous Pell§r\n§kMarit Pell§r\n§kthe Harrow children§r\n§kOld Tamsin§r\n§kFen§r\n§kthe miller§r\n§k......§r\n\nWe write the names on the red page now. The red page does not scrape."),
    4: ("Four Words", "we are still here\n\nwe are still here\n\nwe are §kstill§r here\n\nwe are §k.....§r here"),
    5: ("A Letter", "My love,\nthey are scraping the east side of the village today. You can hear it from the well: a long sound, like someone drawing a breath that does not end. When it passes, the houses are still there but they are §kpale§r, and nobody in them remembers their own §kname§r.\n\nIf it comes for our street, do not stand in the road. Stand on the red step. Hold on to something red.\n\nI will meet you at the §k......§r."),
    6: ("A Drawing", "(The page is a child's drawing: a house with a door, a round sun, four figures holding hands.)\n\ni drew a house so we would have somewhere to live when it comes\n\n(Something has scraped the sun out, very carefully, leaving the rays.)"),
    7: ("Rules", "RULES FOR AFTER DARK\n1. Do not open for knocking. Your family has keys.\n2. Do not look at the tall one. Do not look away from it either. Carry a lamp.\n3. Count your animals twice.\n4. If you meet yourself, do not touch yourself.\n5. If the birds go quiet, get off the road.\n6. §k.............................§r"),
    8: ("A Thought", "the writer did not hate us.\n\nthe writer simply finished, and turned the page, and began again.\n\nthat is what we cannot forgive."),
    9: ("Directions", "From the well walk toward the black sun until the ground turns red. Where the red is widest there is a building of books, and past it a door sewn shut, and past that the §klast page§r.\n\nThe man who went in to sew it closed did not come back out. We hear his needles at night."),
    10: ("When It Comes", "when the knife comes it is quiet.\n\nyou do not hear it. you hear the silence where the birds were.\n\nthen you see the red lines on the ground, and you have the time it takes to say your own name to step off them.\n\nsay it quickly."),
}

# id -> (title, text)
CODEX = {
    "stage_1": ("Faint Trace", "I have the sense, lately, of having read this page before. Nothing I can point to. A sound that stopped as I turned. A light that was on and then was not."),
    "stage_2": ("Ghosting", "Something is written underneath. I catch it at the edges: knocking where no one is, a sign that said one thing and then another, my own things not quite where I left them."),
    "stage_3": ("Bleed-through", "The lower writing is coming through. Things stand at the edge of sight and are gone. The fog comes in when it should not. I am no longer sure what is an animal."),
    "stage_4": ("Running Ink", "The ink is running. The grass fades where I walk. The lights fail together. Sometimes it rains black. Sometimes the world goes silent all at once, as if listening."),
    "stage_5": ("The Tear", "The page is tearing. There are rips in the air that leak the lower world. Once, for a moment, the sky was the colour of old paper."),
    "stage_6": ("Overwritten", "I am being written over. I have seen myself standing where I was not. Something is scraping the ground near my house, and it is careful, and it is patient."),

    "footsteps": ("Footsteps", "Footsteps behind me, unhurried, stopping when I stop. Once, sounds I know: a chest lid, a hiss, a door. Nothing there when I turn. It is always behind me."),
    "whispers": ("Whispers", "Something said something at my ear. Not words, quite. The sound words make before they are finished."),
    "snuffed_torch": ("Spent Torches", "Torches going out on their own. Not blown out: finished, as if they had burned for a week. Flint and steel lights them again. I have started carrying a lot of flint."),
    "the_sign": ("The Sign", "A sign I made said something else when I passed it. When I came back it said what I wrote. I have stopped reading my signs twice."),
    "knocking": ("Knocking", "Three knocks at the door after dark. A pause. Three more. I did not open it. Wray says: it is not asking to come in. It is asking you to open the door."),
    "moved_things": ("Moved Things", "A door I shut is open. A chest I packed is in a different order: everything there, nothing where it was. Things are being read, and put back carelessly."),
    "watchers": ("Watchers", "Black birds with steel nibs for beaks sit high and watch me, and leave the moment I come near. I killed one. It felt like being noticed. It felt like something wrote that down."),
    "fog": ("Fog", "A fog came in from nowhere and stayed exactly as long as I was frightened of it."),
    "the_other_page": ("The Other Page", "For a moment, everything around me was the wrong material: pale, ruled with faint lines, the trees black as blots. Then it was the world again. I have not told anyone."),
    "silence": ("Silence", "Every sound stopped. Wind, animals, my own footsteps. It lasted about as long as I could hold my breath, and then something snapped, very close."),
    "ink_rain": ("Ink Rain", "It rained, and the rain was black, and it did not wet anything."),
    "someone_typed": ("Someone Typed", "Words in the chat that nobody wrote. Sometimes in my own name. I have started keeping logs."),
    "fading": ("Fading", "Grass going grey in patches, as if the colour had been rubbed out. It grows back in sunlight, slowly. It is worse near where I sleep."),
    "tears": ("Tears", "Rips in the air, a little taller than me, oozing ink. Things come out of them. A Seal of Closing presses them shut. Wards keep new ones from opening near home."),
    "scraping": ("Scraping", "A long scraping sound, and then a stripe of the world is simply blank: white, humming, gone. It comes back after a while. The Rasure is practising."),

    "foxing_moth": ("Foxing Moth", "Pale moths freckled brown like old paper. They drift toward light and leave a fine dust. Harmless. Where there are many of them, the page is thin."),
    "blotling": ("Blotling", "A drop of ink that decided to move. Splits when struck. A rasorium makes short work of them; so does keeping your feet dry."),
    "smudge": ("Smudge", "One of the faded people of the first draft: an outline with the face rubbed out. It walks the same few steps over and over. Look at it long enough and it follows, murmuring. It will not hurt you. Hurt it and it comes apart, sometimes leaving a page."),
    "margin_crawler": ("Margin Crawler", "The doodles from the margins of old manuscripts: a snail in a knight's helmet, a lance too long for it. They climb walls and go for the ankles. Silly, until there are six."),
    "quillcrow": ("Quillcrow", "Crows with steel nibs for beaks. Watchers. They sit high and take their leave when approached. Killing one is remembered, and not by me."),
    "pale_stag": ("Pale Stag", "A deer the colour of blank paper, with no face at all. In the Undertext they graze and bolt. Above, one stands at the treeline, facing me, and is gone before I can get near."),
    "inkhound": ("Inkhound", "Lean hounds of dried ink with no eyes. They hunt by sound: sprinting, jumping, fighting, digging. Crouch, move slowly, and they lose you."),
    "rubricator": ("The Rubricator", "The last scribe of the first draft. His name was scraped away; his red headings were not, and neither was he, mostly. He trades what he kept for what I find: faded pages, fragments, gilt. Do not hurt him."),
    "knocker": ("The Knocker", "Taller than a door, arms to its knees. It comes to houses at night and knocks: three, and three. It cannot open doors. It waits for you to. If you do, or if you step outside while it is there, it comes for you, fast. Light and red wards drive it off. At dawn it leaves."),
    "copyist": ("The Copyist", "Something that learned what an animal is by watching them, and got it almost right. It wears a cow or a pig or a sheep and stands with the herd. Tells: no shadow, never grazes, always facing me, a voice a little too low. Up close, fed, struck or seen through a Reading Lens, it stops pretending."),
    "longhand": ("The Longhand", "One long pen-stroke of a figure. It only moves when no one is looking, and in darkness no one is. Ordinary weapons pass through it. Inkbane cuts. It will not cross red chalk or come near a lit ward. At dawn it is gone. Carry light. Do not blink."),
    "redacted": ("The Redacted", "A figure censored with black bars. Its cuts do not heal while Erasure lasts, and sometimes the name of something I carry goes black. Struck, it is somewhere else. Sealing wax stops the Erasure."),
    "fair_copy": ("The Fair Copy", "In a manuscript the fair copy is the clean final version, made from the messy draft. I saw myself, holding what I held, standing where I was not. It walked away when I came near. I did not strike it. Wray's note says: do not touch yourself."),
    "erratum": ("Erratum", "A block that is in the wrong place. I stood on it. When I looked back it was closer."),
    "palehand": ("The Hand", "Far away, at night, behind the hills: a hand the size of a mountain, pale as bone, moving its fingers as if writing. Then it went back down. I do not think it knew I was there. I do not think it knows anything is there."),
    "bookbinder": ("The Bookbinder", "Idris Wray tried to sew the tear shut from the inside and the Bindery sewed him into itself: a hunched man grown into a heap of stitched books, walking on needles. At half strength it rebinds itself through three stitched tomes; break them and it falls open. It was a cartographer, once."),
    "rasure": ("The Rasure", "The knife that scraped the first draft, still scraping. It lines the floor in red and hollows it. It blanks the four pillars and hides behind their silence: re-ink them with vermilion or chalk. At the end the page goes white and it goes invisible; censer smoke and ink show it, and it has footsteps."),

    "wray_cabin": ("Wray's Cabin", "A cartographer's cabin, long empty. Maps that do not match the land. A cellar with writing on the walls."),
    "scraped_obelisk": ("Scraped Obelisk", "Standing stones with their faces scraped smooth. Someone wanted something forgotten. Palimpsest stone around the base."),
    "hollow_chapel": ("Hollow Chapel", "A chapel with a red altar where the font should be, and hymnals with every hymn scraped out except the headings."),
    "copying_house": ("The Copying House", "A ruined scriptorium in the dark woods. Desks, ink, vellum, and a great deal of work abandoned mid-sentence."),
    "survey_station": ("Station Four", "An underground survey station: iron doors, logbooks, a gate frame bricked up from the inside. The Anomalous Terrain Survey came looking for Wray. Their logs stop on the same day."),
    "crow_roost": ("Crow Roost", "A dead tree hung with black feathers. The quillcrows sleep here, facing outward."),
    "doubled_house": ("The House Written Twice", "A house that exists twice, one inside the other: a door that opens onto a wall, stairs that meet the ceiling. The second writing did not quite cover the first."),
    "broken_gate": ("Broken Gate", "A red-framed doorway deep underground, cracked through. Somebody tried to read their way down, a long time ago, and the reading failed."),
    "undertext": ("The Undertext", "The lower writing. The first draft. A world like ours with half its words scraped off: paper sky, black sun, ink seas, ruled ground. The bed will not let me sleep. A bookmark will take me home."),
    "faded_village": ("Faded Village", "Outlines of houses and the faded people who still keep them. The Rubricator is often here."),
    "marginalia_spire": ("Marginalia Spire", "A tower drawn in the margin by someone bored: too tall, too thin, full of doodles that crawl."),
    "ink_well": ("Ink Well", "A great round shaft of inkstone going down into the black. Hounds circle it. There is gilt at the bottom."),
    "bindery": ("The Bindery", "A building made of books, sewn together with red thread. The Bookbinder waits inside. The Margin Compass points here in the lower page."),
    "last_folio": ("The Last Folio", "Past the stitched door: four pillars, an altar, and the last page of the first draft. Read the Rite of the Last Reading here and the Rasure comes."),

    "palimpsest_stone": ("Palimpsest Stone", "Grey stone with older writing just beneath the surface. Mined, it is just stone. Scraped with a rasorium, it gives up a fragment of the lower writing and turns pale."),
    "rasorium": ("The Rasorium", "A scribe's scraping knife. Scrapes palimpsest stone, vellum soil and signs (you learn what they said before). Quick in a fight, and bites deep into anything made of ink."),
    "iron_gall_ink": ("Iron-Gall Ink", "Oak galls, iron, a bottle. The only ink the lower writing takes. Oak galls fall from oak leaves."),
    "vermilion": ("Vermilion", "Ground cinnabar: the red of the rubricators, which does not scrape away. Makes rubric chalk, wards, and the red frames of gates. Cinnabar lies deep, near lava."),
    "scriptorium_desk": ("Scriptorium Desk", "A surface, an ink and a subject, worked into something new: compasses, dowsing quills, vestments, and one strange record."),
    "rites": ("Rites", "A Rubric Altar with Reading Stands close around it. Lay one offering on each stand, hold the catalyst, use the altar. Conditions matter: time, moon, weather, which page you stand on. Every rite costs a little of you. Some bite back; friends standing near make that less likely.\n\nKnown rites: Warding, the Open Page, Recall, Closing, Unmaking, Quieting, Illumination, Binding, the Last Reading. Others are hinted at."),
    "rubric_ward": ("Rubric Ward", "A lamp glazed in vermilion. Inkborn will not come within sixteen blocks. Knockers leave warded doors alone. Tears will not open near it. Sleeping beside one lets the page settle."),
    "rubric_chalk": ("Rubric Chalk", "Red chalk lines on the ground. To anything made of ink they are a wall. To everything else they are chalk."),
    "folio_gate": ("Folio Gates", "Frame a doorway in rubricated vellum bricks: the opening two wide, three high. Read a Folio of Descent at the frame, at night. Stand in the veil until it finishes the sentence. The Undertext lies directly beneath: the gate below opens where the gate above stands."),
    "bookmark": ("Bookmark", "Keeps your place. Used above, it remembers where you stand; used below, it takes you back there, and is spent."),
    "reading_lens": ("Reading Lens", "Gilt lens with lower writing in the glass. Shows what is made of ink, and what is pretending not to be."),
    "margin_compass": ("Margin Compass", "Points at the nearest place where the page is thin. Above: old ruins. Below: the Bindery. Bound at the altar with the Bookbinder's needle, it points to the Last Folio."),
    "gall_iron": ("Gall Iron", "Iron steeped in iron-gall ink, then blasted: it holds an edge against ink. Inkbane."),
    "illumine": ("Illumine", "Gilt from the lower page: the gold leaf illuminators laid on their capitals. It glows. It burns ink."),
    "remedies": ("Remedies", "Candlewort tea settles the Bleed a little, once in a while. Sealing wax, chewed, stops Erasure and clears ink from the eyes."),
    "seals": ("Seals of Closing", "Red wax pressed over a rip in the page. Closes a Tear. Writes a blank back at once."),

    "faded_pages": ("Faded Pages", "Pages from the first draft, most of their words scraped to noise. The faded people drop them. The Rubricator collects them."),
    "the_choice": ("The Choice", "The Last Folio lies on its stand, with my name on it. I can write my name over the tear, and the page will be clean. I can read it aloud, and the first draft will stay open around me. There may be a third way. I would need the knife to find it."),
}
for i, (title, _) in FOLIOS.items():
    CODEX[f"folio_{i}"] = (f"Folio {['I','II','III','IV','V','VI','VII','VIII','IX','X','XI','XII'][i-1]}: {title}",
                           "One of Idris Wray's folios. I have copied it out; read the original to see his hand.")

STAGE_DESCRIPTIONS = {
    0: "The page is clean.",
    1: "Something on this page has been written before.",
    2: "The lower writing shows at the edges.",
    3: "The lower writing is coming through.",
    4: "The ink is running.",
    5: "The page is tearing.",
    6: "I am being written over.",
}

STAGE_MESSAGES = {
    1: "You have the sense you have read this page before.",
    2: "Something is written underneath.",
    3: "The lower writing is coming through.",
    4: "The ink is running.",
    5: "The page is tearing.",
    6: "You are being written over.",
}

DREAMS = [
    "You dreamt of a page being turned very slowly.",
    "You dreamt of lines of writing under the floorboards.",
    "You dreamt someone was reading over your shoulder.",
    "You dreamt of a black sun that did not move.",
    "You dreamt of your own name, scraped off a door.",
    "You dreamt of knocking. You did not dream of answering.",
]

RUBRICATOR_LINES = [
    "Red does not scrape. Remember that, when it comes.",
    "They took my name. They could not take my headings.",
    "You are the upper writing. You sit on us like frost on a window.",
    "The tall one does not move while you watch. So watch.",
    "Bring me pages. I am putting a book back together.",
    "The cartographer came through here. He bought thread.",
    "Do not open for knocking. Your family has keys.",
    "The knife is not cruel. The knife is only unfinished.",
    "There is a red step outside every house here. Stand on it.",
    "Somewhere above, someone is writing about you right now.",
    "Count your animals twice.",
    "When the birds go quiet, get off the road.",
]

SIGN_UNDERTEXT = [
    "HERE LIVED THE HARROWS", "MIND THE WELL", "TURN BACK", "SEVEN MILES TO THE RED ROAD", "NO KNOCKING AFTER DARK",
    "WE ARE STILL HERE", "BREAD AND SALT", "THIS WAY TO THE LAST PAGE", "DO NOT LOOK UP", "WRAY WAS HERE",
]
