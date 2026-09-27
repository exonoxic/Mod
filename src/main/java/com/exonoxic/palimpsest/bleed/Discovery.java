package com.exonoxic.palimpsest.bleed;

import com.exonoxic.palimpsest.codex.CodexEntries;
import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModItems;
import com.exonoxic.palimpsest.util.Advancements;
import com.exonoxic.palimpsest.world.ModStructures;
import com.exonoxic.palimpsest.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.HashMap;
import java.util.Map;

/**
 * Notices what a player has found — places, creatures, objects — and writes it into their
 * Commonplace Book. Runs every five seconds per player; each check is cheap and bounded.
 */
public final class Discovery {
    private static Map<Item, String> itemEntries;

    public static void tick(ServerPlayer player, BleedData data) {
        checkStructures(player);
        checkCreatures(player);
        checkInventory(player, data);
    }

    private static void checkStructures(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        if (level.structureManager().getAllStructuresAt(pos).isEmpty()) return;
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (ResourceKey<Structure> key : ModStructures.ALL) {
            Structure structure = registry.get(key);
            if (structure == null) continue;
            StructureStart start = level.structureManager().getStructureWithPieceAt(pos, structure);
            if (!start.isValid()) continue;
            String name = key.location().getPath();
            boolean undertext = level.dimension() == ModDimensions.UNDERTEXT;
            if (BleedManager.addOnce(player, "visited:" + name, undertext ? 5F : 8F)) {
                if (CodexEntries.get(name) != null) BleedManager.unlock(player, name);
                switch (name) {
                    case "survey_station" -> Advancements.grant(player, "station_four");
                    case "doubled_house" -> Advancements.grant(player, "written_twice");
                    case "last_folio" -> Advancements.grant(player, "the_last_folio");
                    case "bindery" -> Advancements.grant(player, "the_bindery");
                    default -> {}
                }
            }
        }
    }

    private static void checkCreatures(ServerPlayer player) {
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(20),
                m -> CodexEntries.forEntity(m.getType()) != null && !m.isInvisible())) {
            String id = CodexEntries.forEntity(mob.getType());
            if (id == null || BleedCapability.get(player).getCodex().contains(id)) continue;
            if (!player.hasLineOfSight(mob)) continue;
            BleedManager.unlock(player, id);
            BleedManager.addOnce(player, "saw:" + id, 3F);
            switch (id) {
                case "fair_copy" -> Advancements.grant(player, "which_one");
                case "erratum" -> Advancements.grant(player, "that_wasnt_there");
                case "rubricator" -> Advancements.grant(player, "the_rubricator");
                default -> {}
            }
        }
    }

    private static void checkInventory(ServerPlayer player, BleedData data) {
        Map<Item, String> map = itemEntries();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            String entry = map.get(stack.getItem());
            if (entry != null && !data.getCodex().contains(entry)) BleedManager.unlock(player, entry);
        }
    }

    private static Map<Item, String> itemEntries() {
        if (itemEntries == null) {
            Map<Item, String> m = new HashMap<>();
            m.put(ModBlocks.PALIMPSEST_STONE.get().asItem(), "palimpsest_stone");
            m.put(ModBlocks.DEEPSLATE_PALIMPSEST_STONE.get().asItem(), "palimpsest_stone");
            m.put(ModItems.UNDERTEXT_FRAGMENT.get(), "palimpsest_stone");
            m.put(ModItems.IRON_RASORIUM.get(), "rasorium");
            m.put(ModItems.OAK_GALL.get(), "iron_gall_ink");
            m.put(ModItems.IRON_GALL_INK.get(), "iron_gall_ink");
            m.put(ModItems.CINNABAR.get(), "vermilion");
            m.put(ModItems.VERMILION.get(), "vermilion");
            m.put(ModBlocks.SCRIPTORIUM_DESK.get().asItem(), "scriptorium_desk");
            m.put(ModBlocks.RUBRIC_ALTAR.get().asItem(), "rites");
            m.put(ModBlocks.READING_STAND.get().asItem(), "rites");
            m.put(ModBlocks.RUBRIC_WARD.get().asItem(), "rubric_ward");
            m.put(ModItems.RUBRIC_CHALK.get(), "rubric_chalk");
            m.put(ModItems.FOLIO_OF_DESCENT.get(), "folio_gate");
            m.put(ModBlocks.RUBRICATED_VELLUM_BRICKS.get().asItem(), "folio_gate");
            m.put(ModItems.BOOKMARK.get(), "bookmark");
            m.put(ModItems.READING_LENS.get(), "reading_lens");
            m.put(ModItems.MARGIN_COMPASS.get(), "margin_compass");
            m.put(ModItems.GALL_STEEPED_IRON.get(), "gall_iron");
            m.put(ModItems.GALL_IRON_INGOT.get(), "gall_iron");
            m.put(ModItems.RAW_ILLUMINE.get(), "illumine");
            m.put(ModItems.ILLUMINE_LEAF.get(), "illumine");
            m.put(ModItems.CANDLEWORT_TEA.get(), "remedies");
            m.put(ModItems.SEALING_WAX.get(), "remedies");
            m.put(ModBlocks.CANDLEWORT.get().asItem(), "remedies");
            m.put(ModItems.SEAL_OF_CLOSING.get(), "seals");
            m.put(ModItems.FADED_PAGE.get(), "faded_pages");
            m.put(ModItems.LAST_FOLIO.get(), "the_choice");
            itemEntries = m;
        }
        return itemEntries;
    }

    private Discovery() {}
}
