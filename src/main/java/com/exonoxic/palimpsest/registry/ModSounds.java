package com.exonoxic.palimpsest.registry;

import com.exonoxic.palimpsest.Palimpsest;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Every sound is original, synthesised by tools/gen_sounds.py. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Palimpsest.MODID);

    // Ambience & music
    public static final RegistryObject<SoundEvent> UNDERTEXT_LOOP = reg("ambient.undertext.loop");
    public static final RegistryObject<SoundEvent> UNDERTEXT_ADDITIONS = reg("ambient.undertext.additions");
    public static final RegistryObject<SoundEvent> UNDERTEXT_MOOD = reg("ambient.undertext.mood");
    public static final RegistryObject<SoundEvent> MUSIC_UNDERTEXT = reg("music.undertext");
    public static final RegistryObject<SoundEvent> MUSIC_RASURE = reg("music.rasure");
    public static final RegistryObject<SoundEvent> MUSIC_DISC_LOWER_WRITING = reg("music_disc.lower_writing");

    // Director events
    public static final RegistryObject<SoundEvent> EVENT_KNOCK = reg("event.knock");
    public static final RegistryObject<SoundEvent> EVENT_FOOTSTEPS = reg("event.footsteps");
    public static final RegistryObject<SoundEvent> EVENT_WHISPER = reg("event.whisper");
    public static final RegistryObject<SoundEvent> EVENT_SCREAM = reg("event.scream");
    public static final RegistryObject<SoundEvent> EVENT_STINGER = reg("event.stinger");
    public static final RegistryObject<SoundEvent> EVENT_HEARTBEAT = reg("event.heartbeat");
    public static final RegistryObject<SoundEvent> EVENT_TINNITUS = reg("event.tinnitus");
    public static final RegistryObject<SoundEvent> EVENT_BREATH = reg("event.breath");
    public static final RegistryObject<SoundEvent> EVENT_DOOR_CREAK = reg("event.door_creak");
    public static final RegistryObject<SoundEvent> EVENT_PAGE_TURN_DISTANT = reg("event.page_turn_distant");
    public static final RegistryObject<SoundEvent> EVENT_SCRAPE_DISTANT = reg("event.scrape_distant");
    public static final RegistryObject<SoundEvent> EVENT_SILENCE_BREAK = reg("event.silence_break");

    // Items
    public static final RegistryObject<SoundEvent> RASORIUM_SCRAPE = reg("item.rasorium.scrape");
    public static final RegistryObject<SoundEvent> PAGE_TURN = reg("item.page_turn");
    public static final RegistryObject<SoundEvent> QUILL_SCRATCH = reg("item.quill_scratch");
    public static final RegistryObject<SoundEvent> BOOKMARK_USE = reg("item.bookmark.use");
    public static final RegistryObject<SoundEvent> LENS_FOCUS = reg("item.lens.focus");
    public static final RegistryObject<SoundEvent> CENSER_SWING = reg("item.censer.swing");

    // Blocks
    public static final RegistryObject<SoundEvent> TEAR_AMBIENT = reg("block.tear.ambient");
    public static final RegistryObject<SoundEvent> TEAR_SEAL = reg("block.tear.seal");
    public static final RegistryObject<SoundEvent> VEIL_AMBIENT = reg("block.veil.ambient");
    public static final RegistryObject<SoundEvent> VEIL_TRAVEL = reg("block.veil.travel");
    public static final RegistryObject<SoundEvent> GATE_OPEN = reg("block.gate.open");
    public static final RegistryObject<SoundEvent> RITUAL_BEGIN = reg("block.ritual.begin");
    public static final RegistryObject<SoundEvent> RITUAL_COMPLETE = reg("block.ritual.complete");
    public static final RegistryObject<SoundEvent> RITUAL_FAIL = reg("block.ritual.fail");
    public static final RegistryObject<SoundEvent> WARD_HUM = reg("block.ward.hum");
    public static final RegistryObject<SoundEvent> BLANK_ERASE = reg("block.blank.erase");
    public static final RegistryObject<SoundEvent> SEALED_DOOR_OPEN = reg("block.sealed_door.open");

    // Creatures
    public static final RegistryObject<SoundEvent> BLOTLING_SQUISH = reg("entity.blotling.squish");
    public static final RegistryObject<SoundEvent> BLOTLING_HURT = reg("entity.blotling.hurt");
    public static final RegistryObject<SoundEvent> BLOTLING_DEATH = reg("entity.blotling.death");
    public static final RegistryObject<SoundEvent> SMUDGE_AMBIENT = reg("entity.smudge.ambient");
    public static final RegistryObject<SoundEvent> SMUDGE_HURT = reg("entity.smudge.hurt");
    public static final RegistryObject<SoundEvent> SMUDGE_DEATH = reg("entity.smudge.death");
    public static final RegistryObject<SoundEvent> CRAWLER_AMBIENT = reg("entity.margin_crawler.ambient");
    public static final RegistryObject<SoundEvent> CRAWLER_HURT = reg("entity.margin_crawler.hurt");
    public static final RegistryObject<SoundEvent> CRAWLER_DEATH = reg("entity.margin_crawler.death");
    public static final RegistryObject<SoundEvent> QUILLCROW_AMBIENT = reg("entity.quillcrow.ambient");
    public static final RegistryObject<SoundEvent> QUILLCROW_HURT = reg("entity.quillcrow.hurt");
    public static final RegistryObject<SoundEvent> QUILLCROW_DEATH = reg("entity.quillcrow.death");
    public static final RegistryObject<SoundEvent> QUILLCROW_FLAP = reg("entity.quillcrow.flap");
    public static final RegistryObject<SoundEvent> STAG_AMBIENT = reg("entity.pale_stag.ambient");
    public static final RegistryObject<SoundEvent> STAG_HURT = reg("entity.pale_stag.hurt");
    public static final RegistryObject<SoundEvent> STAG_DEATH = reg("entity.pale_stag.death");
    public static final RegistryObject<SoundEvent> INKHOUND_AMBIENT = reg("entity.inkhound.ambient");
    public static final RegistryObject<SoundEvent> INKHOUND_HOWL = reg("entity.inkhound.howl");
    public static final RegistryObject<SoundEvent> INKHOUND_HURT = reg("entity.inkhound.hurt");
    public static final RegistryObject<SoundEvent> INKHOUND_DEATH = reg("entity.inkhound.death");
    public static final RegistryObject<SoundEvent> MOTH_AMBIENT = reg("entity.foxing_moth.ambient");
    public static final RegistryObject<SoundEvent> RUBRICATOR_AMBIENT = reg("entity.rubricator.ambient");
    public static final RegistryObject<SoundEvent> RUBRICATOR_YES = reg("entity.rubricator.yes");
    public static final RegistryObject<SoundEvent> RUBRICATOR_NO = reg("entity.rubricator.no");
    public static final RegistryObject<SoundEvent> RUBRICATOR_HURT = reg("entity.rubricator.hurt");
    public static final RegistryObject<SoundEvent> RUBRICATOR_DEATH = reg("entity.rubricator.death");
    public static final RegistryObject<SoundEvent> KNOCKER_AMBIENT = reg("entity.knocker.ambient");
    public static final RegistryObject<SoundEvent> KNOCKER_LUNGE = reg("entity.knocker.lunge");
    public static final RegistryObject<SoundEvent> KNOCKER_HURT = reg("entity.knocker.hurt");
    public static final RegistryObject<SoundEvent> KNOCKER_DEATH = reg("entity.knocker.death");
    public static final RegistryObject<SoundEvent> COPYIST_REVEAL = reg("entity.copyist.reveal");
    public static final RegistryObject<SoundEvent> COPYIST_AMBIENT = reg("entity.copyist.ambient");
    public static final RegistryObject<SoundEvent> COPYIST_HURT = reg("entity.copyist.hurt");
    public static final RegistryObject<SoundEvent> COPYIST_DEATH = reg("entity.copyist.death");
    public static final RegistryObject<SoundEvent> LONGHAND_AMBIENT = reg("entity.longhand.ambient");
    public static final RegistryObject<SoundEvent> LONGHAND_MOVE = reg("entity.longhand.move");
    public static final RegistryObject<SoundEvent> LONGHAND_HURT = reg("entity.longhand.hurt");
    public static final RegistryObject<SoundEvent> LONGHAND_DEATH = reg("entity.longhand.death");
    public static final RegistryObject<SoundEvent> SQUEEZE_CRACK = reg("entity.squeeze.crack");
    public static final RegistryObject<SoundEvent> SQUEEZE_DRAG = reg("entity.squeeze.drag");
    public static final RegistryObject<SoundEvent> KNOCKER_TAP = reg("entity.knocker.tap");
    public static final RegistryObject<SoundEvent> KNOCKER_SCRATCH = reg("entity.knocker.scratch");
    public static final RegistryObject<SoundEvent> REDACTED_AMBIENT = reg("entity.redacted.ambient");
    public static final RegistryObject<SoundEvent> REDACTED_ATTACK = reg("entity.redacted.attack");
    public static final RegistryObject<SoundEvent> REDACTED_HURT = reg("entity.redacted.hurt");
    public static final RegistryObject<SoundEvent> REDACTED_DEATH = reg("entity.redacted.death");
    public static final RegistryObject<SoundEvent> ERRATUM_AMBIENT = reg("entity.erratum.ambient");
    public static final RegistryObject<SoundEvent> ERRATUM_REVEAL = reg("entity.erratum.reveal");
    public static final RegistryObject<SoundEvent> PALEHAND_AMBIENT = reg("entity.palehand.ambient");
    public static final RegistryObject<SoundEvent> FAIR_COPY_VANISH = reg("entity.fair_copy.vanish");
    public static final RegistryObject<SoundEvent> BOOKBINDER_AMBIENT = reg("entity.bookbinder.ambient");
    public static final RegistryObject<SoundEvent> BOOKBINDER_NEEDLE = reg("entity.bookbinder.needle");
    public static final RegistryObject<SoundEvent> BOOKBINDER_REBIND = reg("entity.bookbinder.rebind");
    public static final RegistryObject<SoundEvent> BOOKBINDER_HURT = reg("entity.bookbinder.hurt");
    public static final RegistryObject<SoundEvent> BOOKBINDER_DEATH = reg("entity.bookbinder.death");
    public static final RegistryObject<SoundEvent> RASURE_AMBIENT = reg("entity.rasure.ambient");
    public static final RegistryObject<SoundEvent> RASURE_SCRAPE = reg("entity.rasure.scrape");
    public static final RegistryObject<SoundEvent> RASURE_PHASE = reg("entity.rasure.phase");
    public static final RegistryObject<SoundEvent> RASURE_HURT = reg("entity.rasure.hurt");
    public static final RegistryObject<SoundEvent> RASURE_DEATH = reg("entity.rasure.death");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Palimpsest.id(name)));
    }

    private ModSounds() {}
}
