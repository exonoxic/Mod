package com.exonoxic.palimpsest.entity;

import com.exonoxic.palimpsest.entity.ai.NoiseMap;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.world.SpawnRules;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Lean hounds of dried ink with no eyes at all. They hunt by sound: sprint, jump or break
 * something and they come; crouch and hold still and they lose you.
 */
public class InkhoundEntity extends Monster {
    @Nullable
    private UUID heardFrom;
    private int heardCount;
    private long lastHeard;

    public InkhoundEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D).add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    public static boolean checkSpawn(EntityType<InkhoundEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && SpawnRules.undertextOr(level, pos, 99)
                && level.getMaxLocalRawBrightness(pos) < 9 && checkMobSpawnRules(type, level, reason, pos, random);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3D, false));
        goalSelector.addGoal(4, new ListenGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || tickCount % 20 != 0) return;
        LivingEntity target = getTarget();
        // A player who goes quiet — crouching, not fighting — slips out of the hound's world.
        if (target instanceof Player p && distanceToSqr(p) > 9.0D && p.isCrouching()
                && level().getGameTime() - NoiseMap.lastHeardFrom(level(), p.getUUID()) > 80) {
            setTarget(null);
            getNavigation().stop();
        }
    }

    private void heard(NoiseMap.Noise noise) {
        long now = level().getGameTime();
        if (noise.source().equals(heardFrom) && now - lastHeard < 120) heardCount++;
        else heardCount = 1;
        heardFrom = noise.source();
        lastHeard = now;
        // The first thing it hears, the jaw drops open a little and it goes still to listen.
        if (heardCount == 1 && getTarget() == null) playSound(ModSounds.INKHOUND_HISS.get(), 0.5F, 1.1F + random.nextFloat() * 0.2F);
        Player p = level().getPlayerByUUID(noise.source());
        if (p != null && !p.isCreative() && !p.isSpectator() && (heardCount >= 3 || p.distanceToSqr(this) < 36.0D)) {
            if (getTarget() != p) playSound(ModSounds.INKHOUND_HISS.get(), 1.2F, 0.85F + random.nextFloat() * 0.15F);
            setTarget(p);
            playSound(ModSounds.INKHOUND_HOWL.get(), 1.6F, 0.9F + random.nextFloat() * 0.2F);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSounds.INKHOUND_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.INKHOUND_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.INKHOUND_DEATH.get();
    }

    /** Turns toward and walks to whatever it last heard. */
    private class ListenGoal extends Goal {
        @Nullable
        private Vec3 goal;
        private int cooldown;

        ListenGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null) return false;
            if (--cooldown > 0) return false;
            cooldown = 10;
            NoiseMap.Noise n = NoiseMap.loudestHeard(level(), position(), 40);
            if (n == null) return false;
            goal = n.pos();
            heard(n);
            return getTarget() == null;
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() == null && goal != null && !getNavigation().isDone();
        }

        @Override
        public void start() {
            if (goal != null) getNavigation().moveTo(goal.x, goal.y, goal.z, 1.0D);
        }

        @Override
        public void tick() {
            if (goal != null) getLookControl().setLookAt(goal.x, goal.y + 1, goal.z);
        }
    }
}
