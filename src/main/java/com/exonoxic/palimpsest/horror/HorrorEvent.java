package com.exonoxic.palimpsest.horror;

import com.exonoxic.palimpsest.bleed.BleedStage;

import java.util.function.Predicate;

/**
 * One thing the Director can make happen. Events are data-light and stateless; any state they
 * need lives on the player's BleedData (cooldowns) or on the entities they spawn.
 */
public final class HorrorEvent {
    public interface Action {
        /** @return true if the event actually happened (it may find nothing suitable nearby). */
        boolean fire(EventContext ctx);
    }

    public final String id;
    public final BleedStage minStage;
    public final int weight;
    public final int cooldown;
    public final boolean undertextOnly;
    public final boolean overworldOnly;
    public final boolean major;
    public final float bleed;
    private final Predicate<EventContext> condition;
    private final Action action;

    private HorrorEvent(Builder b) {
        this.id = b.id;
        this.minStage = b.minStage;
        this.weight = b.weight;
        this.cooldown = b.cooldown;
        this.undertextOnly = b.undertextOnly;
        this.overworldOnly = b.overworldOnly;
        this.major = b.major;
        this.bleed = b.bleed;
        this.condition = b.condition;
        this.action = b.action;
    }

    public boolean eligible(EventContext ctx) {
        if (undertextOnly && !ctx.undertext) return false;
        if (overworldOnly && !ctx.overworld) return false;
        if (!undertextOnly && !ctx.atLeast(minStage)) return false;
        if (ctx.data.onCooldown("event:" + id, ctx.now)) return false;
        return condition.test(ctx);
    }

    public boolean fire(EventContext ctx) {
        return action.fire(ctx);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private BleedStage minStage = BleedStage.FAINT_TRACE;
        private int weight = 5;
        private int cooldown = 6000;
        private boolean undertextOnly;
        private boolean overworldOnly;
        private boolean major;
        private float bleed = 1F;
        private Predicate<EventContext> condition = c -> true;
        private Action action = c -> false;

        private Builder(String id) {
            this.id = id;
        }

        public Builder stage(BleedStage s) { this.minStage = s; return this; }
        public Builder weight(int w) { this.weight = w; return this; }
        public Builder cooldown(int ticks) { this.cooldown = ticks; return this; }
        public Builder undertextOnly() { this.undertextOnly = true; return this; }
        public Builder overworldOnly() { this.overworldOnly = true; return this; }
        public Builder major() { this.major = true; return this; }
        public Builder bleed(float b) { this.bleed = b; return this; }
        public Builder when(Predicate<EventContext> c) { this.condition = this.condition.and(c); return this; }
        public Builder action(Action a) { this.action = a; return this; }

        public HorrorEvent build() {
            return new HorrorEvent(this);
        }
    }
}
