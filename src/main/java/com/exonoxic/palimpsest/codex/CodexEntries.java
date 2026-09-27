package com.exonoxic.palimpsest.codex;

import com.exonoxic.palimpsest.codex.CodexEntry.Category;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/** The complete, ordered table of contents of the Commonplace Book. */
public final class CodexEntries {
    private static final Map<String, CodexEntry> BY_ID = new LinkedHashMap<>();
    public static final int FOLIO_COUNT = 12;
    public static final int FADED_PAGE_COUNT = 10;

    static {
        for (int i = 1; i <= 6; i++) add("stage_" + i, Category.THE_PAGE, "palimpsest:blank_vellum", true);

        add("footsteps", Category.OBSERVATIONS, "minecraft:leather_boots", false);
        add("whispers", Category.OBSERVATIONS, "palimpsest:faded_page", false);
        add("snuffed_torch", Category.OBSERVATIONS, "palimpsest:spent_torch", false);
        add("the_sign", Category.OBSERVATIONS, "minecraft:oak_sign", false);
        add("knocking", Category.OBSERVATIONS, "minecraft:oak_door", false);
        add("moved_things", Category.OBSERVATIONS, "minecraft:chest", false);
        add("watchers", Category.OBSERVATIONS, "palimpsest:black_quill", false);
        add("fog", Category.OBSERVATIONS, "minecraft:gray_dye", false);
        add("the_other_page", Category.OBSERVATIONS, "palimpsest:ruled_vellum", false);
        add("silence", Category.OBSERVATIONS, "minecraft:note_block", false);
        add("ink_rain", Category.OBSERVATIONS, "palimpsest:iron_gall_ink", false);
        add("someone_typed", Category.OBSERVATIONS, "minecraft:writable_book", false);
        add("fading", Category.OBSERVATIONS, "palimpsest:faded_grass_block", false);
        add("tears", Category.OBSERVATIONS, "palimpsest:seal_of_closing", false);
        add("scraping", Category.OBSERVATIONS, "palimpsest:rasure_shard", false);

        add("foxing_moth", Category.CREATURES, "palimpsest:foxed_dust", false);
        add("blotling", Category.CREATURES, "palimpsest:blot_residue", false);
        add("smudge", Category.CREATURES, "palimpsest:faded_page", false);
        add("margin_crawler", Category.CREATURES, "palimpsest:vellum_scrap", false);
        add("quillcrow", Category.CREATURES, "palimpsest:black_quill", false);
        add("pale_stag", Category.CREATURES, "palimpsest:pale_antler", false);
        add("inkhound", Category.CREATURES, "minecraft:bone", false);
        add("rubricator", Category.CREATURES, "palimpsest:vermilion", false);
        add("knocker", Category.CREATURES, "minecraft:oak_door", false);
        add("copyist", Category.CREATURES, "minecraft:leather", false);
        add("longhand", Category.CREATURES, "minecraft:ender_eye", false);
        add("redacted", Category.CREATURES, "palimpsest:redaction_strip", false);
        add("fair_copy", Category.CREATURES, "minecraft:player_head", true);
        add("erratum", Category.CREATURES, "palimpsest:misprint", true);
        add("palehand", Category.CREATURES, "minecraft:bone_block", true);
        add("bookbinder", Category.CREATURES, "palimpsest:bookbinder_needle", false);
        add("rasure", Category.CREATURES, "palimpsest:rasure_shard", false);

        add("wray_cabin", Category.PLACES, "minecraft:spruce_door", false);
        add("scraped_obelisk", Category.PLACES, "palimpsest:scraped_stone", false);
        add("hollow_chapel", Category.PLACES, "minecraft:bell", false);
        add("copying_house", Category.PLACES, "palimpsest:scriptorium_desk", false);
        add("survey_station", Category.PLACES, "minecraft:iron_door", false);
        add("crow_roost", Category.PLACES, "palimpsest:black_quill", false);
        add("doubled_house", Category.PLACES, "minecraft:oak_planks", true);
        add("broken_gate", Category.PLACES, "palimpsest:rubricated_vellum_bricks", false);
        add("undertext", Category.PLACES, "palimpsest:ruled_vellum", false);
        add("faded_village", Category.PLACES, "palimpsest:erased_grass", false);
        add("marginalia_spire", Category.PLACES, "palimpsest:scrawl", false);
        add("ink_well", Category.PLACES, "palimpsest:inkstone_bricks", false);
        add("bindery", Category.PLACES, "palimpsest:binding_thread", false);
        add("last_folio", Category.PLACES, "palimpsest:folio_stand", false);

        add("palimpsest_stone", Category.CRAFT, "palimpsest:palimpsest_stone", false);
        add("rasorium", Category.CRAFT, "palimpsest:iron_rasorium", false);
        add("iron_gall_ink", Category.CRAFT, "palimpsest:iron_gall_ink", false);
        add("vermilion", Category.CRAFT, "palimpsest:vermilion", false);
        add("scriptorium_desk", Category.CRAFT, "palimpsest:scriptorium_desk", false);
        add("rites", Category.CRAFT, "palimpsest:rubric_altar", false);
        add("rubric_ward", Category.CRAFT, "palimpsest:rubric_ward", false);
        add("rubric_chalk", Category.CRAFT, "palimpsest:rubric_chalk", false);
        add("folio_gate", Category.CRAFT, "palimpsest:folio_of_descent", false);
        add("bookmark", Category.CRAFT, "palimpsest:bookmark", false);
        add("reading_lens", Category.CRAFT, "palimpsest:reading_lens", false);
        add("margin_compass", Category.CRAFT, "palimpsest:margin_compass", false);
        add("gall_iron", Category.CRAFT, "palimpsest:gall_iron_ingot", false);
        add("illumine", Category.CRAFT, "palimpsest:illumine_leaf", false);
        add("remedies", Category.CRAFT, "palimpsest:candlewort_tea", false);
        add("seals", Category.CRAFT, "palimpsest:seal_of_closing", false);

        for (int i = 1; i <= FOLIO_COUNT; i++) add("folio_" + i, Category.FOLIOS, "palimpsest:torn_folio", false);
        add("faded_pages", Category.FOLIOS, "palimpsest:faded_page", false);
        add("the_choice", Category.FOLIOS, "palimpsest:last_folio", false);
    }

    private static void add(String id, Category category, String icon, boolean silent) {
        BY_ID.put(id, new CodexEntry(id, category, icon, silent));
    }

    @Nullable
    public static CodexEntry get(String id) {
        return BY_ID.get(id);
    }

    public static Collection<CodexEntry> all() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    public static List<CodexEntry> inCategory(Category category) {
        List<CodexEntry> list = new ArrayList<>();
        for (CodexEntry e : BY_ID.values()) if (e.category() == category) list.add(e);
        return list;
    }

    /** Creature entries are keyed by entity registry path. */
    @Nullable
    public static String forEntity(EntityType<?> type) {
        var key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (key == null || !key.getNamespace().equals("palimpsest")) return null;
        return BY_ID.containsKey(key.getPath()) ? key.getPath() : null;
    }

    private CodexEntries() {}
}
