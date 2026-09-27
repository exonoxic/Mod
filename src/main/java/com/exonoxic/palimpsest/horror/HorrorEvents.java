package com.exonoxic.palimpsest.horror;

import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.bleed.BleedStage;
import com.exonoxic.palimpsest.block.BlankBlock;
import com.exonoxic.palimpsest.block.TearBlock;
import com.exonoxic.palimpsest.config.CommonConfig;
import com.exonoxic.palimpsest.entity.*;
import com.exonoxic.palimpsest.network.PacketHandler;
import com.exonoxic.palimpsest.network.VisionPacket;
import com.exonoxic.palimpsest.registry.ModPoiTypes;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.util.ServerScheduler;
import com.exonoxic.palimpsest.util.Sounds;
import com.exonoxic.palimpsest.world.WardHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Every event the Director can choose from. Weights are relative within the eligible set;
 * cooldowns are per player and per event.
 */
public final class HorrorEvents {
    public static final List<HorrorEvent> ALL = new ArrayList<>();

    private static final String[] SIGN_LINES_A = {"", "it was here", "before you", ""};
    private static final String[] SIGN_LINES_B = {"turn around", "", "", ""};
    private static final String[] SIGN_LINES_C = {"THIS PAGE", "HAS BEEN", "WRITTEN", "OVER"};
    private static final String[] SIGN_LINES_D = {"", "", "you read it", "again"};
    private static final String[][] SIGN_LINES = {SIGN_LINES_A, SIGN_LINES_B, SIGN_LINES_C, SIGN_LINES_D};

    private static final String[] TYPED_LINES = {
            "hello?", "is someone there", "i can hear you reading", "why did you open it",
            "dont look behind you", "who wrote this", "i was here first", "come downstairs",
            "it says your name on the other side", "stop reading"
    };

