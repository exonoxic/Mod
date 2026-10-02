package com.exonoxic.palimpsest.entity;

import com.exonoxic.palimpsest.config.CommonConfig;
import com.exonoxic.palimpsest.entity.ai.Pursuit;
import com.exonoxic.palimpsest.registry.ModEffects;
import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.world.SpawnRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * A figure censored by black bars. Its blade does not cut so much as delete: the wound will
 * not heal while Erasure lasts, and sometimes the name of something you carry goes black.
 * When struck, the bars shift and it is somewhere else.
 */
public class RedactedEntity extends Monster {
    public RedactedEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50.0D).add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D).add(Attributes.ARMOR, 6.0D).add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    public static boolean checkSpawn(EntityType<RedactedEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && SpawnRules.undertextOr(level, pos, 99) && checkMobSpawnRules(type, level, reason, pos, random);
    }

    @Override
    protected void registerGoals() {
        Pursuit.install(this, goalSelector, 0, () -> true, 50, 110);
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1D, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (!hit || !(target instanceof LivingEntity living)) return hit;
        living.addEffect(new MobEffectInstance(ModEffects.ERASURE.get(), 100, 0));
        playSound(ModSounds.REDACTED_ATTACK.get(), 1.0F, 1.0F);
        if (target instanceof Player player && CommonConfig.REDACTED_RENAMES_ITEMS.get() && random.nextFloat() < 0.2F) {
            int slot = random.nextInt(9);
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && !stack.hasCustomHoverName()) {
                int len = Math.max(3, stack.getHoverName().getString().length());
                stack.setHoverName(Component.literal("█".repeat(Math.min(len, 16))).withStyle(ChatFormatting.BLACK));
            }
        }
        return true;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        // It has noticed you: the bars twitch and something under them squeals like a marker.
        if (target instanceof Player && getTarget() == null && !level().isClientSide) playSound(ModSounds.REDACTED_STATIC.get(), 1.2F, 1.0F);
        super.setTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive() && !level().isClientSide && random.nextFloat() < 0.25F) {
            for (int i = 0; i < 12; i++) {
                double x = getX() + (random.nextDouble() - 0.5) * 10;
                double y = getY() + random.nextInt(5) - 2;
                double z = getZ() + (random.nextDouble() - 0.5) * 10;
                double ox = getX(), oy = getY(), oz = getZ();
                if (randomTeleport(x, y, z, true)) {
                    ((ServerLevel) level()).sendParticles(ModParticles.ERASURE_MOTE.get(), ox, oy + 1, oz, 20, 0.3, 0.8, 0.3, 0.02);
                    level().playSound(null, ox, oy, oz, ModSounds.REDACTED_STATIC.get(), getSoundSource(), 1.0F, 1.0F);
                    playSound(ModSounds.REDACTED_STATIC.get(), 1.0F, 0.8F);
                    break;
                }
            }
        }
        return hurt;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.REDACTED_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.REDACTED_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.REDACTED_DEATH.get();
    }
}
