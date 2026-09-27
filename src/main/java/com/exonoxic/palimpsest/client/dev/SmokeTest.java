package com.exonoxic.palimpsest.client.dev;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.client.screen.CodexScreen;
import com.exonoxic.palimpsest.client.screen.EndingScreen;
import com.exonoxic.palimpsest.client.screen.LoreScreen;
import com.exonoxic.palimpsest.codex.CodexEntry;
import com.exonoxic.palimpsest.entity.FairCopyEntity;
import com.exonoxic.palimpsest.entity.LonghandEntity;
import com.exonoxic.palimpsest.entity.PalehandEntity;
import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModEntities;
import com.exonoxic.palimpsest.world.ModStructures;
import com.exonoxic.palimpsest.world.dimension.ModDimensions;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Development-only visual smoke test. Inert unless the game is started with
 * {@code -Dpalimpsest.smokeTest=true} (CI does this under a virtual display).
 * <p>
 * It creates a fresh world, lays out every block, item and creature in front of the camera,
 * raises the Bleed, visits every Undertext biome and a copy of every structure, and opens the
 * mod's screens, taking a screenshot at each stop, then quits. A crash anywhere fails the CI
 * job, and the screenshots are the only way to look at the game without a person playing it.
 */
@Mod.EventBusSubscriber(modid = Palimpsest.MODID, value = Dist.CLIENT)
public final class SmokeTest {
    private static final Logger LOG = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("palimpsest.smokeTest");
    private static final String WORLD = "palimpsest_smoke";
    private static final int Y = 200;
    private static final int SETTLE = 260;

    private record Step(int delay, Runnable action) {}

    private static final List<Step> STEPS = new ArrayList<>();
    private static int clientTicks;
    private static boolean worldRequested;
    private static int stepIndex;
    private static int wait;
    private static String lastScreen = "";

