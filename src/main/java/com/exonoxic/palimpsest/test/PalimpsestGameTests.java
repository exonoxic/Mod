package com.exonoxic.palimpsest.test;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.bleed.BleedData;
import com.exonoxic.palimpsest.bleed.BleedStage;
import com.exonoxic.palimpsest.bleed.Ending;
import com.exonoxic.palimpsest.block.BlankBlock;
import com.exonoxic.palimpsest.entity.CopyistEntity;
import com.exonoxic.palimpsest.entity.KnockerEntity;
import com.exonoxic.palimpsest.entity.ai.Squeeze;
import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModEntities;
import com.exonoxic.palimpsest.registry.ModRecipes;
import com.exonoxic.palimpsest.world.dimension.ModDimensions;
import com.exonoxic.palimpsest.world.gate.FolioGate;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Server-side smoke tests, run by {@code ./gradlew runGameTestServer} (and in CI). They load the
 * whole mod on a dedicated server, generate real Undertext terrain, and poke the systems that
 * are easiest to break by editing data: worldgen JSON, templates, recipes, gate frames.
 */
@GameTestHolder(Palimpsest.MODID)
@PrefixGameTestTemplate(false)
public final class PalimpsestGameTests {
    private static final String EMPTY = "gametest/empty";
    private static final String YARD = "gametest/yard";
    private static final String[] TEMPLATES = {
            "wray_cabin/wray_cabin_0", "wray_cabin/wray_cabin_1", "wray_cabin/wray_cabin_2",
            "scraped_obelisk/scraped_obelisk_0", "scrap_shrine/scrap_shrine_0", "faded_village/faded_village_0",
            "faded_village/faded_village_1", "hollow_chapel/hollow_chapel", "copying_house/copying_house",
            "survey_station/survey_station", "crow_roost/crow_roost", "doubled_house/doubled_house",
            "broken_gate/broken_gate", "marginalia_spire/marginalia_spire", "ink_well/ink_well",
            "bindery/bindery", "last_folio/last_folio"};

    private PalimpsestGameTests() {}

    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    // ------------------------------------------------------------------ pure logic

    @GameTest(template = EMPTY)
    public static void bleedStages(GameTestHelper helper) {
        check(BleedStage.of(0F) == BleedStage.CLEAN_PAGE, "0 should be a clean page");
        check(BleedStage.of(49.9F) == BleedStage.CLEAN_PAGE, "49.9 should still be clean");
        check(BleedStage.of(50F) == BleedStage.FAINT_TRACE, "50 should be a faint trace");
        check(BleedStage.of(700F) == BleedStage.THE_TEAR, "700 should open the tear");
        check(BleedStage.of(BleedStage.MAX) == BleedStage.OVERWRITTEN, "max should be overwritten");
        check(BleedStage.byIndex(99) == BleedStage.OVERWRITTEN && BleedStage.byIndex(-3) == BleedStage.CLEAN_PAGE, "byIndex must clamp");

        BleedData data = new BleedData();
        data.setBleed(321F);
        data.setHighestStage(4);
        data.setEnding(Ending.SEALED);
        data.unlock("knocker");
        data.setFlag("visited:palimpsest:wray_cabin");
        CompoundTag tag = data.save();
        BleedData copy = new BleedData();
        copy.load(tag);
        check(Math.abs(copy.getBleed() - 321F) < 0.01F, "bleed did not survive a save/load");
        check(copy.getStage() == BleedStage.BLEED_THROUGH, "stage not derived from loaded bleed");
        check(copy.getHighestStage() == 4 && copy.getEnding() == Ending.SEALED, "progress lost on save/load");
        check(copy.getCodex().contains("knocker") && copy.hasFlag("visited:palimpsest:wray_cabin"), "codex/flags lost on save/load");
        helper.succeed();
    }

    // ------------------------------------------------------------------ data loading

