package com.exonoxic.palimpsest.client.dev;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.bleed.BleedCapability;
import com.exonoxic.palimpsest.client.ClientBleedState;
import com.exonoxic.palimpsest.client.screen.CodexScreen;
import com.exonoxic.palimpsest.client.screen.EndingScreen;
import com.exonoxic.palimpsest.client.screen.LoreScreen;
import com.exonoxic.palimpsest.codex.CodexEntry;
import com.exonoxic.palimpsest.entity.CopyistEntity;
import com.exonoxic.palimpsest.entity.FairCopyEntity;
import com.exonoxic.palimpsest.entity.KnockerEntity;
import com.exonoxic.palimpsest.entity.LonghandEntity;
import com.exonoxic.palimpsest.entity.PalehandEntity;
import com.exonoxic.palimpsest.entity.ai.Squeeze;
import com.exonoxic.palimpsest.horror.EventContext;
import com.exonoxic.palimpsest.horror.HorrorDirector;
import com.exonoxic.palimpsest.horror.HorrorEvent;
import com.exonoxic.palimpsest.horror.HorrorEvents;
import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModEntities;
import com.exonoxic.palimpsest.world.ModStructures;
import com.exonoxic.palimpsest.world.dimension.ModDimensions;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

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
    /** For working on one thing at a time: only the real Knocker at a door, then quit (PALIMPSEST_SMOKE_QUICK=true). */
    private static final boolean QUICK = Boolean.getBoolean("palimpsest.smokeQuick");
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
    private static final List<Entity> MANNEQUINS = new ArrayList<>();
    /** Knocker hunt: server steps it has been seen in the gap, and coming for the player; shots taken. */
    private static volatile int huntGap;
    private static volatile int huntLunge;
    private static final Set<String> huntShots = new HashSet<>();
    private static int nextMannequinId = 2_000_000;

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

        if (QUICK) {
            realDoor("a", 1.5, 4.6, 4.93, false, 4.0);
            realDoor("b", 5.5, 2.4, 2.63, true, 3.8125);
            step(20, () -> Minecraft.getInstance().stop());
            return;
        }

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
        featuredCreatures();
        bestiaryPortraits();
        // A real Knocker nobody answers, end to end: a hut at night with a door on one side and a
        // one-block gap in the far wall; the player stands inside looking at the gap. The Knocker is
        // put at the door with its knocking nearly done, and should go quiet, find the gap, crawl in.
        step(40, () -> {
            clearMannequins();
            server((srv, p) -> {
                ServerLevel level = p.serverLevel();
                killMobs(level);
                clear(level, -12, Y - 1, -12, 12, Y + 12, 24, Blocks.SMOOTH_STONE.defaultBlockState());
                command(srv, p, "time set 18000");
                // As the player would see it: by the light of one torch.
                p.removeEffect(MobEffects.NIGHT_VISION);
                fill(level, -3, Y, -3, 3, Y + 2, 3, Blocks.STONE_BRICKS.defaultBlockState());
                fill(level, -3, Y + 3, -3, 3, Y + 3, 3, Blocks.SPRUCE_PLANKS.defaultBlockState());
                fill(level, -2, Y, -2, 2, Y + 2, 2, Blocks.AIR.defaultBlockState());
                BlockState lower = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
                level.setBlock(new BlockPos(0, Y, 3), lower, Block.UPDATE_ALL);
                level.setBlock(new BlockPos(0, Y + 1, 3), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
                level.setBlock(new BlockPos(0, Y, -3), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(new BlockPos(1, Y, 1), Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
                view(p, 0.5, Y + 1.62, 1.2, 0.5, Y + 0.4, -3.0);
                // It will not go for someone in creative, so for this the player is in survival (but
                // cannot be hurt).
                p.setGameMode(GameType.SURVIVAL);
                p.getAbilities().invulnerable = true;
                p.onUpdateAbilities();
                if (!KnockerEntity.spawnAtDoor(level, new BlockPos(0, Y, 3), p)) {
                    LOG.error("[smoke] SMOKE_FAIL the Knocker would not come to the hut door");
                    return;
                }
                for (KnockerEntity k : level.getEntitiesOfClass(KnockerEntity.class, p.getBoundingBox().inflate(16))) {
                    // Skip to the end of its third round of knocking.
                    CompoundTag tag = new CompoundTag();
                    k.saveWithoutId(tag);
                    tag.putInt("Rounds", 3);
                    k.load(tag);
                }
            });
        });
        // Watched in short steps (the server gets through two or three ticks for each of the
        // client's here): its state is logged now and then, the moment it is in the gap is
        // photographed, and once it comes for the player they turn round to face it.
        step(1, () -> {
            huntGap = 0;
            huntLunge = 0;
            huntShots.clear();
        });
        for (int i = 0; i < 48; i++) {
            int n = i;
            step(8, () -> server((srv, p) -> {
                for (KnockerEntity k : p.serverLevel().getEntitiesOfClass(KnockerEntity.class, p.getBoundingBox().inflate(32))) {
                    if (Math.abs(k.getX() - 0.5D) < 1.0D && k.getZ() > -4.3D && k.getZ() < -1.7D) huntGap++;
                    if (k.getState() == KnockerEntity.LUNGE) {
                        huntLunge++;
                        Vec3 eye = p.getEyePosition();
                        Vec3 at = k.getEyePosition();
                        double dx = at.x - eye.x, dy = at.y - eye.y, dz = at.z - eye.z;
                        float yaw = (float) Math.toDegrees(Mth.atan2(dz, dx)) - 90F;
                        float pitch = (float) -Math.toDegrees(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
                        p.connection.teleport(p.getX(), p.getY(), p.getZ(), yaw, pitch);
                    }
                    if (n % 3 != 0) continue;
                    // And whether, from where it stands, it could get in to the player right now.
                    Path way = k.wayIn(p.blockPosition());
                    String route = way == null ? "none" : way.getNodeCount() + " nodes, reaches " + way.canReach() + ", ends "
                            + (way.getNodeCount() > 0 ? way.getEndNode().asBlockPos().offset(0, -Y, 0).toShortString() : "-");
                    LOG.info("[smoke] knocker hunt {}: state {} pose {} creeping {} at {} {} {} (player at {} {}); way in: {}", n, k.getState(),
                            k.getPose(), k.isCreeping(), String.format("%.1f", k.getX()), String.format("%.1f", k.getY() - Y),
                            String.format("%.1f", k.getZ()), String.format("%.1f", p.getX()), String.format("%.1f", p.getZ()), route);
                }
            }));
            step(1, () -> {
                if (huntGap > 0 && huntShots.add("gap")) grabNow("knocker_hunt_gap");
                else if (huntLunge >= 2 && huntShots.add("face")) grabNow("knocker_hunt_face");
                else if (huntLunge >= 6 && huntShots.add("face_close")) grabNow("knocker_hunt_face_close");
                else if (n % 4 == 0) grabNow("knocker_hunt_" + n / 4);
            });
        }
        step(10, () -> server((srv, p) -> {
            killMobs(p.serverLevel());
            command(srv, p, "time set 6000");
            p.setGameMode(GameType.CREATIVE);
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1_000_000, 0, false, false));
        }));

        realDoor("a", 1.5, 4.6, 4.93, false, 4.0);
        realDoor("b", 5.5, 2.4, 2.63, true, 3.8125);

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

        // Every Overworld horror event, fired directly, with something nearby for each to act on.
        step(20, () -> server((srv, p) -> furnishForEvents(p)));
        for (HorrorEvent event : HorrorEvents.ALL) {
            if (event.undertextOnly) continue;
            step(VISUAL_EVENTS.contains(event.id) ? 25 : 50, () -> server((srv, p) -> fireEvent(p, event)));
            if (VISUAL_EVENTS.contains(event.id)) shot("event_" + event.id);
        }
        step(200, () -> server((srv, p) -> killMobs(p.serverLevel())));
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
        // Per-player state must survive a change of dimension.
        step(40, () -> server((srv, p) -> {
            ServerLevel undertext = srv.getLevel(ModDimensions.UNDERTEXT);
            if (undertext != null) moveTo(p, undertext, 0.5, 120, 0.5, 0F, 0F);
        }));
        step(20, () -> server((srv, p) -> {
            command(srv, p, "palimpsest bleed set @s 432");
            float bleed = BleedCapability.get(p).getBleed();
            if (Math.abs(bleed - 432F) > 0.01F) LOG.error("[smoke] SMOKE_FAIL Bleed after a dimension change is {} (expected 432)", bleed);
            else LOG.info("[smoke] Bleed survives a dimension change");
        }));
        step(10, () -> {
            float seen = ClientBleedState.bleed();
            if (Math.abs(seen - 432F) > 0.01F) LOG.error("[smoke] SMOKE_FAIL client sees Bleed {} (expected 432)", seen);
            server((srv, p) -> command(srv, p, "palimpsest bleed set @s 0"));
        });
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
        for (HorrorEvent event : HorrorEvents.ALL) {
            if (!event.undertextOnly) continue;
            step(50, () -> server((srv, p) -> fireEvent(p, event)));
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

    /**
     * A real Knocker put at a real door by {@code spawnAtDoor}, photographed every few ticks as it
     * knocks, in profile from the west. It stands on whichever side of the door is farther from the
     * player, so the two calls (the player at {@code playerZ} when it is placed, then the camera
     * moved to {@code camZ}, looking at {@code lookZ}) cover both the side the door panel is on and
     * the one it is not. Fails if its face ends up past the panel's face at {@code panelZ} (its head
     * reaches about 0.85 blocks ahead of its feet when it leans in to knock).
     */
    private static void realDoor(String tag, double playerZ, double camZ, double lookZ, boolean facesSouth, double panelZ) {
        step(40, () -> {
            clearMannequins();
            server((srv, p) -> {
                ServerLevel level = p.serverLevel();
                killMobs(level);
                // It leaves at dawn, and by day.
                command(srv, p, "time set 18000");
                clear(level, -12, Y - 1, -12, 12, Y + 12, 24, Blocks.SMOOTH_STONE.defaultBlockState());
                fill(level, -4, Y, 3, 4, Y + 3, 3, Blocks.SPRUCE_PLANKS.defaultBlockState());
                BlockState lower = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
                level.setBlock(new BlockPos(0, Y, 3), lower, Block.UPDATE_ALL);
                level.setBlock(new BlockPos(0, Y + 1, 3), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
                moveTo(p, level, 0.5, Y, playerZ, 0F, 0F);
                if (!KnockerEntity.spawnAtDoor(level, new BlockPos(0, Y, 3), p)) LOG.error("[smoke] SMOKE_FAIL no Knocker at the door ({})", tag);
                else for (KnockerEntity k : level.getEntitiesOfClass(KnockerEntity.class, p.getBoundingBox().inflate(16))) {
                    LOG.info("[smoke] real door {}: Knocker at {} {} facing {}", tag, String.format("%.2f", k.getX()), String.format("%.2f", k.getZ()), k.getDirection());
                    double face = k.getZ() + (facesSouth ? 0.85D : -0.85D);
                    if (facesSouth ? face > panelZ + 0.02D : face < panelZ - 0.02D)
                        LOG.error("[smoke] SMOKE_FAIL the Knocker's face is through the door ({}): face at {}, panel at {}", tag, face, panelZ);
                }
                view(p, -3.0, Y + 1.9, camZ, 0.5, Y + 1.6, lookZ);
            });
        });
        for (int i = 0; i < 18; i++) {
            int n = i;
            step(4, () -> grabNow("realdoor_" + tag + "_" + (n < 10 ? "0" : "") + n));
        }
    }

    // ------------------------------------------------------------------ featured creatures

    /**
     * The four creatures with the most detail, posed by hand in the states players meet them in and
     * seen from several sides. They exist only on this client, so nothing on the server walks them
     * out of the pose or makes them vanish.
     */
    /**
     * The rest of the bestiary, each close up in daylight on a bare floor, some in the state that
     * shows them at their worst (a mannequin is created at the start of its step, so its animation
     * clock reads about 32 ticks when the photograph is taken).
     */
    private static void bestiaryPortraits() {
        portrait("inkhound", ModEntities.INKHOUND.get(), 200F, 1.6, 1.1, -1.5, 0.6, e -> {});
        portrait("inkhound_hunting", ModEntities.INKHOUND.get(), 200F, 1.3, 0.9, -1.2, 0.55, e -> ((Mob) e).setAggressive(true));
        portrait("pale_stag", ModEntities.PALE_STAG.get(), 195F, 2.4, 2.1, -3.4, 1.5, e -> {});
        portrait("pale_stag_face", ModEntities.PALE_STAG.get(), 190F, 1.0, 2.3, -1.6, 2.1, e -> {});
        portrait("smudge", ModEntities.SMUDGE.get(), 190F, 1.3, 1.9, -2.2, 1.4, e -> {});
        portrait("redacted", ModEntities.REDACTED.get(), 195F, 1.5, 2.0, -2.5, 1.4, e -> {});
        // Coming for you, the bar over the eyes slides off them.
        portrait("redacted_revealed", ModEntities.REDACTED.get(), 190F, 0.9, 1.95, -1.3, 1.8, e -> ((Mob) e).setAggressive(true));
        portrait("rubricator", ModEntities.RUBRICATOR.get(), 195F, 1.3, 2.0, -2.1, 1.5, e -> {});
        portrait("quillcrow", ModEntities.QUILLCROW.get(), 205F, 1.0, 0.7, -1.1, 0.3, e -> {});
        portrait("foxing_moth", ModEntities.FOXING_MOTH.get(), 180F, 0.5, 1.3, -0.2, 0.1, e -> {});
        portrait("blotling", ModEntities.BLOTLING.get(), 200F, 1.3, 0.9, -1.5, 0.4, e -> {});
        portrait("margin_crawler", ModEntities.MARGIN_CRAWLER.get(), 205F, 1.1, 0.75, -1.2, 0.3, e -> ((Mob) e).setAggressive(true));
        portrait("bookbinder", ModEntities.BOOKBINDER.get(), 200F, 3.4, 2.9, -4.4, 1.5, e -> {});
        portrait("bookbinder_face", ModEntities.BOOKBINDER.get(), 195F, 1.2, 3.0, -1.9, 2.8, e -> {});
        portrait("rasure", ModEntities.RASURE.get(), 195F, 3.2, 3.4, -5.6, 2.6, e -> {});
        portrait("rasure_face", ModEntities.RASURE.get(), 190F, 1.0, 4.4, -2.4, 4.4, e -> {});
        // Its face is yours, except for the eyes.
        portrait("fair_copy_face", ModEntities.FAIR_COPY.get(), 180F, 0.5, 1.55, -1.0, 1.5, e -> {
            LocalPlayer me = Minecraft.getInstance().player;
            if (me != null) data(e, "COPY_OF", Optional.of(me.getUUID()));
        });
    }

    /** One mannequin at (0.5, 0.5) facing the camera, seen from (0.5 + dx, Y + eyeY, 0.5 + dz) looking at height lookY. */
    private static <T extends Entity> void portrait(String name, EntityType<T> type, float yaw, double dx, double eyeY, double dz, double lookY,
                                                    Consumer<Entity> setup) {
        step(20, () -> {
            clearMannequins();
            server((srv, p) -> {
                ServerLevel level = p.serverLevel();
                killMobs(level);
                clear(level, -12, Y - 1, -12, 12, Y + 12, 24, Blocks.SMOOTH_STONE.defaultBlockState());
                view(p, 0.5 + dx, Y + eyeY, 0.5 + dz, 0.5, Y + lookY, 0.5);
            });
            mannequin(type, 0.5, Y, 0.5, yaw, setup::accept);
        });
        shot("portrait_" + name);
    }

    private static void featuredCreatures() {
        // The Knocker at a door: mid-knock in profile, then from behind, lunging, and its face.
        step(40, () -> {
            clearMannequins();
            server((srv, p) -> {
                ServerLevel level = p.serverLevel();
                killMobs(level);
                clear(level, -12, Y - 1, -12, 12, Y + 12, 24, Blocks.SMOOTH_STONE.defaultBlockState());
                fill(level, -2, Y, 3, 2, Y + 3, 3, Blocks.SPRUCE_PLANKS.defaultBlockState());
                BlockState lower = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
                level.setBlock(new BlockPos(0, Y, 3), lower, Block.UPDATE_ALL);
                level.setBlock(new BlockPos(0, Y + 1, 3), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
                view(p, -3.2, Y + 1.9, 2.2, 0.5, Y + 1.7, 2.8);
            });
            // Where a real one stands: REACH from the door panel (which sits on the south edge of its block).
            mannequin(ModEntities.KNOCKER.get(), 0.5, Y, 3.8125 - (KnockerEntity.REACH + 0.03), 0F, k -> data(k, "STATE", KnockerEntity.KNOCKING));
        });
        // Photographed just after the first blow lands.
        step(3, () -> knockerGesture(KnockerEntity.GESTURE_KNOCK));
        shot("featured_knocker_knocking");
        step(10, () -> server((srv, p) -> view(p, 2.2, Y + 2.3, -1.6, 0.5, Y + 1.5, 2.2)));
        shot("featured_knocker_listening_behind");
        step(20, () -> {
            clearMannequins();
            server((srv, p) -> {
                clear(p.serverLevel(), -12, Y - 1, -12, 12, Y + 12, 24, Blocks.SMOOTH_STONE.defaultBlockState());
                view(p, 1.4, Y + 2.0, -2.6, 0.5, Y + 1.5, 0.5);
            });
            mannequin(ModEntities.KNOCKER.get(), 0.5, Y, 0.5, 200F, k -> data(k, "STATE", KnockerEntity.LUNGE));
        });
        shot("featured_knocker_lunge");
        step(10, () -> {
            MANNEQUINS.forEach(k -> data(k, "STATE", 0));
            server((srv, p) -> view(p, 1.1, Y + 2.4, -1.4, 0.5, Y + 2.1, 0.5));
        });
        shot("featured_knocker_face");
        step(10, () -> server((srv, p) -> view(p, -2.6, Y + 1.9, 2.4, 0.5, Y + 1.4, 0.5)));
        shot("featured_knocker_back");

        // The Longhand: every frozen pose in a row, then each one close, then unwatched.
        step(20, () -> {
            clearMannequins();
            server((srv, p) -> view(p, 0.5, Y + 2.4, -6.8, 0.5, Y + 1.8, 0.5));
            for (int i = 0; i < LonghandEntity.POSES; i++) {
                int pose = i;
                mannequin(ModEntities.LONGHAND.get(), 0.5 + (i - 2) * 2.7, Y, 0.5, 180F, l -> {
                    data(l, "FROZEN", true);
                    data(l, "POSE_INDEX", pose);
                });
            }
        });
        shot("featured_longhand_poses");
        for (int i = 0; i < LonghandEntity.POSES; i++) {
            int pose = i;
            step(10, () -> {
                clearMannequins();
                server((srv, p) -> view(p, 2.3, Y + 2.6, -2.7, 0.5, Y + 2.0, 0.5));
                mannequin(ModEntities.LONGHAND.get(), 0.5, Y, 0.5, 195F, l -> {
                    data(l, "FROZEN", true);
                    data(l, "POSE_INDEX", pose);
                });
            });
            shot("featured_longhand_pose" + pose);
        }
        step(20, () -> {
            clearMannequins();
            mannequin(ModEntities.LONGHAND.get(), 0.5, Y, 0.5, 160F, l -> data(l, "FROZEN", false));
        });
        shot("featured_longhand_unwatched");
        step(10, () -> server((srv, p) -> view(p, -1.8, Y + 2.6, 3.4, 0.5, Y + 1.8, 0.5)));
        shot("featured_longhand_back");

        // The Copyist: pretending, all four skins from the front and back, a close look, and a reveal.
        step(20, () -> {
            clearMannequins();
            server((srv, p) -> view(p, 2.0, Y + 1.9, -2.6, 0.5, Y + 0.8, 0.5));
            mannequin(ModEntities.COPYIST.get(), 0.5, Y, 0.5, 200F, c -> {
                data(c, "HIDE", CopyistEntity.COW);
                data(c, "DISGUISE", CopyistEntity.COW);
            });
        });
        shot("featured_copyist_disguised");
        step(20, () -> {
            clearMannequins();
            server((srv, p) -> view(p, 0.5, Y + 2.4, -5.6, 0.5, Y + 1.2, 0.5));
            for (int hide = 0; hide < 4; hide++) {
                int h = hide;
                mannequin(ModEntities.COPYIST.get(), 0.5 + (hide - 1.5) * 2.3, Y, 0.5, 190F, c -> data(c, "HIDE", h));
            }
        });
        shot("featured_copyist_skins");
        step(10, () -> {
            for (Entity c : MANNEQUINS) turn(c, 10F);
            server((srv, p) -> view(p, 0.5, Y + 2.4, -5.6, 0.5, Y + 1.2, 0.5));
        });
        shot("featured_copyist_skins_back");
        step(10, () -> {
            clearMannequins();
            server((srv, p) -> view(p, 1.6, Y + 2.1, -1.8, 0.5, Y + 1.5, 0.5));
            mannequin(ModEntities.COPYIST.get(), 0.5, Y, 0.5, 205F, c -> data(c, "HIDE", CopyistEntity.COW));
        });
        shot("featured_copyist_close");
        step(10, () -> server((srv, p) -> view(p, 3.0, Y + 1.8, 1.6, 0.5, Y + 1.1, 0.5)));
        shot("featured_copyist_side");
        step(30, () -> {
            clearMannequins();
            server((srv, p) -> view(p, 1.8, Y + 2.2, -3.2, 0.5, Y + 1.0, 0.5));
            mannequin(ModEntities.COPYIST.get(), 0.5, Y, 0.5, 200F, c -> {
                data(c, "HIDE", CopyistEntity.PIG);
                data(c, "DISGUISE", CopyistEntity.PIG);
            });
        });
        // Taken about half-way through the unfolding.
        step(7, () -> MANNEQUINS.forEach(c -> data(c, "DISGUISE", CopyistEntity.REVEALED)));
        shot("featured_copyist_revealing");

        // The Erratum: still among real blocks (the eye opens when you look slightly away), then walking.
        step(20, () -> {
            clearMannequins();
            server((srv, p) -> {
                ServerLevel level = p.serverLevel();
                level.setBlock(new BlockPos(-1, Y, 0), Blocks.BOOKSHELF.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(new BlockPos(1, Y, 0), Blocks.BOOKSHELF.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(new BlockPos(1, Y, 1), Blocks.BOOKSHELF.defaultBlockState(), Block.UPDATE_ALL);
                view(p, 0.5, Y + 1.3, -2.4, -1.4, Y + 0.5, 0.5);
            });
            mannequin(ModEntities.ERRATUM.get(), 0.5, Y, 0.5, 0F, e -> data(e, "MIMIC", Blocks.BOOKSHELF.defaultBlockState()));
        });
        step(30, () -> {});
        shot("featured_erratum_eye");
        step(10, () -> server((srv, p) -> view(p, 0.5, Y + 1.3, -2.4, 0.5, Y + 0.5, 0.5)));
        shot("featured_erratum_stared_at");
        step(30, () -> {
            server((srv, p) -> {
                clear(p.serverLevel(), -12, Y - 1, -12, 12, Y + 12, 24, Blocks.SMOOTH_STONE.defaultBlockState());
                view(p, 2.3, Y + 1.1, -1.7, 0.5, Y + 0.45, 0.5);
            });
            MANNEQUINS.forEach(e -> data(e, "MOVING", true));
        });
        shot("featured_erratum_walking");
        // From a pit in the floor, to look up under the block at the mouth.
        step(10, () -> server((srv, p) -> {
            fill(p.serverLevel(), 0, Y - 3, -2, 0, Y - 1, -2, Blocks.AIR.defaultBlockState());
            view(p, 0.5, Y + 0.08, -1.5, 0.5, Y + 0.3, 0.5);
        }));
        shot("featured_erratum_underneath");

        // Squeezing through gaps: each one crawling out of a one-block hole in a wall, then the Knocker
        // and the Longhand bent double through a doorway, seen as a player standing in the room would.
        for (EntityType<?> type : List.of(ModEntities.KNOCKER.get(), ModEntities.LONGHAND.get(), ModEntities.COPYIST.get())) {
            String name = ForgeRegistries.ENTITY_TYPES.getKey(type).getPath();
            step(20, () -> {
                clearMannequins();
                server((srv, p) -> {
                    ServerLevel level = p.serverLevel();
                    clear(level, -12, Y - 1, -12, 12, Y + 12, 24, Blocks.SMOOTH_STONE.defaultBlockState());
                    fill(level, -4, Y, 3, 4, Y + 3, 3, Blocks.STONE_BRICKS.defaultBlockState());
                    level.setBlock(new BlockPos(0, Y, 3), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    view(p, 0.9, Y + 1.62, 0.1, 0.5, Y + 0.3, 3.0);
                });
                mannequin(type, 0.5, Y, 3.5, 180F, e -> e.setPose(Squeeze.CRAWL));
            });
            step(20, () -> {});
            shot("squeeze_" + name + "_hole");
            if (type == ModEntities.COPYIST.get()) continue;
            step(20, () -> {
                clearMannequins();
                server((srv, p) -> {
                    ServerLevel level = p.serverLevel();
                    level.setBlock(new BlockPos(0, Y + 1, 3), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    view(p, 2.0, Y + 1.62, -0.6, 0.5, Y + 0.9, 3.0);
                });
                mannequin(type, 0.5, Y, 3.3, 180F, e -> e.setPose(Squeeze.STOOP));
            });
            step(20, () -> {});
            shot("squeeze_" + name + "_doorway");
        }

        // A Knocker that got no answer, bent down at the window to look in: by day, then as it would
        // really be met, at night without night vision.
        step(20, () -> {
            clearMannequins();
            server((srv, p) -> {
                ServerLevel level = p.serverLevel();
                clear(level, -12, Y - 1, -12, 12, Y + 12, 24, Blocks.SMOOTH_STONE.defaultBlockState());
                fill(level, -4, Y, 3, 5, Y + 3, 3, Blocks.SPRUCE_PLANKS.defaultBlockState());
                fill(level, 0, Y + 1, 3, 1, Y + 2, 3, Blocks.GLASS.defaultBlockState());
                view(p, 1.0, Y + 1.62, 0.4, 1.0, Y + 1.6, 3.0);
            });
            mannequin(ModEntities.KNOCKER.get(), 1.0, Y, 4.0 + KnockerEntity.REACH_STOOPED + 0.03, 180F, k -> {
                data(k, "STATE", KnockerEntity.SEARCHING);
                k.setPose(Squeeze.STOOP);
            });
        });
        step(20, () -> {});
        shot("knocker_window");
        // Half way through dragging its nails down the glass.
        step(40, () -> knockerGesture(KnockerEntity.GESTURE_SCRATCH));
        shot("knocker_window_scratch");
        step(60, () -> server((srv, p) -> {
            command(srv, p, "time set 18000");
            p.removeEffect(MobEffects.NIGHT_VISION);
        }));
        shot("knocker_window_night");
        step(10, () -> server((srv, p) -> {
            command(srv, p, "time set 6000");
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1_000_000, 0, false, false));
        }));
        step(10, SmokeTest::clearMannequins);
    }

    /** Adds a creature to this client's world only, facing {@code yaw}, and lets {@code pose} set it up. Returns it (null if there is no world). */
    static <T extends Entity> T mannequin(EntityType<T> type, double x, double y, double z, float yaw, Consumer<T> pose) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;
        T e = type.create(level);
        if (e == null) return null;
        e.setId(nextMannequinId++);
        e.moveTo(x, y, z, yaw, 0F);
        turn(e, yaw);
        if (e instanceof Mob mob) mob.setNoAi(true);
        pose.accept(e);
        level.putNonPlayerEntity(e.getId(), e);
        MANNEQUINS.add(e);
        return e;
    }

    /** Starts a gesture on every posed Knocker. */
    static void knockerGesture(int kind) {
        MANNEQUINS.forEach(e -> {
            if (e instanceof KnockerEntity k) k.playGesture(kind);
        });
    }

    static void turn(Entity e, float yaw) {
        e.setYRot(yaw);
        e.yRotO = yaw;
        if (e instanceof LivingEntity living) {
            living.yBodyRot = living.yBodyRotO = yaw;
            living.yHeadRot = living.yHeadRotO = yaw;
        }
    }

    static void clearMannequins() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) MANNEQUINS.forEach(e -> level.removeEntity(e.getId(), Entity.RemovalReason.DISCARDED));
        MANNEQUINS.clear();
    }

    /** Sets one of a creature's synced values by the name of its (private) accessor field. */
    @SuppressWarnings("unchecked")
    static <V> void data(Entity e, String field, V value) {
        try {
            Field f = e.getClass().getDeclaredField(field);
            f.setAccessible(true);
            e.getEntityData().set((EntityDataAccessor<V>) f.get(null), value);
        } catch (ReflectiveOperationException | ClassCastException ex) {
            LOG.error("[smoke] SMOKE_FAIL cannot set {} on {}", field, e.getClass().getSimpleName(), ex);
        }
    }

    /** Puts the camera (the player's eyes) at the given point, looking at another. */
    private static void view(ServerPlayer p, double ex, double ey, double ez, double tx, double ty, double tz) {
        double dx = tx - ex, dy = ty - ey, dz = tz - ez;
        float yaw = (float) Math.toDegrees(Mth.atan2(dz, dx)) - 90F;
        float pitch = (float) -Math.toDegrees(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        moveTo(p, p.serverLevel(), ex, ey - p.getEyeHeight(), ez, yaw, pitch);
    }

    // ------------------------------------------------------------------ helpers

    /** Events with something to look at: a screenshot is taken shortly after each fires. */
    private static final List<String> VISUAL_EVENTS = List.of("fog_bank", "other_page", "vignette_face", "fading", "ink_rain",
            "lights_out", "tear_opens", "sky_glimpse", "palehand", "fair_copy", "scraping", "longhand_sighting", "stag_glimpse");

    private static void fireEvent(ServerPlayer p, HorrorEvent event) {
        try {
            boolean fired = HorrorDirector.fire(event, new EventContext(p, p.serverLevel(), BleedCapability.get(p)));
            LOG.info("[smoke] event {} -> {}", event.id, fired ? "fired" : "nothing suitable nearby");
        } catch (Throwable t) {
            LOG.error("[smoke] SMOKE_FAIL event {} threw", event.id, t);
        }
    }

    /** A doorway, torches, a sign, a chest and a few cows around the player, for events to use. */
    private static void furnishForEvents(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BlockPos base = p.blockPosition();
        fill(level, base.getX() - 4, base.getY() - 1, base.getZ() - 4, base.getX() + 4, base.getY() - 1, base.getZ() + 4,
                Blocks.COBBLESTONE.defaultBlockState());
        fill(level, base.getX() - 4, base.getY(), base.getZ() - 4, base.getX() + 4, base.getY() + 3, base.getZ() + 4,
                Blocks.AIR.defaultBlockState());
        BlockPos door = base.offset(0, 0, 3);
        fill(level, door.getX() - 1, door.getY(), door.getZ(), door.getX() + 1, door.getY() + 2, door.getZ(), Blocks.OAK_PLANKS.defaultBlockState());
        BlockState lower = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        level.setBlock(door, lower, Block.UPDATE_ALL);
        level.setBlock(door.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        level.setBlock(base.offset(2, 0, 0), Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base.offset(-2, 0, 0), Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base.offset(2, 0, 2), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(base.offset(2, 0, 2)) instanceof net.minecraft.world.Container chest) {
            chest.setItem(0, new ItemStack(net.minecraft.world.item.Items.BREAD, 5));
            chest.setItem(4, new ItemStack(net.minecraft.world.item.Items.TORCH, 12));
            chest.setItem(13, new ItemStack(net.minecraft.world.item.Items.PAPER, 3));
        }
        level.setBlock(base.offset(-2, 0, 2), Blocks.OAK_SIGN.defaultBlockState(), Block.UPDATE_ALL);
        for (int i = 0; i < 3; i++) {
            Entity cow = EntityType.COW.create(level);
            if (cow == null) continue;
            cow.moveTo(base.getX() + 3.5, base.getY(), base.getZ() - 3.5 + i * 2, 0F, 0F);
            level.addFreshEntity(cow);
        }
        // Face away from the doorway, out over open ground: effects are easier to see and the door is out of view.
        moveTo(p, level, base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 180F, 0F);
        LOG.info("[smoke] furnished {} for events", base);
    }

    private static void step(int delay, Runnable action) {
        STEPS.add(new Step(delay, action));
    }

    /** A screenshot at once, for moments that will not wait for the usual pause. */
    private static void grabNow(String name) {
        Minecraft mc = Minecraft.getInstance();
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "smoke_" + name + ".png", mc.getMainRenderTarget(), msg -> {});
        LOG.info("[smoke] screenshot {}", name);
    }

    private static void shot(String name) {
        step(2, () -> Minecraft.getInstance().getToasts().clear());
        step(30, () -> {
            Minecraft mc = Minecraft.getInstance();
            Screenshot.grab(mc.gameDirectory, "smoke_" + name + ".png", mc.getMainRenderTarget(), msg -> {});
            LOG.info("[smoke] screenshot {}", name);
        });
    }

    interface ServerAction {
        void run(MinecraftServer server, ServerPlayer player);
    }

    static void server(ServerAction action) {
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

    static void command(MinecraftServer srv, ServerPlayer p, String command) {
        srv.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), command);
    }

    /** Teleports and keeps the player flying (vanilla cancels creative flight on touching ground). */
    static void moveTo(ServerPlayer p, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        p.teleportTo(level, x, y, z, yaw, pitch);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
    }

    static BlockPos findBiome(ServerLevel level, TagKey<Biome> tag, BlockPos from) {
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

    static void killMobs(ServerLevel level) {
        List<Entity> doomed = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof Mob || e instanceof ItemFrame) doomed.add(e);
        }
        doomed.forEach(Entity::discard);
    }

    static void clear(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1, BlockState floor) {
        fill(level, x0, y0 + 1, z0, x1, y1, z1, Blocks.AIR.defaultBlockState());
        fill(level, x0, y0, z0, x1, y0, z1, floor);
    }

    static void fill(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (BlockPos pos : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
            level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }
}
