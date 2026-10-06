package com.exonoxic.palimpsest.util;

import com.exonoxic.palimpsest.Palimpsest;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A tiny delayed-task queue on the server thread. Only used for short, cosmetic follow-ups
 * (the second set of footsteps, restoring a fake glimpse). Nothing here needs to survive a
 * restart: every scheduled action is either purely client-visual or idempotent.
 */
@Mod.EventBusSubscriber(modid = Palimpsest.MODID)
public final class ServerScheduler {
    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> PENDING = new ArrayList<>();
    private static long tick;

    private record Task(long at, Runnable action) {}

    public static void schedule(int delayTicks, Runnable action) {
        PENDING.add(new Task(tick + Math.max(1, delayTicks), action));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tick++;
        if (!PENDING.isEmpty()) {
            TASKS.addAll(PENDING);
            PENDING.clear();
        }
        if (TASKS.isEmpty()) return;
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.at() <= tick) {
                it.remove();
                try {
                    t.action().run();
                } catch (Exception e) {
                    Palimpsest.LOGGER.warn("Scheduled Palimpsest task failed", e);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        TASKS.clear();
        PENDING.clear();
    }

    private ServerScheduler() {}
}