    @GameTest(template = EMPTY)
    public static void dataLoads(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        long recipes = level.getRecipeManager().getRecipes().stream()
                .filter(r -> r.getId().getNamespace().equals(Palimpsest.MODID)).count();
        check(recipes >= 50, "only " + recipes + " palimpsest recipes loaded");
        check(!level.getRecipeManager().getAllRecipesFor(ModRecipes.RITUAL_TYPE.get()).isEmpty(), "no rituals loaded");
        check(!level.getRecipeManager().getAllRecipesFor(ModRecipes.TRANSCRIPTION_TYPE.get()).isEmpty(), "no transcriptions loaded");

        long advancements = server.getAdvancements().getAllAdvancements().stream()
                .filter(a -> a.getId().getNamespace().equals(Palimpsest.MODID)).count();
        check(advancements >= 20, "only " + advancements + " advancements loaded");

        for (String chest : new String[]{"chests/wray_cabin", "chests/survey_station", "chests/bindery", "entities/knocker"}) {
            LootTable table = server.getLootData().getLootTable(Palimpsest.id(chest));
            check(table != LootTable.EMPTY, "loot table " + chest + " missing");
        }
        for (String name : TEMPLATES) {
            Optional<StructureTemplate> template = level.getStructureManager().get(Palimpsest.id(name));
            check(template.isPresent() && template.get().getSize().getX() > 0, "structure template " + name + " missing or empty");
        }
        Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        long ours = structures.keySet().stream().filter(k -> k.getNamespace().equals(Palimpsest.MODID)).count();
        check(ours >= 14, "only " + ours + " structures registered");
        helper.succeed();
    }

    // ------------------------------------------------------------------ worldgen

    @GameTest(template = EMPTY, timeoutTicks = 2400)
    public static void undertextGenerates(GameTestHelper helper) {
        ServerLevel undertext = helper.getLevel().getServer().getLevel(ModDimensions.UNDERTEXT);
        check(undertext != null, "the Undertext dimension is not loaded");
        int[][] chunks = {{0, 0}, {9, -5}, {-30, 22}, {120, 80}, {-200, -260}, {400, 30}};
        Set<ResourceLocation> biomes = new HashSet<>();
        int columns = 0, solid = 0;
        for (int[] c : chunks) {
            LevelChunk chunk = undertext.getChunk(c[0], c[1]);
            for (int x = 1; x < 16; x += 5) {
                for (int z = 1; z < 16; z += 5) {
                    columns++;
                    int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                    if (top > undertext.getMinBuildHeight() + 1) solid++;
                    BlockPos probe = chunk.getPos().getWorldPosition().offset(x, Math.max(top, undertext.getMinBuildHeight()), z);
                    Holder<Biome> biome = undertext.getBiome(probe);
                    biome.unwrapKey().ifPresent(k -> biomes.add(k.location()));
                }
            }
        }
        check(solid > columns / 2, "most Undertext columns are empty (" + solid + "/" + columns + ")");
        check(!biomes.isEmpty(), "no biomes resolved");
        for (ResourceLocation b : biomes) check(b.getNamespace().equals(Palimpsest.MODID), "foreign biome in the Undertext: " + b);
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 2400)
    public static void undertextStructuresLocatable(GameTestHelper helper) {
        ServerLevel undertext = helper.getLevel().getServer().getLevel(ModDimensions.UNDERTEXT);
        check(undertext != null, "the Undertext dimension is not loaded");
        Registry<Structure> registry = undertext.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Optional<HolderSet.Named<Structure>> set = registry.getTag(TagKey.create(Registries.STRUCTURE, Palimpsest.id("margin_compass/undertext")));
        check(set.isPresent() && set.get().size() > 0, "margin_compass/undertext structure tag is empty");
        Pair<BlockPos, Holder<Structure>> found = undertext.getChunkSource().getGenerator()
                .findNearestMapStructure(undertext, set.get(), BlockPos.ZERO, 48, false);
        check(found != null, "no Undertext structure within 48 chunks of the origin");
        helper.succeed();
    }

    // ------------------------------------------------------------------ blocks

