package com.exonoxic.palimpsest.client.model;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Small tools the generated models use to move smoothly: states that ease in and out instead of
 * snapping, keyframed gestures, and twitches that are sudden without being a single-frame jump.
 * Client only.
 */
public final class Anim {
    private static final int SLOTS = 8;
    /** Per entity and slot: the eased value, the age it was last updated at, and whether it has started. */
    private static final Map<Entity, float[]> EASED = new WeakHashMap<>();

    private Anim() {}

    /**
     * Moves slot {@code slot} of this entity's eased values towards {@code target}, closing
     * {@code rate} of the gap per tick (so the same speed at any frame rate), and returns it. A
     * value starts out at its target.
     */
    public static float ease(Entity entity, int slot, float target, float ageInTicks, float rate) {
        float[] s = EASED.computeIfAbsent(entity, e -> new float[SLOTS * 3]);
        int i = slot * 3;
        if (s[i + 2] == 0.0F) {
            s[i] = target;
            s[i + 1] = ageInTicks;
            s[i + 2] = 1.0F;
            return target;
        }
        float dt = ageInTicks - s[i + 1];
        s[i + 1] = ageInTicks;
        if (dt <= 0.0F) return s[i];
        if (dt > 40.0F) return s[i] = target;
        s[i] += (target - s[i]) * (1.0F - (float) Math.pow(1.0F - rate, dt));
        return s[i];
    }

    /** {@link #ease} for an on/off state. */
    public static float ease(Entity entity, int slot, boolean on, float ageInTicks, float rate) {
        return ease(entity, slot, on ? 1.0F : 0.0F, ageInTicks, rate);
    }

    /**
     * A curve through (time, value) pairs, eased in and out between each; it holds the first value
     * before the first time and the last after the last.
     */
    public static float keys(float t, float... timesAndValues) {
        if (t <= timesAndValues[0]) return timesAndValues[1];
        for (int i = 2; i < timesAndValues.length; i += 2) {
            if (t <= timesAndValues[i]) {
                float u = (t - timesAndValues[i - 2]) / (timesAndValues[i] - timesAndValues[i - 2]);
                return Mth.lerp(smooth(u), timesAndValues[i - 1], timesAndValues[i + 1]);
            }
        }
        return timesAndValues[timesAndValues.length - 1];
    }

    /** {@link #keys} on a loop: {@code phase} in any range, times running 0 to 1 (give 0 and 1 the same value). */
    public static float loop(float phase, float... timesAndValues) {
        return keys(phase - Mth.floor(phase), timesAndValues);
    }

    /**
     * 0 to 1 as a slow wave climbs past {@code threshold}, rising over the next third of the way to
     * its crest: a twitch that comes on suddenly but is not a one-frame jump.
     */
    public static float pulse(float wave, float threshold) {
        return smooth((wave - threshold) / ((1.0F - threshold) * 0.33F));
    }

    /**
     * A blow that lands at {@code u} = 0 (in ticks): drawn back, driven in, bouncing off. Add it to
     * a resting 0..1 reach (0 drawn back, 1 on the wood); it is 0 away from the blow.
     */
    public static float knock(float u) {
        return u <= -4.0F || u >= 8.0F ? 0.0F : keys(u, -4.0F, 0.0F, -1.5F, -0.3F, 0.0F, 0.7F, 2.5F, 0.15F, 8.0F, 0.0F);
    }

    public static float smooth(float u) {
        u = Mth.clamp(u, 0.0F, 1.0F);
        return u * u * (3.0F - 2.0F * u);
    }
}
