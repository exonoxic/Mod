package com.exonoxic.palimpsest.client.dev;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.entity.CopyistEntity;
import com.exonoxic.palimpsest.entity.InkhoundEntity;
import com.exonoxic.palimpsest.entity.KnockerEntity;
import com.exonoxic.palimpsest.entity.RedactedEntity;
import com.exonoxic.palimpsest.entity.ai.Squeeze;
import com.exonoxic.palimpsest.registry.ModEntities;
import com.exonoxic.palimpsest.world.ModStructures;
import com.exonoxic.palimpsest.world.dimension.ModDimensions;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

import static com.exonoxic.palimpsest.client.dev.SmokeTest.clear;
import static com.exonoxic.palimpsest.client.dev.SmokeTest.clearMannequins;
import static com.exonoxic.palimpsest.client.dev.SmokeTest.command;
import static com.exonoxic.palimpsest.client.dev.SmokeTest.data;
import static com.exonoxic.palimpsest.client.dev.SmokeTest.fill;
import static com.exonoxic.palimpsest.client.dev.SmokeTest.findBiome;
import static com.exonoxic.palimpsest.client.dev.SmokeTest.killMobs;
import static com.exonoxic.palimpsest.client.dev.SmokeTest.mannequin;
import static com.exonoxic.palimpsest.client.dev.SmokeTest.moveTo;
import static com.exonoxic.palimpsest.client.dev.SmokeTest.server;

/**
 * Development-only trailer. Inert unless the game is started with {@code -Dpalimpsest.filmTrailer=true}
 * (the Trailer workflow does this under a virtual display, then runs {@code tools/trailer_mix.py}).
 * <p>
 * It makes a world, clears a glade in a forest at night and films a script of shots there and in
 * the Undertext: posed creatures (added to this client only, so nothing walks them out of their
 * marks) under a moving camera, and the real thing hunting the player where the behaviour is the
 * point (a Knocker that got no answer, a Redacted that stares before it comes, an Inkhound that
 * swims). {@link TrailerRecorder} turns what is drawn into video and logs what is heard.
 */
@Mod.EventBusSubscriber(modid = Palimpsest.MODID, value = Dist.CLIENT)
public final class Trailer {
    private static final Logger LOG = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("palimpsest.filmTrailer");
    private static final String WORLD = "palimpsest_trailer";
    private static final int GLADE = 14;
    private static final int NIGHT = 18000;

    /** One shot: set up, left to settle unfilmed, then filmed for {@code length} ticks (or until {@code until}). */
    private static final class Shot {
        final String name;
        int length;
        int preroll = 30;
        int maxWait = 0;
        Runnable setup = () -> {};
        BooleanSupplier ready = () -> true;
        BooleanSupplier until = () -> false;
        IntConsumer each = t -> {};
        Camera camera;
        boolean card;

        Shot(String name, int length) {
            this.name = name;
            this.length = length;
        }

        /** Adds to the setup. */
        Shot then(Runnable more) {
            Runnable first = setup;
            setup = () -> {
                first.run();
                more.run();
            };
            return this;
        }
    }

    /** Where the camera is and what it looks at, {@code u} running 0 to 1 over the shot. */
    private interface Camera {
        /** Returns {eye, target}; a null eye leaves the player where they stand and only turns their head. */
        Vec3[] at(int t, float u);
    }

    private static final List<Shot> SHOTS = new ArrayList<>();
    private static int clientTicks;
    private static boolean worldRequested;
    private static boolean installed;
    private static int shotIndex;
    private static Shot current;
    private static boolean filming;
    private static int phaseTicks;
    private static Vec3 lookNow;

    /** The glade: the block at its centre, on the ground. Set on the server thread. */
    private static volatile BlockPos base;
    /** Open ground in the Undertext, for the bosses. */
    private static volatile BlockPos under;
    /** What an Undertext shot circles: {focus, (radius, 0, 0)}. */
    private static volatile Vec3[] sight;

    private Trailer() {}