    static {
        // ============================================================ stage 1 — Faint Trace
        add(HorrorEvent.builder("footsteps").stage(BleedStage.FAINT_TRACE).weight(10).cooldown(3600)
                .action(ctx -> {
                    ServerPlayer p = ctx.player;
                    Vec3 far = Sounds.behind(p, 9, 3);
                    Sounds.playTo(p, ModSounds.EVENT_FOOTSTEPS.get(), SoundSource.HOSTILE, far, 0.6F, 0.9F + ctx.random.nextFloat() * 0.2F);
                    if (ctx.atLeast(BleedStage.GHOSTING)) {
                        ServerScheduler.schedule(50, () -> Sounds.playTo(p, ModSounds.EVENT_FOOTSTEPS.get(), SoundSource.HOSTILE,
                                Sounds.behind(p, 4, 1), 0.8F, 1.0F));
                    }
                    BleedManager.unlock(p, "footsteps");
                    return true;
                }).build());

        add(HorrorEvent.builder("whisper").stage(BleedStage.FAINT_TRACE).weight(7).cooldown(4800)
                .action(ctx -> {
                    Sounds.playTo(ctx.player, ModSounds.EVENT_WHISPER.get(), SoundSource.HOSTILE, Sounds.atEar(ctx.player), 0.35F, 1.0F);
                    BleedManager.unlock(ctx.player, "whispers");
                    return true;
                }).build());

        add(HorrorEvent.builder("watcher_crow").stage(BleedStage.FAINT_TRACE).weight(6).cooldown(6000).overworldOnly()
                .when(ctx -> !ctx.indoors && !ctx.underground)
                .action(ctx -> {
                    BlockPos pos = Spots.ground(ctx.level, ctx.player, 12, 22, 0, 70, 1, 12, p -> true);
                    return pos != null && QuillcrowEntity.spawnWatcher(ctx.level, pos, ctx.player);
                }).build());

        add(HorrorEvent.builder("snuffed_torch").stage(BleedStage.FAINT_TRACE).weight(6).cooldown(7200)
                .when(EventContext::alterationsAllowed)
                .action(ctx -> {
                    int n = WorldAlterations.snuffTorches(ctx.level, ctx.player, 10, 1, false);
                    if (n > 0) BleedManager.unlock(ctx.player, "snuffed_torch");
                    return n > 0;
                }).build());

        // ============================================================ stage 2 — Ghosting
        add(HorrorEvent.builder("the_sign").stage(BleedStage.GHOSTING).weight(5).cooldown(12000)
                .action(ctx -> {
                    String[] lines = SIGN_LINES[ctx.random.nextInt(SIGN_LINES.length)];
                    boolean ok = Glimpses.rewriteSign(ctx.player, 12, lines, 400);
                    if (ok) BleedManager.unlock(ctx.player, "the_sign");
                    return ok;
                }).build());

        add(HorrorEvent.builder("knocking").stage(BleedStage.GHOSTING).weight(8).cooldown(6000)
                .when(ctx -> ctx.indoors && ctx.night)
                .action(ctx -> {
                    BlockPos door = WorldAlterations.findDoor(ctx.level, ctx.player.blockPosition(), 9, true);
                    if (door == null) return false;
                    Vec3 at = Vec3.atCenterOf(door);
                    ServerPlayer p = ctx.player;
                    for (int i = 0; i < 3; i++) {
                        ServerScheduler.schedule(1 + i * 14, () -> Sounds.playTo(p, ModSounds.EVENT_KNOCK.get(), SoundSource.HOSTILE, at, 0.9F, 1.0F));
                    }
                    BleedManager.unlock(p, "knocking");
                    return true;
                }).build());

        add(HorrorEvent.builder("door_opens").stage(BleedStage.GHOSTING).weight(4).cooldown(12000)
                .when(EventContext::alterationsAllowed)
                .action(ctx -> {
                    boolean ok = WorldAlterations.openDoor(ctx.level, ctx.player, 10);
                    if (ok) BleedManager.unlock(ctx.player, "moved_things");
                    return ok;
                }).build());

        add(HorrorEvent.builder("false_sound").stage(BleedStage.GHOSTING).weight(8).cooldown(3000)
                .action(ctx -> {
                    SoundEvent[] pool = {SoundEvents.CREEPER_PRIMED, SoundEvents.CHEST_OPEN, SoundEvents.ZOMBIE_AMBIENT,
                            SoundEvents.SKELETON_AMBIENT, SoundEvents.STONE_BREAK, SoundEvents.WOODEN_DOOR_OPEN,
                            SoundEvents.ARROW_SHOOT, SoundEvents.GRAVEL_STEP};
                    SoundEvent s = pool[ctx.random.nextInt(pool.length)];
                    Sounds.playTo(ctx.player, s, SoundSource.HOSTILE, Sounds.behind(ctx.player, 3 + ctx.random.nextInt(4), 1.5), 0.8F, 0.95F);
                    BleedManager.unlock(ctx.player, "footsteps");
                    return true;
                }).build());

        add(HorrorEvent.builder("chest_shuffle").stage(BleedStage.GHOSTING).weight(3).cooldown(18000)
                .when(EventContext::alterationsAllowed)
                .action(ctx -> {
                    boolean ok = WorldAlterations.shuffleChest(ctx.level, ctx.player, 10);
                    if (ok) BleedManager.unlock(ctx.player, "moved_things");
                    return ok;
                }).build());

        add(HorrorEvent.builder("heartbeat").stage(BleedStage.GHOSTING).weight(5).cooldown(6000)
                .when(ctx -> ctx.underground || ctx.dark)
                .action(ctx -> {
                    ctx.player.playNotifySound(ModSounds.EVENT_HEARTBEAT.get(), SoundSource.AMBIENT, 0.7F, 1.0F);
                    PacketHandler.sendTo(ctx.player, VisionPacket.of(VisionPacket.SHAKE, 60, 0.15F));
                    return true;
                }).build());

        add(HorrorEvent.builder("smudge_glimpse").stage(BleedStage.GHOSTING).weight(6).cooldown(9000).major()
                .when(ctx -> ctx.night || ctx.underground)
                .action(ctx -> {
                    BlockPos pos = Spots.ground(ctx.level, ctx.player, 22, 34, 0, 55, 2, 16, p -> ctx.level.getMaxLocalRawBrightness(p) <= 7);
                    return pos != null && SmudgeEntity.spawnApparition(ctx.level, pos, ctx.player);
                }).build());

        add(HorrorEvent.builder("stag_glimpse").stage(BleedStage.GHOSTING).weight(4).cooldown(12000).overworldOnly().major()
                .when(ctx -> ctx.night && !ctx.indoors)
                .action(ctx -> {
                    BlockPos pos = Spots.ground(ctx.level, ctx.player, 28, 40, 0, 50, 2, 16, p -> true);
                    return pos != null && PaleStagEntity.spawnApparition(ctx.level, pos, ctx.player);
                }).build());

        // ============================================================ stage 3 — Bleed-through
        add(HorrorEvent.builder("knocker_visit").stage(BleedStage.BLEED_THROUGH).weight(6).cooldown(24000).overworldOnly().major().bleed(4)
                .when(ctx -> ctx.night && ctx.indoors && CommonConfig.ENABLE_KNOCKER.get())
                .when(ctx -> !WardHelper.isWarded(ctx.level, ctx.player.blockPosition(), 24))
                .action(ctx -> {
                    BlockPos door = WorldAlterations.findDoor(ctx.level, ctx.player.blockPosition(), 10, true);
                    return door != null && KnockerEntity.spawnAtDoor(ctx.level, door, ctx.player);
                }).build());

        add(HorrorEvent.builder("copyist").stage(BleedStage.BLEED_THROUGH).weight(5).cooldown(24000).overworldOnly().major().bleed(3)
                .when(ctx -> CommonConfig.ENABLE_COPYIST.get() && !ctx.underground)
                .action(ctx -> {
                    List<Animal> animals = ctx.level.getEntitiesOfClass(Animal.class, ctx.player.getBoundingBox().inflate(32),
                            a -> (a instanceof Cow || a instanceof Pig || a instanceof Sheep || a instanceof Chicken)
                                    && !a.isBaby() && !a.hasCustomName() && !a.isLeashed() && !Spots.inView(ctx.player, a.position(), 0.5));
                    if (!animals.isEmpty()) {
                        return CopyistEntity.replaceAnimal(ctx.level, animals.get(ctx.random.nextInt(animals.size())), ctx.player);
                    }
                    BlockPos pos = Spots.ground(ctx.level, ctx.player, 18, 30, 180, 90, 2, 12,
                            p -> ctx.level.getBlockState(p.below()).is(Blocks.GRASS_BLOCK));
                    return pos != null && CopyistEntity.spawnDisguised(ctx.level, pos, ctx.player);
                }).build());

        add(HorrorEvent.builder("longhand_sighting").stage(BleedStage.BLEED_THROUGH).weight(5).cooldown(18000).overworldOnly().major().bleed(3)
                .when(ctx -> CommonConfig.ENABLE_LONGHAND.get() && ctx.night && !ctx.indoors)
                .action(ctx -> {
                    BlockPos pos = Spots.ground(ctx.level, ctx.player, 38, 58, 0, 45, 4, 20, p -> true);
                    return pos != null && LonghandEntity.spawnWatcher(ctx.level, pos, ctx.player);
                }).build());

        add(HorrorEvent.builder("fog_bank").stage(BleedStage.BLEED_THROUGH).weight(6).cooldown(9000).overworldOnly()
                .when(ctx -> !ctx.indoors)
                .action(ctx -> {
                    PacketHandler.sendTo(ctx.player, VisionPacket.of(VisionPacket.FOG, 1200, (float) Math.min(1.0, 0.6 + 0.2 * ctx.intensity)));
                    BleedManager.unlock(ctx.player, "fog");
                    return true;
                }).build());

        add(HorrorEvent.builder("other_page").stage(BleedStage.BLEED_THROUGH).weight(5).cooldown(12000).overworldOnly().major()
                .action(ctx -> {
                    int n = Glimpses.showOtherPage(ctx.player, 9 + (int) (3 * ctx.intensity), 50 + ctx.random.nextInt(30));
                    if (n > 0) {
                        ctx.player.playNotifySound(ModSounds.EVENT_PAGE_TURN_DISTANT.get(), SoundSource.AMBIENT, 0.9F, 0.7F);
                        BleedManager.unlock(ctx.player, "the_other_page");
                    }
                    return n > 0;
                }).build());

        add(HorrorEvent.builder("vignette_face").stage(BleedStage.BLEED_THROUGH).weight(2).cooldown(36000)
                .action(ctx -> {
                    PacketHandler.sendTo(ctx.player, VisionPacket.of(VisionPacket.VIGNETTE_FACE, 5, 1.0F));
                    return true;
                }).build());

        // ============================================================ stage 4 — Running Ink
        add(HorrorEvent.builder("fading").stage(BleedStage.RUNNING_INK).weight(5).cooldown(12000).overworldOnly()
                .when(ctx -> ctx.alterationsAllowed() && !ctx.indoors)
                .action(ctx -> {
                    BlockPos pos = Spots.ground(ctx.level, ctx.player, 8, 16, 180, 80, 1, 8, p -> true);
                    if (pos == null) return false;
                    int n = WorldAlterations.fadeGrass(ctx.level, pos, 4, (int) (10 + 10 * ctx.intensity));
                    if (n > 0) BleedManager.unlock(ctx.player, "fading");
                    return n > 0;
                }).build());

        add(HorrorEvent.builder("ink_rain").stage(BleedStage.RUNNING_INK).weight(4).cooldown(18000).overworldOnly()
                .when(ctx -> !ctx.indoors)
                .action(ctx -> {
                    PacketHandler.sendTo(ctx.player, VisionPacket.of(VisionPacket.INK_RAIN, 900, 1.0F));
                    BleedManager.unlock(ctx.player, "ink_rain");
                    return true;
                }).build());

        add(HorrorEvent.builder("silence").stage(BleedStage.RUNNING_INK).weight(4).cooldown(18000).major()
                .action(ctx -> {
                    PacketHandler.sendTo(ctx.player, VisionPacket.of(VisionPacket.SILENCE, 360, 1.0F));
                    BleedManager.unlock(ctx.player, "silence");
                    return true;
                }).build());

        add(HorrorEvent.builder("lights_out").stage(BleedStage.RUNNING_INK).weight(3).cooldown(24000).major()
                .when(ctx -> ctx.alterationsAllowed() && ctx.indoors && ctx.night)
                .action(ctx -> {
                    int n = WorldAlterations.snuffTorches(ctx.level, ctx.player, 12, (int) (3 + 3 * ctx.intensity), true);
                    if (n > 0) {
                        Sounds.playTo(ctx.player, ModSounds.EVENT_BREATH.get(), SoundSource.HOSTILE, Sounds.behind(ctx.player, 2, 0.5), 0.5F, 0.9F);
                        BleedManager.unlock(ctx.player, "snuffed_torch");
                    }
                    return n > 0;
                }).build());

        add(HorrorEvent.builder("someone_typed").stage(BleedStage.RUNNING_INK).weight(3).cooldown(36000)
                .when(ctx -> CommonConfig.FAKE_CHAT_MESSAGES.get())
                .action(ctx -> {
                    ServerPlayer p = ctx.player;
                    Component name = p.getDisplayName();
                    List<ServerPlayer> others = new ArrayList<>(ctx.level.getServer().getPlayerList().getPlayers());
                    others.remove(p);
                    if (!others.isEmpty() && ctx.random.nextBoolean()) name = others.get(ctx.random.nextInt(others.size())).getDisplayName();
                    String line = TYPED_LINES[ctx.random.nextInt(TYPED_LINES.length)];
                    p.sendSystemMessage(Component.translatable("chat.type.text", name, Component.literal(line)));
                    BleedManager.unlock(p, "someone_typed");
                    return true;
                }).build());

        add(HorrorEvent.builder("erratum").stage(BleedStage.RUNNING_INK).weight(2).cooldown(60000).major()
                .when(ctx -> CommonConfig.ENABLE_ERRATUM.get())
                .action(ctx -> {
                    BlockPos pos = Spots.ground(ctx.level, ctx.player, 6, 12, 180, 70, 1, 12, p -> true);
                    if (pos == null) return false;
                    BlockState mimic = ctx.level.getBlockState(pos.below());
                    if (!mimic.isCollisionShapeFullBlock(ctx.level, pos.below()) || mimic.hasBlockEntity()) mimic = Blocks.COBBLESTONE.defaultBlockState();
                    return ErratumEntity.spawnMimic(ctx.level, pos, mimic, ctx.player);
                }).build());

        // ============================================================ stage 5 — The Tear
        add(HorrorEvent.builder("tear_opens").stage(BleedStage.THE_TEAR).weight(4).cooldown(24000).overworldOnly().major().bleed(4)
                .when(ctx -> CommonConfig.ALLOW_TEARS.get() && !ctx.indoors)
                .when(ctx -> ctx.level.getPoiManager().getCountInRange(h -> h.is(ModPoiTypes.TEAR.getKey()), ctx.player.blockPosition(), 128,
                        PoiManager.Occupancy.ANY) < CommonConfig.MAX_TEARS_PER_PLAYER.get())
                .action(ctx -> {
                    BlockPos pos = Spots.ground(ctx.level, ctx.player, 20, 40, 0, 120, 2, 16,
                            p -> !WardHelper.isWarded(ctx.level, p, 32));
                    if (pos == null || !TearBlock.open(ctx.level, pos)) return false;
                    BleedManager.unlock(ctx.player, "tears");
                    return true;
                }).build());

        add(HorrorEvent.builder("sky_glimpse").stage(BleedStage.THE_TEAR).weight(4).cooldown(18000).overworldOnly()
                .when(ctx -> !ctx.indoors)
                .action(ctx -> {
                    PacketHandler.sendTo(ctx.player, VisionPacket.of(VisionPacket.SKY_GLIMPSE, 120, 1.0F));
                    return true;
                }).build());

        add(HorrorEvent.builder("palehand").stage(BleedStage.THE_TEAR).weight(1).cooldown(120000).overworldOnly().major()
                .when(ctx -> !ctx.indoors && ctx.night)
                .action(ctx -> {
                    BlockPos pos = Spots.ground(ctx.level, ctx.player, 120, 150, 0, 25, 1, 10, p -> true);
                    return pos != null && PalehandEntity.spawnSighting(ctx.level, pos, ctx.player);
                }).build());

        add(HorrorEvent.builder("fair_copy").stage(BleedStage.THE_TEAR).weight(2).cooldown(48000).major()
                .when(ctx -> CommonConfig.ENABLE_FAIR_COPY.get())
                .action(ctx -> {
                    boolean inside = ctx.atLeast(BleedStage.OVERWRITTEN) && ctx.indoors;
                    BlockPos pos = inside
                            ? Spots.ground(ctx.level, ctx.player, 6, 10, 180, 60, 2, 16, p -> true)
                            : Spots.ground(ctx.level, ctx.player, 22, 32, 0, 60, 2, 16, p -> true);
                    return pos != null && FairCopyEntity.spawnFor(ctx.level, pos, ctx.player);
                }).build());

        // ============================================================ stage 6 — Overwritten
        add(HorrorEvent.builder("scraping").stage(BleedStage.OVERWRITTEN).weight(6).cooldown(6000).overworldOnly().major()
                .when(ctx -> CommonConfig.ALLOW_SCRAPING.get())
                .action(ctx -> {
                    BlockPos origin = Spots.ground(ctx.level, ctx.player, 10, 18, ctx.random.nextInt(360), 30, 1, 8, p -> true);
                    if (origin == null) return false;
                    scrapeLine(ctx.level, origin, ctx.random.nextFloat() * 360F, 8 + (int) (6 * ctx.intensity), false, 2400 + ctx.random.nextInt(2400));
                    Sounds.playTo(ctx.player, ModSounds.EVENT_SCRAPE_DISTANT.get(), SoundSource.HOSTILE, Vec3.atCenterOf(origin), 1.2F, 0.8F);
                    BleedManager.unlock(ctx.player, "scraping");
                    return true;
                }).build());

        // ============================================================ the Undertext
        add(HorrorEvent.builder("u_page_turn").undertextOnly().weight(8).cooldown(3000).bleed(0.5F)
                .action(ctx -> {
                    PacketHandler.sendTo(ctx.player, VisionPacket.of(VisionPacket.PAGE_TURN, 40, 1.0F));
                    ctx.player.playNotifySound(ModSounds.EVENT_PAGE_TURN_DISTANT.get(), SoundSource.AMBIENT, 1.0F, 0.6F);
                    SmudgeEntity.fixateAll(ctx.level, ctx.player, 48);
                    return true;
                }).build());

        add(HorrorEvent.builder("u_whisper").undertextOnly().weight(6).cooldown(2400).bleed(0.5F)
                .action(ctx -> {
                    Sounds.playTo(ctx.player, ModSounds.EVENT_WHISPER.get(), SoundSource.HOSTILE, Sounds.atEar(ctx.player), 0.4F, 0.85F);
                    return true;
                }).build());

        add(HorrorEvent.builder("u_rasure_line").undertextOnly().weight(5).cooldown(4800).bleed(1F)
                .when(ctx -> !ctx.underground)
                .action(ctx -> {
                    BlockPos origin = Spots.ground(ctx.level, ctx.player, 6, 12, 0, 40, 1, 8, p -> true);
                    if (origin == null) return false;
                    float yaw = ctx.player.getYRot() + 90F + (ctx.random.nextFloat() - 0.5F) * 40F;
                    telegraphLine(ctx.level, origin, yaw, 14);
                    ServerScheduler.schedule(40, () -> scrapeLine(ctx.level, origin, yaw, 14, true, 200));
                    Sounds.playTo(ctx.player, ModSounds.EVENT_SCRAPE_DISTANT.get(), SoundSource.HOSTILE, Vec3.atCenterOf(origin), 1.0F, 1.1F);
                    BleedManager.unlock(ctx.player, "scraping");
                    return true;
                }).build());

        add(HorrorEvent.builder("u_longhand").undertextOnly().weight(3).cooldown(24000).major()
                .when(ctx -> CommonConfig.ENABLE_LONGHAND.get() && !WardHelper.isWarded(ctx.level, ctx.player.blockPosition(), 16))
                .action(ctx -> {
                    BlockPos pos = Spots.ground(ctx.level, ctx.player, 24, 36, 180, 90, 4, 16, p -> ctx.level.getMaxLocalRawBrightness(p) < 6);
                    return pos != null && LonghandEntity.spawnHunter(ctx.level, pos, ctx.player);
                }).build());

        add(HorrorEvent.builder("u_footsteps").undertextOnly().weight(5).cooldown(3600)
                .action(ctx -> {
                    Sounds.playTo(ctx.player, ModSounds.EVENT_FOOTSTEPS.get(), SoundSource.HOSTILE, Sounds.behind(ctx.player, 5, 2), 0.7F, 0.8F);
                    return true;
                }).build());
    }

