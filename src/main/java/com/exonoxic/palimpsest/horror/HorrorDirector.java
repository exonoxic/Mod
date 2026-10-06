package com.exonoxic.palimpsest.horror;

import com.exonoxic.palimpsest.bleed.BleedData;
import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.bleed.BleedStage;
import com.exonoxic.palimpsest.bleed.Ending;
import com.exonoxic.palimpsest.config.CommonConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Paces the horror for each player, like an AI director: it waits a stage-dependent, jittered
 * interval, then picks one eligible event by weight. Big events buy a longer quiet afterwards,
 * so tension has room to build instead of turning into noise.
 *
 * <p>Runs once per second per player and does nothing at all until its timer expires, so the
 * steady-state cost is a couple of comparisons.</p>
 */
public final class HorrorDirector {
    /** Base ticks between events at each stage (index = stage). Stage 0 never fires. */
    private static final int[] BASE_INTERVAL = {0, 14000, 9000, 6000, 4500, 3200, 2400};
    private static final int UNDERTEXT_INTERVAL = 2800;

    public static void tick(ServerPlayer player, BleedData data) {
        if (player.isSpectator() || !(player.level() instanceof ServerLevel level)) return;
        double intensity = CommonConfig.HORROR_INTENSITY.get();
        if (intensity <= 0) return;
        if (data.getEnding() == Ending.SEALED) return;

        long now = level.getGameTime();
        long grace = CommonConfig.GRACE_PERIOD_DAYS.get() * 24000L;
        if (data.getFirstSeen() >= 0 && now - data.getFirstSeen() < grace) return;

        EventContext ctx = new EventContext(player, level, data);
        if (ctx.stage == BleedStage.CLEAN_PAGE && !ctx.undertext) return;

        if (data.getNextEventTime() <= 0) {
            data.setNextEventTime(now + interval(ctx));
            return;
        }
        if (now < data.getNextEventTime()) return;

        HorrorEvent chosen = pick(ctx);
        boolean fired = chosen != null && fire(chosen, ctx);
        long next;
        if (fired) {
            next = (long) (interval(ctx) * (chosen.major ? 1.5D : 1.0D));
        } else {
            next = 400 + ctx.random.nextInt(400); // nothing fit; look again soon
        }
        data.setNextEventTime(now + next);
    }

    /** Fires an event and records its cooldown. Also used by /palimpsest event. */
    public static boolean fire(HorrorEvent event, EventContext ctx) {
        boolean fired = event.fire(ctx);
        if (fired) {
            ctx.data.setCooldown("event:" + event.id, ctx.now + event.cooldown);
            ctx.data.countApparition();
            if (event.bleed > 0) BleedManager.add(ctx.player, event.bleed);
        }
        return fired;
    }

    @Nullable
    private static HorrorEvent pick(EventContext ctx) {
        List<HorrorEvent> eligible = HorrorEvents.eligible(ctx);
        int total = 0;
        for (HorrorEvent e : eligible) total += e.weight;
        if (total <= 0) return null;
        int roll = ctx.random.nextInt(total);
        for (HorrorEvent e : eligible) {
            roll -= e.weight;
            if (roll < 0) return e;
        }
        return null;
    }

    private static long interval(EventContext ctx) {
        int base = ctx.undertext ? UNDERTEXT_INTERVAL : BASE_INTERVAL[ctx.stage.index];
        RandomSource r = ctx.random;
        double jitter = 0.6D + r.nextDouble() * 0.8D;
        double freq = CommonConfig.EVENT_FREQUENCY.get();
        return Math.max(200L, (long) (base * jitter / freq));
    }

    private HorrorDirector() {}
}