    // ------------------------------------------------------------------ driving

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        clientTicks++;
        if (!worldRequested) {
            if (clientTicks > 100 && mc.getOverlay() == null) createWorld(mc);
            return;
        }
        if (mc.level == null || mc.player == null) return;
        if (!installed) {
            installed = true;
            try {
                TrailerRecorder.install(mc);
            } catch (Exception e) {
                LOG.error("[trailer] TRAILER_FAIL cannot install the recorder", e);
                mc.stop();
                return;
            }
            buildScript();
        }
        try {
            advance(mc);
        } catch (Throwable t) {
            LOG.error("[trailer] TRAILER_FAIL in shot {}", current == null ? "-" : current.name, t);
            current = null;
            TrailerRecorder.record(false);
        }
    }

    private static void advance(Minecraft mc) {
        if (current == null) {
            if (shotIndex >= SHOTS.size()) {
                if (shotIndex++ == SHOTS.size()) {
                    TrailerRecorder.finish();
                    LOG.info("[trailer] PALIMPSEST TRAILER COMPLETE");
                } else if (shotIndex > SHOTS.size() + 20) {
                    mc.stop();
                }
                return;
            }
            current = SHOTS.get(shotIndex++);
            filming = false;
            phaseTicks = 0;
            lookNow = null;
            LOG.info("[trailer] shot {} ({})", current.name, shotIndex);
            if (!current.card && mc.screen instanceof TitleCardScreen) mc.setScreen(null);
            current.setup.run();
            return;
        }
        Shot shot = current;
        if (!filming) {
            place(mc, shot, 0, true);
            phaseTicks++;
            boolean waited = phaseTicks >= shot.preroll;
            if (waited && (shot.ready.getAsBoolean() || phaseTicks >= shot.preroll + shot.maxWait)) {
                if (!shot.ready.getAsBoolean()) LOG.warn("[trailer] shot {} filmed without its cue after {} ticks", shot.name, phaseTicks);
                if (shot.length <= 0) {
                    current = null;
                    return;
                }
                filming = true;
                phaseTicks = 0;
                TrailerRecorder.record(true);
                shot.each.accept(0);
            }
            return;
        }
        int t = ++phaseTicks;
        place(mc, shot, t, false);
        shot.each.accept(t);
        if (t >= shot.length || shot.until.getAsBoolean()) {
            LOG.info("[trailer] shot {} filmed, {} ticks, {} frames so far", shot.name, t, TrailerRecorder.frames());
            TrailerRecorder.record(false);
            current = null;
        }
    }

    /** Moves the camera (the local player) for tick {@code t} of the shot. */
    private static void place(Minecraft mc, Shot shot, int t, boolean cut) {
        LocalPlayer p = mc.player;
        if (shot.camera == null || p == null) return;
        Vec3[] view = shot.camera.at(t, Mth.clamp(t / (float) Math.max(1, shot.length), 0.0F, 1.0F));
        if (view == null) return;
        Vec3 eye = view[0] != null ? view[0] : p.getEyePosition();
        Vec3 look = view[1];
        if (view.length > 2 && lookNow != null && !cut) look = lookNow.lerp(look, 0.22D);
        lookNow = look;
        double dx = look.x - eye.x, dy = look.y - eye.y, dz = look.z - eye.z;
        float yaw = (float) Math.toDegrees(Mth.atan2(dz, dx)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        yaw = p.getYRot() + Mth.wrapDegrees(yaw - p.getYRot());
        if (view[0] != null) {
            p.setPos(eye.x, eye.y - p.getEyeHeight(), eye.z);
            p.setDeltaMovement(Vec3.ZERO);
        }
        p.setYRot(yaw);
        p.setXRot(pitch);
        p.setYHeadRot(yaw);
        p.yBodyRot = yaw;
        if (cut) {
            p.setOldPosAndRot();
            p.yHeadRotO = yaw;
            p.yBodyRotO = yaw;
        }
    }

    private static void createWorld(Minecraft mc) {
        worldRequested = true;
        mc.options.pauseOnLostFocus = false;
        mc.options.hideGui = true;
        mc.options.renderDistance().set(8);
        mc.options.framerateLimit().set(260);
        mc.options.enableVsync().set(false);
        mc.options.cloudStatus().set(CloudStatus.OFF);
        mc.options.fov().set(70);
        mc.options.fovEffectScale().set(0.0D);
        mc.options.bobView().set(false);
        mc.options.gamma().set(1.0D);
        mc.getTutorial().setStep(TutorialSteps.NONE);
        try {
            FileUtils.deleteDirectory(mc.getLevelSource().getBaseDir().resolve(WORLD).toFile());
        } catch (Exception e) {
            LOG.warn("[trailer] could not clear old world", e);
        }
        LOG.info("[trailer] creating world");
        LevelSettings settings = new LevelSettings("Palimpsest trailer", GameType.CREATIVE, false, Difficulty.NORMAL,
                true, new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(1453L, true, false),
                WorldPresets::createNormalWorldDimensions);
    }

    // ------------------------------------------------------------------ the script

    private static void buildScript() {
        // The glade: flattened in the nearest forest, with a hut in it.
        Shot prepare = shot("prepare", 0);
        prepare.preroll = 200;
        prepare.maxWait = 400;
        prepare.setup = () -> server((srv, p) -> {
            command(srv, p, "gamerule doDaylightCycle false");
            command(srv, p, "gamerule doMobSpawning false");
            command(srv, p, "gamerule doWeatherCycle false");
            command(srv, p, "gamerule doInsomnia false");
            command(srv, p, "gamerule announceAdvancements false");
            command(srv, p, "weather clear");
            command(srv, p, "time set " + NIGHT);
            ServerLevel level = p.serverLevel();
            BlockPos land = findBiome(level, BiomeTags.IS_FOREST, p.blockPosition());
            level.getChunk(land.getX() >> 4, land.getZ() >> 4);
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, land);
            p.setGameMode(GameType.SPECTATOR);
            moveTo(p, level, ground.getX() + 0.5, ground.getY() + 4, ground.getZ() + 12.5, 180F, 10F);
            base = ground;
            glade(level);
            hut(level, true);
            LOG.info("[trailer] glade at {}", ground);
        });
        prepare.ready = () -> {
            if (base == null) return false;
            if (SHOTS.size() == 1) buildShots();
            return true;
        };
    }

    private static void buildShots() {
        // "This world was written over another." Then, in the dark, three knocks.
        card("open", 100, 72, 2, false, "This world was written over another.").each = t -> {
            if (t == 1) TrailerRecorder.cue("music music_undertext 0.30");
            // One sound, three knocks.
            if (t == 70) ui("event.knock", 0.9F, 1.0F);
        };

        // Outside: the hut in the glade, a light in the window, and something at the door.
        Shot wide = shot("knocker_wide", 110);
        wide.setup = () -> {
            clearMannequins();
            mannequin(ModEntities.KNOCKER.get(), x(0.5), y(0), z(4.0 + KnockerEntity.REACH + 0.03), 180F, k -> data(k, "STATE", KnockerEntity.KNOCKING));
        };
        wide.camera = dolly(v(8.0, 2.8, 13.5), v(4.6, 2.0, 9.6), v(0.5, 1.4, 4.4), v(0.5, 1.6, 4.3));
        wide.each = t -> knockAt(t, 18, 82);

        Shot profile = shot("knocker_profile", 64);
        profile.camera = dolly(v(-3.6, 1.9, 6.4), v(-2.5, 1.8, 5.7), v(0.5, 1.8, 4.4), v(0.5, 1.7, 4.3));
        profile.each = t -> knockAt(t, 10);

        // Inside, later: nobody answered, so it has come round to the window.
        Shot window = shot("knocker_window", 140);
        window.setup = () -> {
            clearMannequins();
            mannequin(ModEntities.KNOCKER.get(), x(-1.0), y(0), z(4.0 + KnockerEntity.REACH_STOOPED + 0.03), 180F, k -> {
                data(k, "STATE", KnockerEntity.SEARCHING);
                k.setPose(Squeeze.STOOP);
            });
        };
        window.camera = dolly(v(-1.0, 1.62, -1.2), v(-1.0, 1.62, 1.1), v(-1.0, 1.6, 3.0), v(-1.0, 1.65, 3.0));
        window.each = t -> {
            // Slow taps on the glass, then its nails dragged down it (each gesture starts a moment before its sound).
            if (t == 16) SmokeTest.knockerGesture(KnockerEntity.GESTURE_GLASS_TAP);
            if (t == 16 + KnockerEntity.GESTURE_LEAD) sound("entity.knocker.glass_tap", v(-1.0, 1.8, 3.0), 1.2F, 1.0F);
            if (t == 74) SmokeTest.knockerGesture(KnockerEntity.GESTURE_SCRATCH);
            if (t == 74 + KnockerEntity.GESTURE_LEAD) sound("entity.knocker.glass_scratch", v(-1.0, 1.7, 3.0), 1.2F, 1.0F);
        };

        // Later still: back at the door, with its nails, until the door comes down.
        Shot breakIn = shot("knocker_breaks_in", 140);
        breakIn.setup = () -> {
            clearMannequins();
            MannequinAnim.subject = mannequin(ModEntities.KNOCKER.get(), x(0.5), y(0), z(4.0 + KnockerEntity.REACH + 0.03), 180F,
                    k -> data(k, "STATE", KnockerEntity.SEARCHING));
        };
        breakIn.camera = dolly(v(-3.6, 2.0, 6.8), v(-2.1, 1.8, 5.5), v(0.5, 1.6, 4.0), v(0.5, 1.5, 3.6));
        breakIn.each = t -> {
            if (t == 4 || t == 64) SmokeTest.knockerGesture(KnockerEntity.GESTURE_SCRATCH);
            if (t == 4 + KnockerEntity.GESTURE_LEAD || t == 64 + KnockerEntity.GESTURE_LEAD) sound("entity.knocker.scratch", v(0.5, 1.4, 3.6), 1.6F, 0.95F);
            Minecraft mc = Minecraft.getInstance();
            if ((t == 40 || t == 95) && mc.level != null) {
                mc.level.playLocalSound(x(0.5), y(1.0), z(3.5), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 1.2F, 0.6F, false);
            }
            // The cracks spread as they do on any block being broken; then it gives.
            if (t >= 10 && t < 118 && (t - 10) % 12 == 0) {
                int stage = (t - 10) / 12;
                server((srv, p) -> {
                    p.serverLevel().destroyBlockProgress(-7001, at(0, 0, 3), stage);
                    p.serverLevel().destroyBlockProgress(-7002, at(0, 1, 3), stage);
                });
            }
            if (t == 118) {
                server((srv, p) -> {
                    p.serverLevel().destroyBlock(at(0, 0, 3), false);
                    p.serverLevel().playSound(null, at(0, 1, 3), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, 0.7F);
                });
                if (MannequinAnim.subject != null) data(MannequinAnim.subject, "STATE", KnockerEntity.LUNGE);
                sound("entity.knocker.lunge", v(0.5, 1.6, 4.5), 1.6F, 0.95F);
            }
        };

        card("dont_answer", 56, 56, 3, false, "When it knocks,", "don't answer.").each = t -> {
            if (t == 1) ui("event.stinger", 0.8F, 0.7F);
        };

        // The real thing: a Knocker at the door, the player inside, a one-block gap in the back wall.
        Shot door = shot("hunt_door", 76);
        door.preroll = 6;
        door.setup = () -> {
            clearMannequins();
            server((srv, p) -> {
                ServerLevel level = p.serverLevel();
                hut(level, false);
                p.setGameMode(GameType.SURVIVAL);
                p.getAbilities().invulnerable = true;
                p.onUpdateAbilities();
                moveTo(p, level, x(0.5), y(0), z(0.9), 0F, 0F);
                p.getAbilities().flying = false;
                p.onUpdateAbilities();
                if (!KnockerEntity.spawnAtDoor(level, at(0, 0, 3), p)) {
                    LOG.error("[trailer] TRAILER_FAIL the Knocker would not come to the door");
                    return;
                }
                for (KnockerEntity k : level.getEntitiesOfClass(KnockerEntity.class, new AABB(at(0, 0, 0)).inflate(16))) {
                    CompoundTag tag = new CompoundTag();
                    k.saveWithoutId(tag);
                    tag.putInt("Rounds", 2);
                    k.load(tag);
                }
            });
        };
        door.camera = (t, u) -> new Vec3[]{null, v(0.5 + 0.15 * u, 1.25, 3.0)};

        Shot gap = shot("hunt_gap", 64);
        gap.preroll = 20;
        gap.maxWait = 3600;
        gap.ready = () -> {
            KnockerEntity k = knocker();
            return k != null && Math.abs(k.getX() - x(0.5)) < 1.2D && k.getZ() > z(-4.6) && k.getZ() < z(-2.0);
        };
        gap.camera = (t, u) -> new Vec3[]{null, v(0.5, 0.45 + 0.1 * u, -3.0)};

        // In the room with you it stands and watches first; then it comes.
        Shot lunge = shot("hunt_lunge", 220);
        lunge.preroll = 0;
        lunge.camera = (t, u) -> {
            KnockerEntity k = knocker();
            return new Vec3[]{null, k == null ? v(0.5, 0.6, -2.0) : k.getEyePosition(), null};
        };
        lunge.until = () -> {
            KnockerEntity k = knocker();
            LocalPlayer me = Minecraft.getInstance().player;
            return k != null && me != null && k.distanceTo(me) < 2.6F;
        };

        Shot notAlone = card("not_alone", 60, 60, 2, false, "It is not the only one.").then(() -> server((srv, p) -> {
            killMobs(p.serverLevel());
            p.setGameMode(GameType.SPECTATOR);
            glade(p.serverLevel());
        }));
        notAlone.each = t -> {
            if (t == 1) ui("event.stinger", 1.0F, 0.9F);
        };

        // The Longhand only moves while nobody is looking at it.
        Shot longhand = shot("longhand_look_away", 200);
        longhand.setup = () -> {
            clearMannequins();
            server((srv, p) -> {
                ServerLevel level = p.serverLevel();
                level.setBlock(at(-1, 0, -9), Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(at(2, 0, -2), Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(at(2, 0, 4), Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
            });
            MannequinAnim.subject = mannequin(ModEntities.LONGHAND.get(), x(0.5), y(0), z(-10.0), 0F, l -> {
                data(l, "FROZEN", true);
                data(l, "POSE_INDEX", 0);
            });
        };
        Vec3 lhEye = v(0.5, 1.62, 6.0);
        longhand.camera = (t, u) -> {
            float away = t < 35 ? 0 : t < 55 ? ease((t - 35) / 20F) : t < 75 ? 1 - ease((t - 55) / 20F)
                    : t < 110 ? 0 : t < 128 ? ease((t - 110) / 18F) : t < 140 ? 1 - ease((t - 128) / 12F) : 0;
            double yaw = Math.toRadians(180 + 110 * away);
            double pitch = t >= 140 ? 0.55 : t >= 75 ? 0.1 : 0.05;
            return new Vec3[]{lhEye, lhEye.add(-Math.sin(yaw) * 10, pitch * 10 * (t >= 140 ? 1 : 0.4), Math.cos(yaw) * 10)};
        };
        longhand.each = t -> {
            Entity l = MannequinAnim.subject;
            if (l == null) return;
            if (t == 58) {
                l.moveTo(x(0.4), y(0), z(-1.5), 5F, 0F);
                SmokeTest.turn(l, 5F);
                data(l, "POSE_INDEX", 2);
                sound("entity.longhand.move", v(0.4, 1.0, -1.5), 1.0F, 1.0F);
            }
            if (t == 130) {
                l.moveTo(x(0.5), y(0), z(4.2), 0F, 0F);
                SmokeTest.turn(l, 0F);
                data(l, "POSE_INDEX", 4);
                sound("entity.longhand.move", v(0.5, 1.0, 4.2), 1.2F, 0.8F);
            }
            if (t == 141) ui("event.stinger", 1.0F, 1.0F);
        };

        // The Copyist, at dusk, among the cows.
        Shot copyist = shot("copyist_reveal", 120);
        copyist.setup = () -> {
            clearMannequins();
            server((srv, p) -> command(srv, p, "time set 12900"));
            mannequin(EntityType.COW, x(-2.6), y(0), z(1.8), 140F, c -> {});
            mannequin(EntityType.COW, x(3.6), y(0), z(-0.8), 230F, c -> {});
            MannequinAnim.subject = mannequin(ModEntities.COPYIST.get(), x(0.5), y(0), z(0.5), 200F, c -> {
                data(c, "HIDE", CopyistEntity.COW);
                data(c, "DISGUISE", CopyistEntity.COW);
            });
        };
        copyist.camera = dolly(v(3.4, 2.0, -6.4), v(1.9, 1.8, -2.8), v(0.5, 0.9, 0.5), v(0.5, 1.2, 0.5));
        copyist.each = t -> {
            if (t == 62 && MannequinAnim.subject != null) {
                data(MannequinAnim.subject, "DISGUISE", CopyistEntity.REVEALED);
                sound("entity.copyist.reveal", v(0.5, 1.0, 0.5), 1.4F, 1.0F);
            }
        };

        // The Erratum: a bookshelf that is only a bookshelf while you look straight at it.
        Shot erratum = shot("erratum", 110);
        erratum.setup = () -> {
            clearMannequins();
            server((srv, p) -> {
                command(srv, p, "time set " + NIGHT);
                ServerLevel level = p.serverLevel();
                glade(level);
                fill(level, bx(-3), by(0), bz(1), bx(3), by(2), bz(1), Blocks.BOOKSHELF.defaultBlockState());
                level.setBlock(at(-1, 0, 0), Blocks.BOOKSHELF.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(at(1, 0, 0), Blocks.BOOKSHELF.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(at(-2, 0, 0), Blocks.BOOKSHELF.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(at(2, 0, -2), Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(at(-3, 0, -2), Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
            });
            MannequinAnim.subject = mannequin(ModEntities.ERRATUM.get(), x(0.5), y(0), z(0.5), 0F,
                    e -> data(e, "MIMIC", Blocks.BOOKSHELF.defaultBlockState()));
        };
        erratum.camera = (t, u) -> {
            float look = t < 40 ? 0 : t < 58 ? ease((t - 40) / 18F) : 1;
            Vec3 away = v(-1.6, 0.5, 0.5), at = v(0.5, 0.5, 0.5);
            return new Vec3[]{v(0.5, 1.3, -2.4 + 0.5 * u), away.lerp(at, look)};
        };
        erratum.each = t -> {
            if (t == 70 && MannequinAnim.subject != null) {
                data(MannequinAnim.subject, "MOVING", true);
                sound("entity.erratum.reveal", v(0.5, 0.5, 0.5), 1.2F, 1.0F);
            }
        };

        // The real thing again: a Redacted sees you, stops dead and stares, then comes.
        Shot redacted = shot("redacted_stare", 170);
        redacted.preroll = 12;
        redacted.setup = () -> {
            clearMannequins();
            TrailerRecorder.cue("music music_rasure 0.42");
            hunter(ModEntities.REDACTED.get(), -4.5, false);
        };
        redacted.camera = (t, u) -> {
            RedactedEntity r = nearest(RedactedEntity.class);
            return new Vec3[]{null, r == null ? v(0.5, 1.5, -11.0) : r.getEyePosition(), null};
        };
        redacted.each = t -> server((srv, p) -> {
            for (RedactedEntity r : p.serverLevel().getEntitiesOfClass(RedactedEntity.class, p.getBoundingBox().inflate(32))) r.setTarget(p);
        });
        redacted.until = () -> closeTo(nearest(RedactedEntity.class), 1.8F);

        // An Inkhound across the water: it swims.
        Shot inkhound = shot("inkhound_swim", 220);
        inkhound.preroll = 12;
        inkhound.setup = () -> hunter(ModEntities.INKHOUND.get(), -10.5, true);
        inkhound.camera = (t, u) -> {
            InkhoundEntity h = nearest(InkhoundEntity.class);
            return new Vec3[]{null, h == null ? v(0.5, 0.5, -10.0) : h.position().add(0, 0.5, 0), null};
        };
        inkhound.each = t -> server((srv, p) -> {
            for (InkhoundEntity h : p.serverLevel().getEntitiesOfClass(InkhoundEntity.class, p.getBoundingBox().inflate(32))) h.setTarget(p);
        });
        inkhound.until = () -> closeTo(nearest(InkhoundEntity.class), 2.0F);

        // The Undertext.
        card("undertext", 64, 64, 2, false, "The old writing never quite goes away.").then(() -> server((srv, p) -> {
            killMobs(p.serverLevel());
            p.setGameMode(GameType.SPECTATOR);
        }));
        undertext(ModStructures.FADED_VILLAGE, 100, 30F);
        undertext(ModStructures.MARGINALIA_SPIRE, 90, 200F);

        // And what waits at the bottom of it.
        boss("bookbinder", ModEntities.BOOKBINDER.get(), 80, v0(3.6, 3.0, -4.8), v0(1.9, 2.9, -2.6), v0(0.5, 1.8, 0.5), v0(0.5, 2.5, 0.5),
                "entity.bookbinder.ambient");
        boss("rasure", ModEntities.RASURE.get(), 80, v0(3.4, 3.4, -6.0), v0(1.2, 4.3, -2.8), v0(0.5, 2.6, 0.5), v0(0.5, 4.2, 0.5),
                "entity.rasure.ambient");

        card("title", 110, 104, 1, true, "A horror mod for Minecraft 1.20.1 (Forge)").each = t -> {
            if (t == 1) {
                TrailerRecorder.cue("music_off");
                ui("event.stinger", 0.7F, 1.0F);
            }
        };
        card("coda", 90, 0, 1, false).each = t -> {
            if (t == 18) ui("event.knock", 0.85F, 1.0F);
            if (t == 66) ui("entity.knocker.glass_tap", 1.0F, 0.9F);
        };
        Shot end = shot("end", 0);
        end.setup = () -> Minecraft.getInstance().setScreen(null);
    }

    // ------------------------------------------------------------------ shot builders

    private static Shot shot(String name, int length) {
        Shot s = new Shot(name, length);
        SHOTS.add(s);
        return s;
    }

    /** Black, with lines of text (or the logo and a line under it) fading in and out over {@code textLength} ticks. */
    private static Shot card(String name, int length, int textLength, int scale, boolean logo, String... lines) {
        Shot s = shot("card_" + name, length);
        s.preroll = 1;
        s.card = true;
        s.setup = () -> Minecraft.getInstance().setScreen(new TitleCardScreen(List.of(lines), scale, logo, textLength));
        return s;
    }

    private static Camera dolly(Vec3 eye0, Vec3 eye1, Vec3 look0, Vec3 look1) {
        return (t, u) -> {
            float e = ease(u);
            return new Vec3[]{eye0.lerp(eye1, e), look0.lerp(look1, e)};
        };
    }

    private static float ease(float u) {
        u = Mth.clamp(u, 0.0F, 1.0F);
        return u * u * (3 - 2 * u);
    }

    /** Three knocks on the hut door by the posed Knocker (one sound holds all three), the first landing at each given tick. */
    private static void knockAt(int t, int... when) {
        for (int w : when) {
            if (t == w - KnockerEntity.GESTURE_LEAD) SmokeTest.knockerGesture(KnockerEntity.GESTURE_KNOCK);
            if (t == w) sound("event.knock", v(0.5, 1.0, 3.5), 2.0F, 1.0F);
        }
    }

    /** The glade cleared, with a hunter at {@code z} and the player standing across it; over water if {@code pond}. */
    private static void hunter(EntityType<? extends Mob> type, double z, boolean pond) {
        server((srv, p) -> {
            ServerLevel level = p.serverLevel();
            killMobs(level);
            command(srv, p, "time set " + NIGHT);
            glade(level);
            if (pond) {
                fill(level, bx(-GLADE), by(-4), bz(-5), bx(GLADE), by(-1), bz(1), Blocks.DIRT.defaultBlockState());
                fill(level, bx(-GLADE), by(-3), bz(-5), bx(GLADE), by(-1), bz(1), Blocks.WATER.defaultBlockState());
            }
            level.setBlock(at(-3, 0, 3), Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(at(3, 0, -2), Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(at(-2, 0, -7), Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(BlockPos.containing(x(1.5), y(0), z(z + 1.5)), Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
            p.setGameMode(GameType.SURVIVAL);
            p.getAbilities().invulnerable = true;
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            moveTo(p, level, x(0.5), y(0), z(5.5), 180F, 0F);
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            Mob mob = type.create(level);
            if (mob == null) return;
            mob.moveTo(x(0.5), y(0), z(z), 0F, 0F);
            mob.setPersistenceRequired();
            level.addFreshEntity(mob);
        });
    }

    /**
     * A slow half-orbit round one of the Undertext's structures, framed from its real bounds. The
     * bosses that follow are posed on open ground a little way off from the last one.
     */
    private static void undertext(ResourceKey<Structure> key, int length, float heading) {
        Shot s = shot("undertext_" + key.location().getPath(), length);
        s.preroll = 160;
        s.maxWait = 600;
        s.setup = () -> {
            clearMannequins();
            under = null;
            sight = null;
            server((srv, p) -> {
                ServerLevel level = srv.getLevel(ModDimensions.UNDERTEXT);
                if (level == null) {
                    LOG.error("[trailer] TRAILER_FAIL the Undertext is not loaded");
                    return;
                }
                BoundingBox box = structureBox(level, key);
                BlockPos c = box == null ? new BlockPos(0, 64, 0) : box.getCenter();
                double radius = box == null ? 16.0D : Math.min(40.0D, Math.max(box.getXSpan(), box.getZSpan()) * 0.5D + 10.0D);
                int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX(), c.getZ());
                Vec3 focus = new Vec3(c.getX() + 0.5D, ground + (box == null ? 4.0D : Math.min(12.0D, (box.maxY() - ground) * 0.45D)), c.getZ() + 0.5D);
                double h = Math.toRadians(heading);
                moveTo(p, level, focus.x - Math.sin(h) * radius, focus.y + radius * 0.35D, focus.z + Math.cos(h) * radius, heading + 180F, 15F);
                // Open ground for the bosses, well clear of the building.
                BlockPos away = BlockPos.containing(focus.x - Math.sin(h) * (radius + 18), focus.y, focus.z + Math.cos(h) * (radius + 18));
                under = new BlockPos(away.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, away.getX(), away.getZ()), away.getZ());
                sight = new Vec3[]{focus, new Vec3(radius, 0, 0)};
                LOG.info("[trailer] undertext {} at {} (radius {})", key.location(), c, radius);
            });
        };
        s.ready = () -> sight != null && Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.dimension() == ModDimensions.UNDERTEXT;
        s.camera = (t, u) -> {
            Vec3[] at = sight;
            ClientLevel level = Minecraft.getInstance().level;
            if (at == null || level == null || level.dimension() != ModDimensions.UNDERTEXT) return null;
            Vec3 focus = at[0];
            double radius = at[1].x;
            double a = Math.toRadians(heading + 70.0D * ease(u));
            Vec3 eye = focus.add(-Math.sin(a) * radius, radius * (0.35D - 0.1D * u), Math.cos(a) * radius);
            return new Vec3[]{eye, focus};
        };
    }

    /** The bounds of the nearest copy of a structure in this dimension, with its chunks loaded, or null. */
    private static BoundingBox structureBox(ServerLevel level, ResourceKey<Structure> key) {
        Optional<Holder.Reference<Structure>> holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(key);
        if (holder.isEmpty()) return null;
        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(holder.get()), new BlockPos(0, 64, 0), 100, false);
        if (found == null) return null;
        BlockPos at = found.getFirst();
        StructureStart start = level.getChunk(at.getX() >> 4, at.getZ() >> 4).getStartForStructure(holder.get().value());
        if (start == null || !start.isValid()) return new BoundingBox(at);
        BoundingBox box = start.getBoundingBox();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) level.getChunk(cx, cz);
        }
        return box;
    }

    private static void boss(String name, EntityType<?> type, int length, Vec3 eye0, Vec3 eye1, Vec3 look0, Vec3 look1, String sound) {
        Shot s = shot("boss_" + name, length);
        s.preroll = 40;
        s.setup = () -> {
            clearMannequins();
            BlockPos b = under;
            if (b == null) return;
            server((srv, p) -> {
                ServerLevel level = p.serverLevel();
                killMobs(level);
                BlockState floor = level.getBlockState(b.below());
                clear(level, b.getX() - 8, b.getY() - 1, b.getZ() - 8, b.getX() + 8, b.getY() + 10, b.getZ() + 8,
                        floor.isAir() ? Blocks.STONE.defaultBlockState() : floor);
            });
            mannequin(type, b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 200F, e -> {});
        };
        s.camera = (t, u) -> {
            BlockPos b = under;
            if (b == null) return null;
            Vec3 o = Vec3.atBottomCenterOf(b).subtract(0.5, 0, 0.5);
            float e = ease(u);
            return new Vec3[]{o.add(eye0.lerp(eye1, e)), o.add(look0.lerp(look1, e))};
        };
        s.each = t -> {
            BlockPos b = under;
            if (t == 8 && b != null) sound(sound, Vec3.atCenterOf(b.above()), 1.5F, 1.0F);
        };
    }

    // ------------------------------------------------------------------ the glade and the hut

    private static void glade(ServerLevel level) {
        clear(level, bx(-GLADE), by(-1), bz(-GLADE), bx(GLADE), by(16), bz(GLADE), Blocks.GRASS_BLOCK.defaultBlockState());
        fill(level, bx(-GLADE), by(-4), bz(-GLADE), bx(GLADE), by(-2), bz(GLADE), Blocks.DIRT.defaultBlockState());
    }

    /** A plank hut: door in the south wall, a one-block gap low in the north wall, a torch inside, and maybe a window. */
    private static void hut(ServerLevel level, boolean window) {
        BlockState wall = Blocks.SPRUCE_PLANKS.defaultBlockState();
        fill(level, bx(-3), by(0), bz(-3), bx(3), by(2), bz(3), wall);
        fill(level, bx(-3), by(3), bz(-3), bx(3), by(3), bz(3), Blocks.DARK_OAK_PLANKS.defaultBlockState());
        for (int cx : new int[]{-3, 3}) {
            for (int cz : new int[]{-3, 3}) fill(level, bx(cx), by(0), bz(cz), bx(cx), by(3), bz(cz), Blocks.DARK_OAK_LOG.defaultBlockState());
        }
        fill(level, bx(-2), by(0), bz(-2), bx(2), by(2), bz(2), Blocks.AIR.defaultBlockState());
        fill(level, bx(-2), by(-1), bz(-2), bx(2), by(-1), bz(2), Blocks.OAK_PLANKS.defaultBlockState());
        BlockState lower = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
        level.setBlock(at(0, 0, 3), lower, Block.UPDATE_ALL);
        level.setBlock(at(0, 1, 3), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        level.setBlock(at(0, 0, -3), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(at(2, 0, 1), Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
        fill(level, bx(-2), by(1), bz(3), bx(-2), by(2), bz(3), window ? Blocks.GLASS.defaultBlockState() : wall);
        fill(level, bx(-1), by(1), bz(3), bx(-1), by(2), bz(3), window ? Blocks.GLASS.defaultBlockState() : wall);
        // A lamp by the path, so the door can be seen from the trees.
        level.setBlock(at(3, 0, 6), Blocks.OAK_FENCE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(at(3, 1, 6), Blocks.LANTERN.defaultBlockState(), Block.UPDATE_ALL);
    }

    // ------------------------------------------------------------------ small things

    /** The posed creature the current shot is about. */
    private static final class MannequinAnim {
        static Entity subject;
    }

    private static KnockerEntity knocker() {
        return nearest(KnockerEntity.class);
    }

    private static <T extends Entity> T nearest(Class<T> type) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;
        T best = null;
        for (T e : mc.level.getEntitiesOfClass(type, mc.player.getBoundingBox().inflate(40))) {
            if (e.getId() >= 2_000_000) continue;
            if (best == null || e.distanceToSqr(mc.player) < best.distanceToSqr(mc.player)) best = e;
        }
        return best;
    }

    private static boolean closeTo(Entity e, float distance) {
        LocalPlayer me = Minecraft.getInstance().player;
        return e != null && me != null && e.distanceTo(me) < distance;
    }

    private static SoundEvent soundEvent(String id) {
        SoundEvent s = ForgeRegistries.SOUND_EVENTS.getValue(Palimpsest.id(id));
        if (s == null) LOG.error("[trailer] TRAILER_FAIL no sound {}", id);
        return s;
    }

    private static void sound(String id, Vec3 at, float volume, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        SoundEvent s = soundEvent(id);
        if (s != null && mc.level != null) mc.level.playLocalSound(at.x, at.y, at.z, s, SoundSource.HOSTILE, volume, pitch, false);
    }

    private static void ui(String id, float pitch, float volume) {
        SoundEvent s = soundEvent(id);
        if (s != null) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, volume));
    }

    private static double x(double dx) {
        return base.getX() + dx;
    }

    private static double y(double dy) {
        return base.getY() + dy;
    }

    private static double z(double dz) {
        return base.getZ() + dz;
    }

    private static int bx(int dx) {
        return base.getX() + dx;
    }

    private static int by(int dy) {
        return base.getY() + dy;
    }

    private static int bz(int dz) {
        return base.getZ() + dz;
    }

    private static BlockPos at(int dx, int dy, int dz) {
        return base.offset(dx, dy, dz);
    }

    /** A point in the glade. */
    private static Vec3 v(double dx, double dy, double dz) {
        return new Vec3(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
    }

    /** A plain offset, for positions relative to something other than the glade. */
    private static Vec3 v0(double dx, double dy, double dz) {
        return new Vec3(dx, dy, dz);
    }
}
