package com.exonoxic.palimpsest.bleed;

import net.minecraft.network.chat.Component;

/**
 * "Bleed-through": the old writing showing through the new page.
 * The player is never shown these names directly; the Commonplace Book describes each stage
 * obliquely.
 */
public enum BleedStage {
    CLEAN_PAGE(0, 0F),
    FAINT_TRACE(1, 50F),
    GHOSTING(2, 150F),
    BLEED_THROUGH(3, 300F),
    RUNNING_INK(4, 500F),
    THE_TEAR(5, 700F),
    OVERWRITTEN(6, 900F);

    public static final float MAX = 1000F;

    public final int index;
    public final float threshold;

    BleedStage(int index, float threshold) {
        this.index = index;
        this.threshold = threshold;
    }

    public static BleedStage of(float bleed) {
        BleedStage result = CLEAN_PAGE;
        for (BleedStage s : values()) {
            if (bleed >= s.threshold) result = s;
        }
        return result;
    }

    public static BleedStage byIndex(int index) {
        BleedStage[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, index))];
    }

    public boolean atLeast(BleedStage other) {
        return this.index >= other.index;
    }

    /** The oblique line the Commonplace Book shows for this stage. */
    public Component describe() {
        return Component.translatable("codex.palimpsest.stage." + index);
    }
}
