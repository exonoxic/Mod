package com.exonoxic.palimpsest.entity;

import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.world.SpawnRules;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Pale moths, freckled like old paper ("foxing"). They drift toward light and leave a fine
 * brown dust. Harmless. Where there are many, the page is thin.
 */
public class FoxingMothEntity extends AmbientCreature {
    @Nullable
    private BlockPos target;

    public FoxingMothEntity(EntityType<? extends AmbientCreature> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 2.0D);
    }

    public static boolean checkSpawn(EntityType<FoxingMothEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return SpawnRules.undertextOr(level, pos, 1) && level.getBrightness(LightLayer.SKY, pos) < 12 && random.nextInt(3) == 0;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected float getSoundVolume() {
        return 0.25F;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return random.nextInt(3) == 0 ? ModSounds.MOTH_AMBIENT.get() : null;
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
        if (level().isClientSide && random.nextInt(20) == 0) {
            level().addParticle(ModParticles.MOTH_DUST.get(), getX(), getY(), getZ(), 0, -0.01, 0);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (target != null && (!level().isEmptyBlock(target) || target.getY() <= level().getMinBuildHeight())) target = null;
        if (tickCount % 100 == 0) {
            BlockPos light = findLight();
            if (light != null) target = light;
        }
        if (target == null || random.nextInt(30) == 0 || target.closerToCenterThan(position(), 1.5D)) {
            BlockPos base = target != null && random.nextInt(3) != 0 ? target : blockPosition();
            target = BlockPos.containing(base.getX() + random.nextInt(5) - random.nextInt(5), base.getY() + random.nextInt(4) - 1.5D,
                    base.getZ() + random.nextInt(5) - random.nextInt(5));
        }
        double dx = target.getX() + 0.5D - getX();
        double dy = target.getY() + 0.1D - getY();
        double dz = target.getZ() + 0.5D - getZ();
        Vec3 v = getDeltaMovement();
        Vec3 nv = v.add((Math.signum(dx) * 0.3D - v.x) * 0.1D, (Math.signum(dy) * 0.5D - v.y) * 0.1D, (Math.signum(dz) * 0.3D - v.z) * 0.1D);
        setDeltaMovement(nv);
        float yaw = (float) (Mth.atan2(nv.z, nv.x) * (180F / Math.PI)) - 90.0F;
        zza = 0.4F;
        setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()));
    }

    /** Samples a few nearby spots and remembers the brightest block-lit one. */
    @Nullable
    private BlockPos findLight() {
        BlockPos best = null;
        int bestLight = 9;
        for (int i = 0; i < 10; i++) {
            BlockPos p = blockPosition().offset(random.nextInt(17) - 8, random.nextInt(9) - 4, random.nextInt(17) - 8);
            if (!level().isEmptyBlock(p)) continue;
            int l = level().getBrightness(LightLayer.BLOCK, p);
            if (l > bestLight) {
                bestLight = l;
                best = p;
            }
        }
        return best;
    }
}