    @GameTest(template = EMPTY)
    public static void folioGateFrame(GameTestHelper helper) {
        BlockState frame = ModBlocks.RUBRICATED_VELLUM_BRICKS.get().defaultBlockState();
        for (int x = 2; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 1, 4), frame);
            helper.setBlock(new BlockPos(x, 5, 4), frame);
        }
        for (int y = 2; y <= 4; y++) {
            helper.setBlock(new BlockPos(2, y, 4), frame);
            helper.setBlock(new BlockPos(5, y, 4), frame);
        }
        ServerLevel level = helper.getLevel();
        FolioGate.Shape shape = FolioGate.find(level, helper.absolutePos(new BlockPos(2, 3, 4)));
        check(shape != null, "a complete frame was not recognised");
        check(shape.origin().equals(helper.absolutePos(new BlockPos(3, 2, 4))), "wrong gate origin " + shape.origin());
        check(shape.widthDir() == Direction.EAST, "wrong gate orientation " + shape.widthDir());
        FolioGate.fill(level, shape);
        helper.assertBlockPresent(ModBlocks.UNDERTEXT_VEIL.get(), new BlockPos(4, 4, 4));
        FolioGate.Shape back = FolioGate.fromVeil(level, helper.absolutePos(new BlockPos(4, 4, 4)));
        check(back.origin().equals(shape.origin()), "fromVeil walked to " + back.origin() + " instead of " + shape.origin());
        helper.setBlock(new BlockPos(5, 3, 4), Blocks.AIR);
        check(!FolioGate.valid(level, shape.origin(), shape.widthDir(), true), "a broken frame still counts as a gate");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void scrapedBlocksReturn(GameTestHelper helper) {
        BlockPos planks = new BlockPos(4, 1, 4);
        BlockPos chest = new BlockPos(2, 1, 2);
        helper.setBlock(planks, Blocks.OAK_PLANKS);
        helper.setBlock(chest, Blocks.CHEST);
        ServerLevel level = helper.getLevel();
        check(BlankBlock.scrape(level, helper.absolutePos(planks), 40, true), "scraping refused plain planks");
        helper.assertBlockPresent(ModBlocks.BLANK.get(), planks);
        check(!BlankBlock.scrape(level, helper.absolutePos(chest), 40, true), "scraping erased a block entity");
        check(!BlankBlock.scrape(level, helper.absolutePos(new BlockPos(6, 1, 6)), 40, true), "scraping accepted air");
        helper.runAfterDelay(90, () -> {
            helper.assertBlockPresent(Blocks.OAK_PLANKS, planks);
            helper.assertBlockPresent(Blocks.CHEST, chest);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void rubricChalkStopsInkborn(GameTestHelper helper) {
        BlockPos rel = new BlockPos(4, 0, 4);
        helper.setBlock(rel, ModBlocks.RUBRIC_CHALK.get());
        helper.assertBlockPresent(ModBlocks.RUBRIC_CHALK.get(), rel);
        BlockState chalk = helper.getBlockState(rel);
        Entity inkhound = helper.spawn(ModEntities.INKHOUND.get(), new BlockPos(1, 1, 1));
        Entity cow = helper.spawn(EntityType.COW, new BlockPos(7, 1, 7));
        BlockPos abs = helper.absolutePos(rel);
        VoxelShape forInk = chalk.getCollisionShape(helper.getLevel(), abs, CollisionContext.of(inkhound));
        VoxelShape forCow = chalk.getCollisionShape(helper.getLevel(), abs, CollisionContext.of(cow));
        check(!forInk.isEmpty() && forInk.max(Direction.Axis.Y) > 1.0, "inkborn can step over rubric chalk");
        check(forCow.isEmpty(), "rubric chalk blocks ordinary animals");
        helper.succeed();
    }

    // ------------------------------------------------------------------ squeezing through gaps

    /**
     * A sealed tube one block wide: open, then two blocks high, then one block high, then open.
     * A Copyist in its true form (2.2 blocks tall) must get to the far end, stooping and then
     * crawling on the way, without hurting itself.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void copyistSqueezesThroughGaps(GameTestHelper helper) {
        for (int x = 0; x <= 8; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = 3; z <= 5; z++) {
                    boolean inside = z == 4 && y >= 1 && y <= 3 && x >= 1 && x <= 7;
                    helper.setBlock(new BlockPos(x, y, z), inside ? Blocks.AIR : Blocks.STONE);
                }
            }
        }
        helper.setBlock(new BlockPos(3, 3, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 2, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 3, 4), Blocks.STONE);
        CopyistEntity copyist = helper.spawn(ModEntities.COPYIST.get(), new BlockPos(1, 1, 4));
        check(copyist.isRevealed(), "a Copyist spawned without a disguise should be in its true form");
        BlockPos goal = helper.absolutePos(new BlockPos(7, 1, 4));
        float health = copyist.getHealth();
        Set<Pose> seen = new HashSet<>();
        helper.succeedWhen(() -> {
            seen.add(copyist.getPose());
            if (copyist.getNavigation().isDone()) {
                // Accuracy 0: all the way to the last block (moveTo(x, y, z) stops within a block of it).
                copyist.getNavigation().moveTo(copyist.getNavigation().createPath(goal, 0), 1.0D);
            }
            check(copyist.getHealth() >= health, "the Copyist hurt itself squeezing through");
            check(copyist.getX() > goal.getX(), "the Copyist has not got through yet (at x " + (copyist.getX() - helper.absolutePos(BlockPos.ZERO).getX())
                    + ", poses seen " + seen + ")");
            check(seen.contains(Squeeze.STOOP), "the Copyist never stooped under the two-high section");
            check(seen.contains(Squeeze.CRAWL), "the Copyist never crawled through the one-high section");
        });
    }

    /** Knockers (2.7 blocks tall) put into sealed spaces one and two blocks high fold to fit instead of suffocating. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void knockerFoldsToFit(GameTestHelper helper) {
        BlockPos lowCell = new BlockPos(2, 1, 4);
        BlockPos midCell = new BlockPos(6, 1, 4);
        sealedCell(helper, lowCell, 1);
        sealedCell(helper, midCell, 2);
        KnockerEntity low = helper.spawn(ModEntities.KNOCKER.get(), lowCell);
        KnockerEntity mid = helper.spawn(ModEntities.KNOCKER.get(), midCell);
        helper.runAfterDelay(10, () -> {
            check(low.getPose() == Squeeze.CRAWL && low.getBbHeight() < 1.0F,
                    "a Knocker in a one-high space should crawl (pose " + low.getPose() + ", height " + low.getBbHeight() + ")");
            check(mid.getPose() == Squeeze.STOOP && mid.getBbHeight() < 2.0F,
                    "a Knocker in a two-high space should stoop (pose " + mid.getPose() + ", height " + mid.getBbHeight() + ")");
            check(low.getHealth() >= low.getMaxHealth() && mid.getHealth() >= mid.getMaxHealth(), "a folded Knocker should not suffocate");
            helper.succeed();
        });
    }

    /**
     * A room three blocks high with a one-block hole in its end wall, one block up off the floor. A
     * Copyist has to climb up into it (the pathfinder only allows that because it plans at crawling
     * height) and come out in the room beyond.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void squeezersClimbIntoRaisedHoles(GameTestHelper helper) {
        for (int x = 0; x <= 8; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = 3; z <= 5; z++) {
                    boolean room = z == 4 && y >= 1 && y <= 3 && ((x >= 1 && x <= 3) || (x >= 5 && x <= 7));
                    boolean hole = x == 4 && y == 2 && z == 4;
                    helper.setBlock(new BlockPos(x, y, z), room || hole ? Blocks.AIR : Blocks.STONE);
                }
            }
        }
        CopyistEntity copyist = helper.spawn(ModEntities.COPYIST.get(), new BlockPos(1, 1, 4));
        BlockPos goal = helper.absolutePos(new BlockPos(7, 1, 4));
        float health = copyist.getHealth();
        Set<Pose> seen = new HashSet<>();
        helper.succeedWhen(() -> {
            seen.add(copyist.getPose());
            if (copyist.getNavigation().isDone()) copyist.getNavigation().moveTo(copyist.getNavigation().createPath(goal, 0), 1.0D);
            check(copyist.getHealth() >= health, "the Copyist hurt itself climbing into the hole");
            check(copyist.getX() > goal.getX(), "the Copyist has not climbed through yet (at x " + (copyist.getX() - helper.absolutePos(BlockPos.ZERO).getX())
                    + ", poses seen " + seen + ")");
            check(seen.contains(Squeeze.CRAWL), "the Copyist never crawled into the hole");
        });
    }

    /**
     * What a Knocker checks for when nobody answers: is there any way in? Penned outside a hut, it
     * finds none while the hut is sealed, and finds one through a one-block gap at floor level and
     * through one a step up the wall. Each question is asked afresh, never answered from the last route.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void knockerFindsAWayIn(GameTestHelper helper) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        // The hut: stone walls x 3..7, z 2..6, three high, roofed; its inside is x 4..6, z 3..5.
        for (int x = 3; x <= 7; x++) {
            for (int z = 2; z <= 6; z++) {
                for (int y = 1; y <= 4; y++) {
                    boolean inside = x >= 4 && x <= 6 && z >= 3 && z <= 5 && y <= 3;
                    helper.setBlock(new BlockPos(x, y, z), inside ? Blocks.AIR : Blocks.STONE);
                }
            }
        }
        // A pen against its west wall, open to the sky, so the Knocker cannot wander off.
        for (int y = 1; y <= 3; y++) {
            for (int z = 2; z <= 6; z++) helper.setBlock(new BlockPos(0, y, z), Blocks.STONE);
            for (int x = 1; x <= 2; x++) {
                helper.setBlock(new BlockPos(x, y, 2), Blocks.STONE);
                helper.setBlock(new BlockPos(x, y, 6), Blocks.STONE);
            }
        }
        KnockerEntity knocker = helper.spawn(ModEntities.KNOCKER.get(), new BlockPos(1, 1, 4));
        BlockPos inside = helper.absolutePos(new BlockPos(5, 1, 4));
        helper.runAfterDelay(10, () -> {
            check(knocker.isAlive(), "the Knocker vanished before it could be tested");
            Path sealed = knocker.wayIn(inside);
            check(sealed == null || !sealed.canReach(), "the Knocker found a way into a sealed hut");
            helper.setBlock(new BlockPos(3, 1, 4), Blocks.AIR);
            Path low = knocker.wayIn(inside);
            check(low != null && low.canReach(), "the Knocker found no way in through a one-block gap at floor level");
            helper.setBlock(new BlockPos(3, 1, 4), Blocks.STONE);
            helper.setBlock(new BlockPos(3, 2, 4), Blocks.AIR);
            Path raised = knocker.wayIn(inside);
            check(raised != null && raised.canReach(), "the Knocker found no way in through a one-block gap a step up the wall");
            helper.succeed();
        });
    }

    /**
     * The smoke test's hut: a Knocker at the (shut) door has to walk all the way round to a gap in
     * the far wall. Its route has to go round the house, not just through the nearest wall.
     */
    @GameTest(template = YARD, timeoutTicks = 100)
    public static void knockerGoesRoundTheHouse(GameTestHelper helper) {
        for (int x = 0; x <= 16; x++) {
            for (int z = 0; z <= 16; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        // Walls x 5..11, z 5..11, three high and roofed; the inside is x 6..10, z 6..10.
        for (int x = 5; x <= 11; x++) {
            for (int z = 5; z <= 11; z++) {
                for (int y = 1; y <= 4; y++) {
                    boolean inside = x >= 6 && x <= 10 && z >= 6 && z <= 10 && y <= 3;
                    helper.setBlock(new BlockPos(x, y, z), inside ? Blocks.AIR : Blocks.STONE_BRICKS);
                }
            }
        }
        BlockState door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        helper.setBlock(new BlockPos(8, 1, 11), door);
        helper.setBlock(new BlockPos(8, 2, 11), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        helper.setBlock(new BlockPos(9, 1, 9), Blocks.TORCH);
        KnockerEntity knocker = helper.spawn(ModEntities.KNOCKER.get(), new BlockPos(8, 1, 12));
        BlockPos inside = helper.absolutePos(new BlockPos(8, 1, 9));
        helper.runAfterDelay(10, () -> {
            check(knocker.isAlive(), "the Knocker vanished before it could be tested");
            Path sealed = knocker.wayIn(inside);
            check(sealed == null || !sealed.canReach(), "the Knocker found a way into a sealed hut: " + describe(helper, sealed));
            helper.setBlock(new BlockPos(8, 1, 5), Blocks.AIR);
            Path round = knocker.wayIn(inside);
            check(round != null && round.canReach(), "the Knocker found no way round to the gap in the far wall (on ground "
                    + knocker.onGround() + "): " + describe(helper, round));
            helper.succeed();
        });
    }

    private static String describe(GameTestHelper helper, Path path) {
        if (path == null) return "no path at all";
        StringBuilder out = new StringBuilder(path.getNodeCount() + " nodes, reaches " + path.canReach() + ":");
        for (int i = 0; i < path.getNodeCount(); i++) out.append(' ').append(helper.relativePos(path.getNodePos(i)).toShortString());
        return out.toString();
    }

    /** Stone all round a one-block-wide space of the given height, whose floor-level block is {@code inside}. */
    private static void sealedCell(GameTestHelper helper, BlockPos inside, int height) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= height; dy++) {
                    boolean air = dx == 0 && dz == 0 && dy >= 0 && dy < height;
                    helper.setBlock(inside.offset(dx, dy, dz), air ? Blocks.AIR : Blocks.STONE);
                }
            }
        }
    }
}
