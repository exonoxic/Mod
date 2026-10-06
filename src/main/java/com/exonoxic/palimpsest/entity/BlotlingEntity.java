package com.exonoxic.palimpsest.entity;

import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.world.SpawnRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * A drop of ink that has decided to move. Splits when struck, like the blot it is.
 */
public class BlotlingEntity extends Slime {
    public BlotlingEntity(EntityType<? extends Slime> type, Level level) {
        super(type, level);
    }

    public static boolean checkSpawn(EntityType<BlotlingEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        return SpawnRules.undertextOr(level, pos, 5) && level.getMaxLocalRawBrightness(pos) < 6
                && checkMobSpawnRules(type, level, reason, pos, random);
    }

    @Override
    protected ParticleOptions getParticleType() {
        return ModParticles.INK_DRIP.get();
    }

    @Override
    protected SoundEvent getSquishSound() {
        return ModSounds.BLOTLING_SQUISH.get();
    }

    @Override
    protected SoundEvent getJumpSound() {
        return ModSounds.BLOTLING_SQUISH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.BLOTLING_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BLOTLING_DEATH.get();
    }
}
