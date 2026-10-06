package com.exonoxic.palimpsest.registry;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.block.*;
import com.exonoxic.palimpsest.world.feature.BlotwoodTreeGrower;
import net.minecraft.core.BlockPos;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Palimpsest.MODID);

    // ------------------------------------------------------------------ Overworld traces
    public static final RegistryObject<Block> PALIMPSEST_STONE = withItem("palimpsest_stone",
            () -> new PalimpsestStoneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F)));
    public static final RegistryObject<Block> DEEPSLATE_PALIMPSEST_STONE = withItem("deepslate_palimpsest_stone",
            () -> new PalimpsestStoneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE)
                    .instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)));
    public static final RegistryObject<Block> SCRAPED_STONE = withItem("scraped_stone",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE)
                    .instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F).sound(SoundType.CALCITE)));
    public static final RegistryObject<Block> CINNABAR_ORE = withItem("cinnabar_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F), UniformInt.of(1, 3)));
    public static final RegistryObject<Block> DEEPSLATE_CINNABAR_ORE = withItem("deepslate_cinnabar_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE)
                    .instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE), UniformInt.of(1, 3)));
    public static final RegistryObject<Block> CANDLEWORT = withItem("candlewort",
            () -> new CandlewortBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).noCollission().instabreak()
                    .sound(SoundType.GRASS).lightLevel(s -> 5).offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> FADED_GRASS_BLOCK = withItem("faded_grass_block",
            () -> new FadedGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_LIGHT_GRAY).randomTicks().strength(0.6F).sound(SoundType.GRASS)));
    public static final RegistryObject<Block> SPENT_TORCH = BLOCKS.register("spent_torch",
            () -> new SpentTorchBlock(BlockBehaviour.Properties.of().noCollission().instabreak().sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> SPENT_WALL_TORCH = BLOCKS.register("spent_wall_torch",
            () -> new SpentWallTorchBlock(BlockBehaviour.Properties.of().noCollission().instabreak().sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> TEAR = BLOCKS.register("tear",
            () -> new TearBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).noCollission().strength(-1.0F, 3600000.0F)
                    .lightLevel(s -> 3).noLootTable().pushReaction(PushReaction.BLOCK).sound(SoundType.WOOL)));

    // ------------------------------------------------------------------ Rubrication & craft
    public static final RegistryObject<Block> RUBRIC_CHALK = BLOCKS.register("rubric_chalk",
            () -> new RubricChalkBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instabreak().noOcclusion()
                    .sound(SoundType.CALCITE).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> RUBRIC_WARD = withItem("rubric_ward",
            () -> new RubricWardBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F, 6.0F)
                    .sound(SoundType.LANTERN).lightLevel(s -> 14).noOcclusion().requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SCRIPTORIUM_DESK = withItem("scriptorium_desk",
            () -> new ScriptoriumDeskBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final RegistryObject<Block> RUBRIC_ALTAR = withItem("rubric_altar",
            () -> new RubricAltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_RED).strength(3.5F, 9.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion().lightLevel(s -> s.getValue(RubricAltarBlock.ACTIVE) ? 9 : 2)));
    public static final RegistryObject<Block> READING_STAND = withItem("reading_stand",
            () -> new ReadingStandBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));

    public static final RegistryObject<Block> VELLUM_BRICKS = withItem("vellum_bricks",
            () -> new Block(vellumBrick()));
    public static final RegistryObject<Block> VELLUM_BRICK_STAIRS = withItem("vellum_brick_stairs",
            () -> new StairBlock(() -> ModBlocks.VELLUM_BRICKS.get().defaultBlockState(), vellumBrick()));
    public static final RegistryObject<Block> VELLUM_BRICK_SLAB = withItem("vellum_brick_slab",
            () -> new SlabBlock(vellumBrick()));
    public static final RegistryObject<Block> VELLUM_BRICK_WALL = withItem("vellum_brick_wall",
            () -> new WallBlock(vellumBrick().forceSolidOn()));
    public static final RegistryObject<Block> RUBRICATED_VELLUM_BRICKS = withItem("rubricated_vellum_bricks",
            () -> new Block(vellumBrick().mapColor(MapColor.TERRACOTTA_RED)));
    public static final RegistryObject<Block> SCRAPED_VELLUM_BRICKS = withItem("scraped_vellum_bricks",
            () -> new Block(vellumBrick()));
    public static final RegistryObject<Block> UNDERTEXT_VEIL = BLOCKS.register("undertext_veil",
            () -> new UndertextVeilBlock(BlockBehaviour.Properties.of().noCollission().strength(-1.0F).sound(SoundType.WOOL)
                    .lightLevel(s -> 8).noLootTable().pushReaction(PushReaction.BLOCK)));
    public static final RegistryObject<Block> GALL_IRON_BLOCK = withItem("gall_iron_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL)));
    public static final RegistryObject<Block> ILLUMINE_BLOCK = withItem("illumine_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).requiresCorrectToolForDrops().strength(4.0F, 6.0F)
                    .sound(SoundType.METAL).lightLevel(s -> 10)));

    // ------------------------------------------------------------------ The Undertext
    public static final RegistryObject<Block> RULED_VELLUM = withItem("ruled_vellum",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.6F).sound(SoundType.WOOL)));
    public static final RegistryObject<Block> VELLUM_SOIL = withItem("vellum_soil",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(0.5F).sound(SoundType.ROOTED_DIRT)));
    public static final RegistryObject<Block> INKSTONE = withItem("inkstone", () -> new Block(inkstone()));
    public static final RegistryObject<Block> COBBLED_INKSTONE = withItem("cobbled_inkstone", () -> new Block(inkstone().strength(2.0F, 6.0F)));
    public static final RegistryObject<Block> POLISHED_INKSTONE = withItem("polished_inkstone", () -> new Block(inkstone()));
    public static final RegistryObject<Block> INKSTONE_BRICKS = withItem("inkstone_bricks", () -> new Block(inkstone()));
    public static final RegistryObject<Block> INKSTONE_BRICK_STAIRS = withItem("inkstone_brick_stairs",
            () -> new StairBlock(() -> ModBlocks.INKSTONE_BRICKS.get().defaultBlockState(), inkstone()));
    public static final RegistryObject<Block> INKSTONE_BRICK_SLAB = withItem("inkstone_brick_slab", () -> new SlabBlock(inkstone()));
    public static final RegistryObject<Block> INKSTONE_BRICK_WALL = withItem("inkstone_brick_wall", () -> new WallBlock(inkstone().forceSolidOn()));
    public static final RegistryObject<Block> ILLUMINE_ORE = withItem("illumine_ore",
            () -> new DropExperienceBlock(inkstone().strength(4.0F, 4.0F).lightLevel(s -> 4), UniformInt.of(2, 5)));

    public static final RegistryObject<Block> BLOTWOOD_LOG = withItem("blotwood_log", () -> new BlotwoodLogBlock(blotwood()));
    public static final RegistryObject<Block> STRIPPED_BLOTWOOD_LOG = withItem("stripped_blotwood_log", () -> new RotatedPillarBlock(blotwood()));
    public static final RegistryObject<Block> BLOTWOOD_PLANKS = withItem("blotwood_planks", () -> new Block(blotwood().strength(2.0F, 3.0F)));
    public static final RegistryObject<Block> BLOTWOOD_STAIRS = withItem("blotwood_stairs",
            () -> new StairBlock(() -> ModBlocks.BLOTWOOD_PLANKS.get().defaultBlockState(), blotwood().strength(2.0F, 3.0F)));
    public static final RegistryObject<Block> BLOTWOOD_SLAB = withItem("blotwood_slab", () -> new SlabBlock(blotwood().strength(2.0F, 3.0F)));
    public static final RegistryObject<Block> BLOTWOOD_FENCE = withItem("blotwood_fence", () -> new FenceBlock(blotwood().strength(2.0F, 3.0F)));
    public static final RegistryObject<Block> BLOTWOOD_DOOR = BLOCKS.register("blotwood_door",
            () -> new DoorBlock(BlockBehaviour.Properties.copy(Blocks.DARK_OAK_DOOR).mapColor(MapColor.COLOR_BLACK), BlockSetType.DARK_OAK));
    public static final RegistryObject<Block> BLOTWOOD_TRAPDOOR = withItem("blotwood_trapdoor",
            () -> new TrapDoorBlock(BlockBehaviour.Properties.copy(Blocks.DARK_OAK_TRAPDOOR).mapColor(MapColor.COLOR_BLACK), BlockSetType.DARK_OAK));
    public static final RegistryObject<Block> BLOTWOOD_LEAVES = withItem("blotwood_leaves",
            () -> new BlotwoodLeavesBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.2F).randomTicks()
                    .sound(SoundType.AZALEA_LEAVES).noOcclusion().isValidSpawn(ModBlocks::never).isSuffocating(ModBlocks::never)
                    .isViewBlocking(ModBlocks::never).ignitedByLava().pushReaction(PushReaction.DESTROY).isRedstoneConductor(ModBlocks::never)));
    public static final RegistryObject<Block> BLOTWOOD_SAPLING = withItem("blotwood_sapling",
            () -> new SaplingBlock(new BlotwoodTreeGrower(), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .noCollission().randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> BLOTBERRY_BUSH = BLOCKS.register("blotberry_bush",
            () -> new BlotberryBushBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).randomTicks().noCollission()
                    .sound(SoundType.SWEET_BERRY_BUSH).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> ERASED_GRASS = withItem("erased_grass",
            () -> new ErasedGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).replaceable().noCollission().instabreak()
                    .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XYZ).ignitedByLava().pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> SCRAWL = withItem("scrawl",
            () -> new ScrawlBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).replaceable().noCollission()
                    .strength(0.1F).sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> BINDING_THREAD = withItem("binding_thread",
            () -> new WebBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).forceSolidOn().noCollission()
                    .requiresCorrectToolForDrops().strength(3.0F).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> ILLUMINED_LANTERN = withItem("illumined_lantern",
            () -> new LanternBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).forceSolidOn().requiresCorrectToolForDrops()
                    .strength(3.5F).sound(SoundType.LANTERN).lightLevel(s -> 15).noOcclusion().pushReaction(PushReaction.DESTROY)));

    // ------------------------------------------------------------------ Dungeon & endgame
    public static final RegistryObject<Block> SEALED_DOOR = withItem("sealed_door",
            () -> new SealedDoorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLACK).strength(-1.0F, 3600000.0F)
                    .noLootTable().sound(SoundType.WOOD).pushReaction(PushReaction.BLOCK)));
    public static final RegistryObject<Block> FOLIO_STAND = withItem("folio_stand",
            () -> new FolioStandBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(-1.0F, 3600000.0F)
                    .noLootTable().noOcclusion().lightLevel(s -> 7).pushReaction(PushReaction.BLOCK)));
    public static final RegistryObject<Block> RUBRIC_PILLAR = withItem("rubric_pillar",
            () -> new RubricPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(-1.0F, 3600000.0F)
                    .noLootTable().lightLevel(s -> s.getValue(RubricPillarBlock.LIT) ? 12 : 0).pushReaction(PushReaction.BLOCK)));
    public static final RegistryObject<Block> STITCHED_TOME = withItem("stitched_tome",
            () -> new StitchedTomeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).strength(1.2F)
                    .noLootTable().sound(SoundType.WOOL).noOcclusion()));
    public static final RegistryObject<Block> BLANK = BLOCKS.register("blank",
            () -> new BlankBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(-1.0F, 3600000.0F)
                    .noLootTable().noOcclusion().sound(SoundType.WOOL).pushReaction(PushReaction.BLOCK).isValidSpawn(ModBlocks::never)
                    .isRedstoneConductor(ModBlocks::never).isSuffocating(ModBlocks::never).isViewBlocking(ModBlocks::never)));

    // ------------------------------------------------------------------ helpers
    private static BlockBehaviour.Properties vellumBrick() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(1.8F, 6.0F).sound(SoundType.PACKED_MUD);
    }

    private static BlockBehaviour.Properties inkstone() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(1.8F, 6.0F).sound(SoundType.DEEPSLATE_BRICKS);
    }

    private static BlockBehaviour.Properties blotwood() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASS)
                .strength(2.0F).sound(SoundType.WOOD).ignitedByLava();
    }

    private static boolean never(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    private static Boolean never(BlockState state, BlockGetter level, BlockPos pos, EntityType<?> type) {
        return false;
    }

    private static <T extends Block> RegistryObject<T> withItem(String name, Supplier<T> block) {
        RegistryObject<T> ro = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> new BlockItem(ro.get(), new Item.Properties()));
        return ro;
    }

    /** Called during common setup, once blocks exist. */
    public static void registerFlammability() {
        FireBlock fire = (FireBlock) Blocks.FIRE;
        fire.setFlammable(BLOTWOOD_LOG.get(), 5, 5);
        fire.setFlammable(STRIPPED_BLOTWOOD_LOG.get(), 5, 5);
        fire.setFlammable(BLOTWOOD_PLANKS.get(), 5, 20);
        fire.setFlammable(BLOTWOOD_STAIRS.get(), 5, 20);
        fire.setFlammable(BLOTWOOD_SLAB.get(), 5, 20);
        fire.setFlammable(BLOTWOOD_FENCE.get(), 5, 20);
        fire.setFlammable(BLOTWOOD_LEAVES.get(), 30, 60);
        fire.setFlammable(SCRIPTORIUM_DESK.get(), 5, 20);
        fire.setFlammable(READING_STAND.get(), 5, 20);
        fire.setFlammable(ERASED_GRASS.get(), 60, 100);
    }

    private ModBlocks() {}
}
