package com.exonoxic.palimpsest.entity.projectile;

import com.exonoxic.palimpsest.bleed.BleedEvents;
import com.exonoxic.palimpsest.entity.boss.PalimpsestBoss;
import com.exonoxic.palimpsest.registry.ModEffects;
import com.exonoxic.palimpsest.registry.ModEntities;
import com.exonoxic.palimpsest.registry.ModItems;
import com.exonoxic.palimpsest.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class InkBombEntity extends ThrowableItemProjectile {
    public InkBombEntity(EntityType<? extends InkBombEntity> type, Level level) {
        super(type, level);
    }

    public InkBombEntity(Level level, LivingEntity thrower) {
        super(ModEntities.INK_BOMB.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.INK_BOMB.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel server)) return;
        server.sendParticles(ModParticles.INK_DRIP.get(), getX(), getY(), getZ(), 60, 1.2, 0.6, 1.2, 0.1);
        server.playSound(null, blockPosition(), SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, 0.6F);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.0D))) {
            if (e instanceof Player p) {
                if (p != getOwner()) p.addEffect(new MobEffectInstance(ModEffects.INKBLIND.get(), 100, 0));
                continue;
            }
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
            if (e.getType().is(BleedEvents.INKBORN)) e.hurt(damageSources().thrown(this, getOwner()), 3.0F);
            if (e instanceof PalimpsestBoss boss && getOwner() instanceof Player owner) boss.onRevealed(owner);
        }
        discard();
    }
}
