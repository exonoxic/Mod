package com.exonoxic.palimpsest.world.gate;

import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.config.CommonConfig;
import com.exonoxic.palimpsest.entity.KnockerEntity;
import com.exonoxic.palimpsest.horror.WorldAlterations;
import com.exonoxic.palimpsest.network.PacketHandler;
import com.exonoxic.palimpsest.network.VisionPacket;
import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.util.Advancements;
import com.exonoxic.palimpsest.util.ServerScheduler;
import com.exonoxic.palimpsest.world.dimension.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Moving players through Folio Gates. The Undertext lies directly beneath the Overworld:
 * a gate at (x, z) opens onto the Undertext at the same (x, z). Arrival gates are built
 * on demand and linked both ways, so every gate always has a way back.
 */
public final class GateTravel {
    private static final Map<UUID, long[]> TIMERS = new HashMap<>();
    private static final int READ_TICKS = 60;

    public static void playerInVeil(ServerPlayer player, BlockPos pos) {
        if (player.isOnPortalCooldown()) return;
        long now = player.level().getGameTime();
        long[] t = TIMERS.computeIfAbsent(player.getUUID(), k -> new long[]{0, 0});
        if (t[1] == now) return; // entityInside fires once per touched block
        if (now - t[1] > 5) t[0] = 0;
        t[1] = now;
        t[0]++;
        if (t[0] == 1) {
            player.playNotifySound(ModSounds.VEIL_TRAVEL.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        if (t[0] % 8 == 0) {
            player.serverLevel().sendParticles(ModParticles.GLYPH.get(), player.getX(), player.getY() + 1, player.getZ(), 6, 0.4, 0.8, 0.4, 0.01);
        }
        int needed = player.getAbilities().instabuild ? 10 : READ_TICKS;
        if (t[0] >= needed) {
            TIMERS.remove(player.getUUID());
            PacketHandler.sendTo(player, VisionPacket.of(VisionPacket.PAGE_TURN, 30, 1.0F));
            travel(player, pos);
        }
    }

    public static void travel(ServerPlayer player, BlockPos veilPos) {
        ServerLevel from = player.serverLevel();
        MinecraftServer server = player.server;
        FolioGate.Shape here = FolioGate.fromVeil(from, veilPos);
        GateLinks links = GateLinks.get(server);

        boolean down = from.dimension() != ModDimensions.UNDERTEXT;
        ServerLevel dest = down ? server.getLevel(ModDimensions.UNDERTEXT) : server.overworld();
        if (dest == null) {
            player.displayClientMessage(Component.literal("The Undertext is missing from this world.").withStyle(ChatFormatting.RED), false);
            return;
        }

        GateLinks.Link link = down ? links.byOverworld(here.origin()) : links.byUndertext(here.origin());
        FolioGate.Shape there = null;
        if (link != null) {
            BlockPos o = down ? link.undertext() : link.overworld();
            Direction w = down ? link.undertextWidth() : link.overworldWidth();
            dest.getChunk(o.getX() >> 4, o.getZ() >> 4);
            if (FolioGate.valid(dest, o, w, true)) there = new FolioGate.Shape(o, w);
        }
        if (there == null) {
            there = buildArrival(dest, here);
            if (down) links.put(new GateLinks.Link(here.origin(), here.widthDir(), there.origin(), there.widthDir()));
            else links.put(new GateLinks.Link(there.origin(), there.widthDir(), here.origin(), here.widthDir()));
        }

        if (down) {
            CompoundTag ret = new CompoundTag();
            ret.putInt("x", here.front(true).getX());
            ret.putInt("y", here.front(true).getY());
            ret.putInt("z", here.front(true).getZ());
            player.getPersistentData().put("palimpsest_return", ret);
        }
        arrive(player, dest, there);
    }

    private static FolioGate.Shape buildArrival(ServerLevel dest, FolioGate.Shape from) {
        int x = from.origin().getX();
        int z = from.origin().getZ();
        dest.getChunk(x >> 4, z >> 4);
        int y = dest.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        y = Math.max(y, dest.getSeaLevel() + 1);
        y = Math.min(y, dest.getMaxBuildHeight() - 12);
        y = Math.max(y, dest.getMinBuildHeight() + 4);
        return FolioGate.build(dest, new BlockPos(x, y, z), from.widthDir());
    }

    public static void arrive(ServerPlayer player, ServerLevel dest, FolioGate.Shape gate) {
        boolean side = player.getRandom().nextBoolean();
        BlockPos front = gate.front(side);
        Direction facing = side ? gate.widthDir().getClockWise() : gate.widthDir().getCounterClockWise();
        Vec3 spot = new Vec3(front.getX() + 0.5 + (gate.widthDir().getStepX() * 0.5), front.getY(), front.getZ() + 0.5 + (gate.widthDir().getStepZ() * 0.5));
        player.teleportTo(dest, spot.x, spot.y, spot.z, facing.toYRot(), 0F);
        player.setPortalCooldown();
        dest.playSound(null, BlockPos.containing(spot), ModSounds.VEIL_TRAVEL.get(), SoundSource.BLOCKS, 1.0F, 0.7F);
    }

    /**
     * The moment of reading the Folio aloud. Lights nearby fail, something knocks from the other
     * side three times, and the page opens. In a thunderstorm, the something may come through.
     */
    public static void openGate(ServerLevel level, FolioGate.Shape shape, ServerPlayer reader) {
        BlockPos center = shape.origin().relative(shape.widthDir()).above();
        Vec3 c = Vec3.atCenterOf(center);
        if (CommonConfig.ALLOW_WORLD_ALTERATION.get()) WorldAlterations.snuffTorches(level, reader, 12, 12, true);
        level.playSound(null, center, ModSounds.GATE_OPEN.get(), SoundSource.BLOCKS, 1.5F, 0.8F);
        for (int i = 0; i < 3; i++) {
            ServerScheduler.schedule(20 + i * 16, () -> level.playSound(null, center, ModSounds.EVENT_KNOCK.get(), SoundSource.BLOCKS, 1.2F, 0.8F));
        }
        ServerScheduler.schedule(80, () -> {
            if (FolioGate.valid(level, shape.origin(), shape.widthDir(), true)) {
                FolioGate.fill(level, shape);
                level.sendParticles(ModParticles.GLYPH.get(), c.x, c.y, c.z, 60, 0.8, 1.2, 0.8, 0.02);
                if (level.isThundering() && level.random.nextFloat() < 0.3F && CommonConfig.ENABLE_KNOCKER.get()) {
                    KnockerEntity.comeThrough(level, shape.front(true), reader);
                }
            }
        });
        BleedManager.add(reader, 20F);
        BleedManager.unlock(reader, "folio_gate");
        Advancements.grant(reader, "read_aloud");
    }

    /** Used by the Bookmark and as a fallback: where a player should go when leaving the Undertext. */
    public static void returnToOverworld(ServerPlayer player, BlockPos marked) {
        ServerLevel overworld = player.server.overworld();
        BlockPos target = marked;
        if (target == null) {
            CompoundTag ret = player.getPersistentData().getCompound("palimpsest_return");
            if (!ret.isEmpty()) target = new BlockPos(ret.getInt("x"), ret.getInt("y"), ret.getInt("z"));
        }
        if (target == null) {
            BlockPos respawn = player.getRespawnPosition();
            target = respawn != null && player.getRespawnDimension() == Level.OVERWORLD ? respawn.above() : overworld.getSharedSpawnPos();
        }
        overworld.getChunk(target.getX() >> 4, target.getZ() >> 4);
        int surface = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
        int y = Math.max(target.getY(), overworld.getMinBuildHeight() + 1);
        if (!overworld.getBlockState(BlockPos.containing(target.getX(), y, target.getZ())).isAir()) y = surface;
        player.teleportTo(overworld, target.getX() + 0.5, y, target.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.setPortalCooldown();
    }

    private GateTravel() {}
}