    private static void add(HorrorEvent e) {
        ALL.add(e);
    }

    /** Red motes along a line: the only warning before the Rasure's knife passes. */
    public static void telegraphLine(ServerLevel level, BlockPos origin, float yaw, int length) {
        double dx = -Math.sin(Math.toRadians(yaw));
        double dz = Math.cos(Math.toRadians(yaw));
        for (int i = -length / 2; i <= length / 2; i++) {
            double x = origin.getX() + 0.5 + dx * i;
            double z = origin.getZ() + 0.5 + dz * i;
            level.sendParticles(com.exonoxic.palimpsest.registry.ModParticles.RUBRIC_SPARK.get(), x, origin.getY() + 0.1, z, 4, 0.2, 0.05, 0.2, 0.0);
        }
    }

    /** Turns a line of surface blocks blank. Blank blocks always restore themselves. */
    public static int scrapeLine(ServerLevel level, BlockPos origin, float yaw, int length, boolean hollow, int restoreTicks) {
        double dx = -Math.sin(Math.toRadians(yaw));
        double dz = Math.cos(Math.toRadians(yaw));
        int n = 0;
        for (int i = -length / 2; i <= length / 2; i++) {
            for (int w = 0; w < 2; w++) {
                int x = (int) Math.floor(origin.getX() + 0.5 + dx * i + (w == 1 ? dz : 0));
                int z = (int) Math.floor(origin.getZ() + 0.5 + dz * i - (w == 1 ? dx : 0));
                BlockPos surface = Spots.surfaceAt(level, x, z);
                if (surface == null) continue;
                BlockPos target = surface.below();
                if (Math.abs(target.getY() - origin.getY()) > 6) continue;
                if (BlankBlock.scrape(level, target, restoreTicks, hollow)) n++;
            }
        }
        if (n > 0) {
            level.sendParticles(ParticleTypes.WHITE_ASH, origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5, 30, length / 3.0, 0.5, length / 3.0, 0.0);
        }
        return n;
    }

    /** Events eligible right now, for the Director and the debug command. */
    public static List<HorrorEvent> eligible(EventContext ctx) {
        List<HorrorEvent> out = new ArrayList<>();
        for (HorrorEvent e : ALL) if (e.eligible(ctx)) out.add(e);
        Collections.shuffle(out, new java.util.Random(ctx.random.nextLong()));
        return out;
    }

    public static HorrorEvent byId(String id) {
        for (HorrorEvent e : ALL) if (e.id.equals(id)) return e;
        return null;
    }

    private HorrorEvents() {}
}
