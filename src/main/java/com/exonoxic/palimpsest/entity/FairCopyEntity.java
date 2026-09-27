package com.exonoxic.palimpsest.entity;

import com.exonoxic.palimpsest.entity.apparition.Apparition;
import com.exonoxic.palimpsest.entity.apparition.ApparitionState;
import com.exonoxic.palimpsest.entity.apparition.Apparitions;
import com.exonoxic.palimpsest.registry.ModEntities;
import com.exonoxic.palimpsest.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * In a manuscript, the "fair copy" is the clean final version made from a messy draft. This is
 * you, copied out cleanly: your face, your name, whatever you were holding. It stands at a
 * distance and watches. Go toward it and it walks away around a corner. Strike it and you will
 * find out which of you is the draft.
 */
public class FairCopyEntity extends PathfinderMob implements Apparition {
    private static final EntityDataAccessor<Optional<UUID>> COPY_OF = SynchedEntityData.defineId(FairCopyEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> VIEWER = SynchedEntityData.defineId(FairCopyEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private final ApparitionState apparition = new ApparitionState();
    private int walkingAway = -1;
    private int watched;

    public FairCopyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.MOVEMENT_SPEED, 0.28D);
    }

    public static boolean spawnFor(ServerLevel level, BlockPos pos, ServerPlayer player) {
        return Apparitions.spawn(ModEntities.FAIR_COPY.get(), level, pos, player, c -> {
            c.entityData.set(COPY_OF, Optional.of(player.getUUID()));
            c.entityData.set(VIEWER, Apparitions.viewerFor(player));
            c.apparition.begin(player, 900, 0);
            c.setItemSlot(EquipmentSlot.MAINHAND, player.getMainHandItem().copy());
            c.setDropChance(EquipmentSlot.MAINHAND, 0F);
            c.setCustomName(player.getName());
            c.setCustomNameVisible(true);
        }) != null;
    }

    public Optional<UUID> copyOf() {
        return entityData.get(COPY_OF);
    }

    @Override
    public ApparitionState apparition() {
        return apparition;
    }

    @Override
    public Optional<UUID> exclusiveViewer() {
        return entityData.get(VIEWER);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(COPY_OF, Optional.empty());
        entityData.define(VIEWER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        Player p = apparition.target(level());
        if (p == null || apparition.tick(this)) {
            vanish();
            return;
        }
        if (walkingAway >= 0) {
            walkingAway++;
            if (walkingAway > 60 || !p.hasLineOfSight(this) || getNavigation().isDone()) vanish();
            return;
        }
        getLookControl().setLookAt(p, 30F, 30F);
        double d2 = p.distanceToSqr(this);
        if (d2 < 12 * 12) {
            Vec3 away = position().subtract(p.position());
            Vec3 goal = position().add(new Vec3(away.x, 0, away.z).normalize().scale(14));
            getNavigation().moveTo(goal.x, goal.y, goal.z, 1.0D);
            walkingAway = 0;
            return;
        }
        Vec3 to = position().add(0, 1.5, 0).subtract(p.getEyePosition()).normalize();
        watched = to.dot(p.getLookAngle()) > 0.95 && p.hasLineOfSight(this) ? watched + 1 : 0;
        if (watched > 100) vanish();
    }

    private void vanish() {
        level().playSound(null, blockPosition(), ModSounds.FAIR_COPY_VANISH.get(), SoundSource.HOSTILE, 0.4F, 1.0F);
        ApparitionState.vanish(this);
    }

    /** Whatever you do to it happens to you. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (source.getEntity() instanceof Player attacker) {
            attacker.hurt(level().damageSources().magic(), amount);
            vanish();
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        apparition.save(tag);
        copyOf().ifPresent(u -> tag.putUUID("CopyOf", u));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        apparition.load(tag);
        if (tag.hasUUID("CopyOf")) entityData.set(COPY_OF, Optional.of(tag.getUUID("CopyOf")));
    }

    @Override
    protected @Nullable net.minecraft.sounds.SoundEvent getAmbientSound() {
        return null;
    }
}
