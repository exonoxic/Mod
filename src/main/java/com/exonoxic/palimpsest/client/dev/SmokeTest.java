package com.exonoxic.palimpsest.client.dev;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.client.screen.CodexScreen;
import com.exonoxic.palimpsest.client.screen.EndingScreen;
import com.exonoxic.palimpsest.client.screen.LoreScreen;
import com.exonoxic.palimpsest.registry.ModEntities;
import com.exonoxic.palimpsest.world.dimension.ModDimensions;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Development-only visual smoke test. Inert unless the game is started with
 * {@code -Dpalimpsest.smokeTest=true} (CI does this under a virtual display).
 * <p>
 * It creates a fresh world, lays out every block, every item and every creature in front of
 * the camera, raises the Bleed, visits the Undertext and opens the mod's screens, taking a
 * screenshot of each, then quits. A crash anywhere along the way fails the CI job, and the
 * screenshots are the only way to look at the art without a human at a keyboard.
 */
@Mod.EventBusSubscriber(modid = Palimpsest.MODID, value = Dist.CLIENT)
public final class SmokeTest {
    private static final Logger LOG = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("palimpsest.smokeTest");
    private static final String WORLD = "palimpsest_smoke";
    private static final int Y = 200;

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
        try {
            FileUtils.deleteDirectory(mc.getLevelSource().getBaseDir().resolve(WORLD).toFile());
        } catch (Exception e) {
            LOG.warn("[smoke] could not clear old world", e);
        }
        LOG.info("[smoke] creating world");
        LevelSettings settings = new LevelSettings("Palimpsest smoke test", GameType.CREATIVE, false, Difficulty.PEACEFUL,
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
            command(srv, p, "weather clear");
            command(srv, p, "time set 6000");
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }));

        // Every block.
        step(160, () -> server((srv, p) -> {
            ServerLevel level = p.serverLevel();
            clear(level, -12, Y - 1, -12, 12, Y + 12, 20, Blocks.SMOOTH_STONE.defaultBlockState());
            List<Block> blocks = new ArrayList<>();
            for (Block b : ForgeRegistries.BLOCKS.getValues()) {
                if (Palimpsest.MODID.equals(ForgeRegistries.BLOCKS.getKey(b).getNamespace())) blocks.add(b);
            }
            int i = 0;
            for (Block b : blocks) {
                int x = -6 + (i % 12), z = i / 12;
                BlockState state = b.defaultBlockState();
                level.setBlock(new BlockPos(x, Y, z), state, Block.UPDATE_CLIENTS);
                i++;
            }
            LOG.info("[smoke] placed {} blocks", blocks.size());
            p.teleportTo(level, 0.0, Y + 5.0, -4.5, 0F, 45F);
        }));
        shot("blocks");

        // Every item, in frames on a wall.
        step(120, () -> server((srv, p) -> {
            ServerLevel level = p.serverLevel();
            clear(level, -12, Y - 1, -12, 12, Y + 12, 20, Blocks.SMOOTH_STONE.defaultBlockState());
            fill(level, -9, Y, 8, 9, Y + 8, 8, Blocks.SMOOTH_STONE.defaultBlockState());
            List<Item> items = new ArrayList<>();
            for (Item item : ForgeRegistries.ITEMS.getValues()) {
                if (Palimpsest.MODID.equals(ForgeRegistries.ITEMS.getKey(item).getNamespace()) && !(item instanceof BlockItem)) items.add(item);
            }
            int i = 0;
            for (Item item : items) {
                int x = -8 + (i % 17), y = Y + 7 - i / 17;
                ItemFrame frame = new ItemFrame(level, new BlockPos(x, y, 7), Direction.NORTH);
                frame.setItem(new ItemStack(item), false);
                frame.setInvisible(true);
                level.addFreshEntity(frame);
                i++;
            }
            LOG.info("[smoke] framed {} items", items.size());
            p.teleportTo(level, 0.0, Y + 3.5, -2.5, 0F, 0F);
        }));
        shot("items");

        // The smaller creatures.
        step(80, () -> server((srv, p) -> {
            ServerLevel level = p.serverLevel();
            killMobs(level);
            clear(level, -12, Y - 1, -12, 12, Y + 12, 20, Blocks.SMOOTH_STONE.defaultBlockState());
            List<EntityType<?>> small = List.of(ModEntities.FOXING_MOTH.get(), ModEntities.BLOTLING.get(), ModEntities.SMUDGE.get(),
                    ModEntities.MARGIN_CRAWLER.get(), ModEntities.QUILLCROW.get(), ModEntities.INKHOUND.get(), ModEntities.COPYIST.get(),
                    ModEntities.ERRATUM.get(), ModEntities.REDACTED.get(), ModEntities.FAIR_COPY.get());
            lineUp(level, small, 2.2, 5.0);
            p.teleportTo(level, 0.0, Y + 1.6, -3.0, 0F, 8F);
        }));
        shot("creatures_small");

        // The larger ones.
        step(80, () -> server((srv, p) -> {
            ServerLevel level = p.serverLevel();
            killMobs(level);
            List<EntityType<?>> big = List.of(ModEntities.PALE_STAG.get(), ModEntities.RUBRICATOR.get(), ModEntities.KNOCKER.get(),
                    ModEntities.LONGHAND.get(), ModEntities.BOOKBINDER.get(), ModEntities.RASURE.get());
            lineUp(level, big, 4.0, 9.0);
            p.teleportTo(level, 0.0, Y + 3.0, -6.0, 0F, 5F);
        }));
        shot("creatures_large");

        step(60, () -> server((srv, p) -> {
            ServerLevel level = p.serverLevel();
            killMobs(level);
            lineUp(level, List.of(ModEntities.PALEHAND.get()), 0, 30.0);
            p.teleportTo(level, 0.0, Y + 8.0, -30.0, 0F, -18F);
        }));
        step(40, () -> {});
        shot("palehand");

        // Night, high Bleed, with the HUD and overlays showing.
        step(60, () -> server((srv, p) -> {
            killMobs(p.serverLevel());
            command(srv, p, "time set 18000");
            command(srv, p, "palimpsest bleed set @s 960");
            BlockPos ground = p.serverLevel().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(40, 0, 40));
            p.teleportTo(p.serverLevel(), 40.5, ground.getY() + 1.7, 40.5, 30F, 0F);
        }));
        step(200, () -> Minecraft.getInstance().options.hideGui = false);
        shot("overworld_night_bleed");

        // The Undertext.
        step(40, () -> {
            Minecraft.getInstance().options.hideGui = true;
            server((srv, p) -> teleportUndertext(srv, p, 0, 0, 2.0, 0F, 5F));
        });
        step(300, () -> {});
        shot("undertext_ground");
        step(20, () -> server((srv, p) -> teleportUndertext(srv, p, 0, 0, 45.0, 30F, 25F)));
        step(300, () -> {});
        shot("undertext_high");
        step(20, () -> server((srv, p) -> teleportUndertext(srv, p, 700, -400, 3.0, 200F, 0F)));
        step(300, () -> {});
        shot("undertext_far");

        // Screens.
        step(20, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.hideGui = false;
            mc.setScreen(new CodexScreen());
        });
        shot("screen_codex");
        step(20, () -> Minecraft.getInstance().setScreen(new LoreScreen("folio", 1)));
        shot("screen_folio");
        step(20, () -> Minecraft.getInstance().setScreen(new LoreScreen("faded", 3)));
        shot("screen_faded");
        step(20, () -> Minecraft.getInstance().setScreen(new EndingScreen(BlockPos.ZERO)));
        shot("screen_ending");

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

    private static void teleportUndertext(MinecraftServer srv, ServerPlayer p, int x, int z, double above, float yaw, float pitch) {
        ServerLevel undertext = srv.getLevel(ModDimensions.UNDERTEXT);
        if (undertext == null) {
            LOG.error("[smoke] SMOKE_FAIL the Undertext is not loaded");
            return;
        }
        undertext.getChunk(x >> 4, z >> 4);
        int top = undertext.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        p.teleportTo(undertext, x + 0.5, top + above, z + 0.5, yaw, pitch);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        LOG.info("[smoke] in the Undertext at {} {} {} biome {}", x, top, z,
                undertext.getBiome(new BlockPos(x, top, z)).unwrapKey().map(k -> k.location().toString()).orElse("?"));
    }

    private static void lineUp(ServerLevel level, List<EntityType<?>> types, double spacing, double distance) {
        double start = -spacing * (types.size() - 1) / 2.0;
        for (int i = 0; i < types.size(); i++) {
            Entity e = types.get(i).create(level);
            if (e == null) continue;
            e.moveTo(start + i * spacing, Y, distance, 180F, 0F);
            e.setYHeadRot(180F);
            if (e instanceof Mob mob) {
                mob.setNoAi(true);
                mob.setPersistenceRequired();
                mob.yBodyRot = 180F;
            }
            level.addFreshEntity(e);
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