    private SmokeTest() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        clientTicks++;
        String screen = mc.screen == null ? "none" : mc.screen.getClass().getName();
        if (!screen.equals(lastScreen)) {
            LOG.info("[smoke] screen -> {}", screen);
            lastScreen = screen;
        }
        if (!worldRequested) {
            if (clientTicks > 100 && mc.getOverlay() == null) createWorld(mc);
            return;
        }
        if (mc.level == null || mc.player == null) return;
        if (STEPS.isEmpty()) buildScript();
        if (wait > 0) {
            wait--;
            return;
        }
        if (stepIndex >= STEPS.size()) return;
        Step step = STEPS.get(stepIndex++);
        try {
            step.action().run();
        } catch (Throwable t) {
            LOG.error("[smoke] SMOKE_FAIL in step {}", stepIndex, t);
        }
        wait = step.delay();
    }

    private static void createWorld(Minecraft mc) {
        worldRequested = true;
        mc.options.pauseOnLostFocus = false;
        mc.options.hideGui = true;
        mc.getTutorial().setStep(TutorialSteps.NONE);
        try {
            FileUtils.deleteDirectory(mc.getLevelSource().getBaseDir().resolve(WORLD).toFile());
        } catch (Exception e) {
            LOG.warn("[smoke] could not clear old world", e);
        }
        LOG.info("[smoke] creating world");
        LevelSettings settings = new LevelSettings("Palimpsest smoke test", GameType.CREATIVE, false, Difficulty.NORMAL,
                true, new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(1453L, true, false),
                WorldPresets::createNormalWorldDimensions);
    }

    // ------------------------------------------------------------------ the script

    private static void buildScript() {
        step(100, () -> server((srv, p) -> {
            command(srv, p, "gamerule doDaylightCycle false");
            command(srv, p, "gamerule doMobSpawning false");
            command(srv, p, "gamerule doWeatherCycle false");
            command(srv, p, "gamerule announceAdvancements false");
            command(srv, p, "weather clear");
            command(srv, p, "time set 6000");
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1_000_000, 0, false, false));
        }));

        // Every block, spaced out on a platform in the sky.
        step(SETTLE, () -> server((srv, p) -> {
            ServerLevel level = p.serverLevel();
            clear(level, -12, Y - 1, -12, 12, Y + 12, 24, Blocks.SMOOTH_STONE.defaultBlockState());
            int i = 0;
            for (Block b : ForgeRegistries.BLOCKS.getValues()) {
                if (!Palimpsest.MODID.equals(ForgeRegistries.BLOCKS.getKey(b).getNamespace())) continue;
                level.setBlock(new BlockPos(-8 + (i % 9) * 2, Y, (i / 9) * 2), b.defaultBlockState(), Block.UPDATE_CLIENTS);
                i++;
            }
            LOG.info("[smoke] placed {} blocks", i);
            moveTo(p, level, 0.5, Y + 8.0, -5.0, 0F, 50F);
        }));
        shot("blocks");

        // Every item, in frames on a wall.
        step(120, () -> server((srv, p) -> {
            ServerLevel level = p.serverLevel();
            clear(level, -12, Y - 1, -12, 12, Y + 12, 24, Blocks.SMOOTH_STONE.defaultBlockState());
            fill(level, -9, Y, 8, 9, Y + 8, 8, Blocks.SMOOTH_STONE.defaultBlockState());
            int i = 0;
            for (Item item : ForgeRegistries.ITEMS.getValues()) {
                if (!Palimpsest.MODID.equals(ForgeRegistries.ITEMS.getKey(item).getNamespace()) || item instanceof BlockItem) continue;
                ItemFrame frame = new ItemFrame(level, new BlockPos(-8 + (i % 17), Y + 7 - i / 17, 7), Direction.NORTH);
                frame.setItem(new ItemStack(item), false);
                frame.setInvisible(true);
                level.addFreshEntity(frame);
                i++;
            }
            LOG.info("[smoke] framed {} items", i);
            moveTo(p, level, 0.5, Y + 2.5, 1.5, 0F, 0F);
        }));
        shot("items");

        // One close-up per creature.
        List<EntityType<?>> creatures = List.of(ModEntities.FOXING_MOTH.get(), ModEntities.BLOTLING.get(), ModEntities.SMUDGE.get(),
                ModEntities.MARGIN_CRAWLER.get(), ModEntities.QUILLCROW.get(), ModEntities.PALE_STAG.get(), ModEntities.INKHOUND.get(),
                ModEntities.RUBRICATOR.get(), ModEntities.KNOCKER.get(), ModEntities.COPYIST.get(), ModEntities.LONGHAND.get(),
                ModEntities.REDACTED.get(), ModEntities.FAIR_COPY.get(), ModEntities.ERRATUM.get(), ModEntities.BOOKBINDER.get(),
                ModEntities.RASURE.get());
        for (EntityType<?> type : creatures) {
            String name = ForgeRegistries.ENTITY_TYPES.getKey(type).getPath();
            step(40, () -> server((srv, p) -> {
                ServerLevel level = p.serverLevel();
                killMobs(level);
                clear(level, -12, Y - 1, -12, 12, Y + 12, 24, Blocks.SMOOTH_STONE.defaultBlockState());
                Entity e = type.create(level);
                if (e == null) return;
                e.moveTo(0.5, Y, 0.5, 180F, 0F);
                e.setYHeadRot(180F);
                if (e instanceof Mob mob) {
                    mob.setNoAi(true);
                    mob.setPersistenceRequired();
                    mob.yBodyRot = 180F;
                }
                level.addFreshEntity(e);
                double h = Math.max(0.6, e.getBbHeight());
                double dist = Math.max(2.4, Math.max(h, e.getBbWidth()) * 1.35 + e.getBbWidth() / 2);
                moveTo(p, level, 0.5, Y + h * 0.6 - 1.62, 0.5 - dist, 0F, 4F);
            }));
            shot("creature_" + name);
        }
        // Apparitions only exist for one player and keep their distance: a Longhand watcher vanishes
        // inside 24 blocks and a Fair Copy walks away inside 12, so these are taken zoomed in.
        step(40, () -> server((srv, p) -> {
            ServerLevel level = p.serverLevel();
            killMobs(level);
            clear(level, -12, Y - 1, -12, 12, Y + 12, 24, Blocks.SMOOTH_STONE.defaultBlockState());
            moveTo(p, level, 0.5, Y + 0.4, -25.5, 0F, 0F);
            LonghandEntity.spawnWatcher(level, new BlockPos(0, Y, 0), p);
        }));
        step(1, () -> Minecraft.getInstance().options.fov().set(30));
        shot("creature_longhand_watcher");
        step(40, () -> server((srv, p) -> {
            ServerLevel level = p.serverLevel();
            killMobs(level);
            moveTo(p, level, 0.5, Y + 0.2, -15.5, 0F, 0F);
            FairCopyEntity.spawnFor(level, new BlockPos(0, Y, 0), p);
        }));
        shot("creature_fair_copy_sighting");
        step(1, () -> Minecraft.getInstance().options.fov().set(70));

        // The Palehand only exists as a sighting for one player, and sinks if they come within 80 blocks.
        step(150, () -> server((srv, p) -> {
            ServerLevel level = p.serverLevel();
            killMobs(level);
            moveTo(p, level, 0.5, Y + 6.0, 0.5, 0F, -14F);
            PalehandEntity.spawnSighting(level, new BlockPos(0, Y, 120), p);
        }));
        shot("creature_palehand");

        // Night and a high Bleed, somewhere on land, with the HUD and overlays showing.
        step(40, () -> server((srv, p) -> {
            ServerLevel level = p.serverLevel();
            killMobs(level);
            command(srv, p, "time set 18000");
            command(srv, p, "palimpsest bleed set @s 960");
            BlockPos land = findBiome(level, BiomeTags.IS_FOREST, p.blockPosition());
            level.getChunk(land.getX() >> 4, land.getZ() >> 4);
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, land);
            moveTo(p, level, land.getX() + 0.5, ground.getY() + 0.2, land.getZ() + 0.5, 30F, 0F);
            p.removeEffect(MobEffects.NIGHT_VISION);
        }));
        step(SETTLE, () -> Minecraft.getInstance().options.hideGui = false);
        shot("overworld_night_bleed");
        step(10, () -> {
            Minecraft.getInstance().options.hideGui = true;
            server((srv, p) -> {
                command(srv, p, "time set 6000");
                command(srv, p, "palimpsest bleed set @s 0");
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1_000_000, 0, false, false));
            });
        });

        // A copy of every Overworld structure.
        for (ResourceKey<Structure> key : List.of(ModStructures.WRAY_CABIN, ModStructures.SCRAPED_OBELISK, ModStructures.HOLLOW_CHAPEL,
                ModStructures.COPYING_HOUSE, ModStructures.CROW_ROOST, ModStructures.DOUBLED_HOUSE, ModStructures.SURVEY_STATION,
                ModStructures.BROKEN_GATE)) {
            step(SETTLE, () -> server((srv, p) -> visitStructure(srv, p, Level.OVERWORLD, key)));
            shot("structure_" + key.location().getPath());
        }

        // Every Undertext biome, then every Undertext structure (seen as a player would, without night vision).
        step(5, () -> server((srv, p) -> p.removeEffect(MobEffects.NIGHT_VISION)));
        for (String biome : List.of("scraped_expanse", "blotwood", "the_gutter", "inkwell_sea", "rubric_wastes", "marginalia")) {
            step(SETTLE, () -> server((srv, p) -> {
                ServerLevel undertext = srv.getLevel(ModDimensions.UNDERTEXT);
                if (undertext == null) {
                    LOG.error("[smoke] SMOKE_FAIL the Undertext is not loaded");
                    return;
                }
                ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, Palimpsest.id(biome));
                Pair<BlockPos, Holder<Biome>> found = undertext.findClosestBiome3d(h -> h.is(key), new BlockPos(0, 64, 0), 3000, 32, 64);
                if (found == null) {
                    LOG.error("[smoke] SMOKE_FAIL biome {} not found within 3000 blocks", biome);
                    return;
                }
                BlockPos at = found.getFirst();
                undertext.getChunk(at.getX() >> 4, at.getZ() >> 4);
                int top = undertext.getHeight(Heightmap.Types.MOTION_BLOCKING, at.getX(), at.getZ());
                LOG.info("[smoke] biome {} at {} {} {}", biome, at.getX(), top, at.getZ());
                moveTo(p, undertext, at.getX() + 0.5, top + 1.0, at.getZ() + 0.5, 45F, 8F);
            }));
            shot("biome_" + biome);
        }
        for (ResourceKey<Structure> key : List.of(ModStructures.FADED_VILLAGE, ModStructures.MARGINALIA_SPIRE, ModStructures.INK_WELL,
                ModStructures.SCRAP_SHRINE, ModStructures.BINDERY, ModStructures.LAST_FOLIO)) {
            step(SETTLE, () -> server((srv, p) -> visitStructure(srv, p, ModDimensions.UNDERTEXT, key)));
            shot("structure_" + key.location().getPath());
        }

        // Screens.
        step(20, () -> {
            Minecraft.getInstance().options.hideGui = false;
            server((srv, p) -> command(srv, p, "palimpsest codex unlockall"));
        });
        step(20, () -> Minecraft.getInstance().setScreen(new CodexScreen()));
        shot("screen_codex");
        step(10, () -> Minecraft.getInstance().setScreen(new CodexScreen(CodexEntry.Category.CREATURES, "knocker")));
        shot("screen_codex_creature");
        step(10, () -> Minecraft.getInstance().setScreen(new CodexScreen(CodexEntry.Category.PLACES, "wray_cabin")));
        shot("screen_codex_place");
        step(20, () -> Minecraft.getInstance().setScreen(new LoreScreen("folio", 1)));
        shot("screen_folio");
        step(20, () -> Minecraft.getInstance().setScreen(new LoreScreen("faded", 3)));
        shot("screen_faded");
        step(20, () -> Minecraft.getInstance().setScreen(new EndingScreen(BlockPos.ZERO)));
        shot("screen_ending");
        step(20, () -> {
            Minecraft.getInstance().setScreen(null);
            server((srv, p) -> {
                BlockPos desk = p.blockPosition().below(2);
                p.serverLevel().setBlock(desk, ModBlocks.SCRIPTORIUM_DESK.get().defaultBlockState(), Block.UPDATE_ALL);
                if (p.serverLevel().getBlockEntity(desk) instanceof MenuProvider provider) {
                    NetworkHooks.openScreen(p, provider, desk);
                } else {
                    LOG.error("[smoke] SMOKE_FAIL scriptorium desk has no menu");
                }
            });
        });
        shot("screen_desk");

        step(40, () -> {
            Minecraft.getInstance().setScreen(null);
            LOG.info("[smoke] PALIMPSEST SMOKE TEST COMPLETE");
        });
        step(20, () -> Minecraft.getInstance().stop());
    }

    // ------------------------------------------------------------------ helpers

    private static void step(int delay, Runnable action) {
        STEPS.add(new Step(delay, action));
    }

    private static void shot(String name) {
        step(2, () -> Minecraft.getInstance().getToasts().clear());
        step(30, () -> {
            Minecraft mc = Minecraft.getInstance();
            Screenshot.grab(mc.gameDirectory, "smoke_" + name + ".png", mc.getMainRenderTarget(), msg -> {});
            LOG.info("[smoke] screenshot {}", name);
        });
    }

    private interface ServerAction {
        void run(MinecraftServer server, ServerPlayer player);
    }

    private static void server(ServerAction action) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer srv = mc.getSingleplayerServer();
        if (srv == null || mc.player == null) return;
        srv.execute(() -> {
            ServerPlayer p = srv.getPlayerList().getPlayer(mc.player.getUUID());
            if (p == null) return;
            try {
                action.run(srv, p);
            } catch (Throwable t) {
                LOG.error("[smoke] SMOKE_FAIL on server", t);
            }
        });
    }

    private static void command(MinecraftServer srv, ServerPlayer p, String command) {
        srv.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), command);
    }

    /** Teleports and keeps the player flying (vanilla cancels creative flight on touching ground). */
    private static void moveTo(ServerPlayer p, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        p.teleportTo(level, x, y, z, yaw, pitch);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
    }

    private static BlockPos findBiome(ServerLevel level, TagKey<Biome> tag, BlockPos from) {
        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(h -> h.is(tag), from, 4000, 32, 64);
        return found != null ? found.getFirst() : from;
    }

    private static void visitStructure(MinecraftServer srv, ServerPlayer p, ResourceKey<Level> dimension, ResourceKey<Structure> key) {
        ServerLevel level = srv.getLevel(dimension);
        if (level == null) return;
        Optional<Holder.Reference<Structure>> holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(key);
        if (holder.isEmpty()) {
            LOG.error("[smoke] SMOKE_FAIL structure {} not registered", key.location());
            return;
        }
        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(holder.get()), new BlockPos(0, 64, 0), 100, false);
        if (found == null) {
            LOG.error("[smoke] SMOKE_FAIL structure {} not found within 100 chunks", key.location());
            return;
        }
        BlockPos at = found.getFirst();
        ChunkAccess chunk = level.getChunk(at.getX() >> 4, at.getZ() >> 4);
        StructureStart start = chunk.getStartForStructure(holder.get().value());
        if (start == null || !start.isValid()) {
            LOG.warn("[smoke] structure {} start missing at {}", key.location(), at);
            moveTo(p, level, at.getX() + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING, at.getX(), at.getZ()) + 12, at.getZ() - 14.5, 0F, 35F);
            return;
        }
        BoundingBox box = start.getBoundingBox();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) level.getChunk(cx, cz);
        }
        BlockPos c = box.getCenter();
        boolean buried = level.getHeight(Heightmap.Types.WORLD_SURFACE, c.getX(), c.getZ()) > box.maxY() + 2;
        LOG.info("[smoke] structure {} at {} (box {}..{}), {}", key.location(), c, box.minY(), box.maxY(), buried ? "buried" : "surface");
        if (buried) {
            // Underground: stand inside the first room, looking along it.
            moveTo(p, level, c.getX() + 0.5, box.minY() + 1.2, box.minZ() + 1.5, 0F, 10F);
        } else {
            // Frame the building from above and to the south, aimed at the ground at its centre.
            // In the Undertext the fog ends at ~48 blocks, so stay closer.
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX(), c.getZ());
            double back = Math.max(box.getXSpan(), box.getZSpan()) * 0.5 + (dimension == ModDimensions.UNDERTEXT ? 8 : 12);
            moveTo(p, level, c.getX() + 0.5, ground + back * 0.55, c.getZ() - back, 0F, 29F);
        }
    }

    private static void killMobs(ServerLevel level) {
        List<Entity> doomed = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof Mob || e instanceof ItemFrame) doomed.add(e);
        }
        doomed.forEach(Entity::discard);
    }

    private static void clear(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1, BlockState floor) {
        fill(level, x0, y0 + 1, z0, x1, y1, z1, Blocks.AIR.defaultBlockState());
        fill(level, x0, y0, z0, x1, y0, z1, floor);
    }

    private static void fill(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (BlockPos pos : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }
}
