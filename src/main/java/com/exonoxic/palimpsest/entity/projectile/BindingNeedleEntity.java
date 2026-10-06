package com.exonoxic.palimpsest.entity.projectile;

import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModEntities;
import com.exonoxic.palimpsest.registry.ModItems;
import com.exonoxic.palimpsest.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Thrown by the Bookbinder: a needle trailing red thread. It pins you where you stand. */
public class BindingNeedleEntity extends ThrowableItemProjectile {
    public BindingNeedleEntity(EntityType<? extends BindingNeedleEntity> type, Level level) {
        super(type, level);
    }

    public BindingNeedleEntity(Level level, LivingEntity thrower) {
        super(ModEntities.BINDING_NEEDLE.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BOOKBINDER_NEEDLE.get();
    }

    @Override
    protected float getGravity() {
        return 0.01F;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (result.getEntity() instanceof LivingEntity target && target != getOwner()) {
            target.hurt(damageSources().thrown(this, getOwner()), 4.0F);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 3));
            level().playSound(null, target.blockPosition(), ModSounds.BOOKBINDER_NEEDLE.get(), SoundSource.HOSTILE, 1.0F, 1.2F);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide && random.nextFloat() < 0.25F) {
            BlockPos p = result.getBlockPos().relative(result.getDirection());
            if (level().isEmptyBlock(p)) level().setBlock(p, ModBlocks.BINDING_THREAD.get().defaultBlockState(), 3);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) discard();
    }
}
