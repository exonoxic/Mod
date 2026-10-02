package com.exonoxic.palimpsest.entity.ai;

import com.exonoxic.palimpsest.Palimpsest;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What blind hunters can hear. Players make noise by sprinting, jumping, fighting, breaking and
 * placing blocks, and (quietly) by walking. Sneaking makes none. Entries fade after a few
 * seconds; the whole map is a handful of records per level.
 */
@Mod.EventBusSubscriber(modid = Palimpsest.MODID)
public final class NoiseMap {
    public record Noise(Vec3 pos, float loudness, long time, UUID source) {}

    private static final Map<ResourceKey<Level>, Deque<Noise>> NOISES = new HashMap<>();
    private static final int MAX_PER_LEVEL = 96;
    private static final long MAX_AGE = 100L;

    public static void emit(Player player, float loudness) {
        if (player.level().isClientSide || player.isSpectator() || player.isCrouching()) return;
        Deque<Noise> q = NOISES.computeIfAbsent(player.level().dimension(), k -> new ArrayDeque<>());
        long now = player.level().getGameTime();
        while (!q.isEmpty() && (now - q.peekFirst().time() > MAX_AGE || q.size() >= MAX_PER_LEVEL)) q.pollFirst();
        q.addLast(new Noise(player.position(), loudness, now, player.getUUID()));
    }

    /** The loudest recent noise that reaches {@code pos}; loudness 1.0 carries 16 blocks. */
    @Nullable
    public static Noise loudestHeard(Level level, Vec3 pos, long maxAge) {
        Deque<Noise> q = NOISES.get(level.dimension());
        if (q == null) return null;
        long now = level.getGameTime();
        Noise best = null;
        double bestScore = 0;
        for (Noise n : q) {
            if (now - n.time() > maxAge) continue;
            double range = 16.0D * n.loudness();
            double d2 = n.pos().distanceToSqr(pos);
            if (d2 > range * range) continue;
            double score = n.loudness() / (1.0D + Math.sqrt(d2));
            if (score > bestScore) {
                bestScore = score;
                best = n;
            }
        }
        return best;
    }

    public static long lastHeardFrom(Level level, UUID source) {
        Deque<Noise> q = NOISES.get(level.dimension());
        if (q == null) return Long.MIN_VALUE;
        long last = Long.MIN_VALUE;
        for (Noise n : q) if (n.source().equals(source)) last = Math.max(last, n.time());
        return last;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        Player p = event.player;
        long t = p.level().getGameTime();
        if (p.isSprinting() && t % 10 == 0) emit(p, 1.0F);
        else if (t % 20 == 0 && p.getDeltaMovement().horizontalDistanceSqr() > 0.002D && p.onGround()) emit(p, 0.35F);
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player p) emit(p, 0.8F);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        emit(event.getPlayer(), 1.0F);
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player p) emit(p, 0.7F);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        emit(event.getEntity(), 1.0F);
    }

    @SubscribeEvent
    public static void onStop(ServerStoppingEvent event) {
        NOISES.clear();
    }

    private NoiseMap() {}
}
