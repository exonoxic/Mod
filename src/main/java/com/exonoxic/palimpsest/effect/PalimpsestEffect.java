package com.exonoxic.palimpsest.effect;

import com.exonoxic.palimpsest.registry.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Shared class for the mod's status effects. Most of their behaviour lives in event handlers
 * (healing is cancelled under Erasure, Bleed gain is halved under Warded, fog closes in under
 * Inkblind); the only per-tick work is Erasure II+, which slowly scrapes the bearer thin.
 */
public class PalimpsestEffect extends MobEffect {
    public PalimpsestEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (this == ModEffects.ERASURE.get() && amplifier >= 1 && entity.getHealth() > 1.0F) {
            entity.hurt(entity.damageSources().magic(), 1.0F);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        if (this != ModEffects.ERASURE.get() || amplifier < 1) return false;
        int interval = Math.max(20, 60 >> (amplifier - 1));
        return duration % interval == 0;
    }
}
