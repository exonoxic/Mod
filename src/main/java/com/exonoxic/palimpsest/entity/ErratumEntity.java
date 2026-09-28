package com.exonoxic.palimpsest.entity;

import com.exonoxic.palimpsest.entity.apparition.Apparition;
import com.exonoxic.palimpsest.entity.apparition.ApparitionState;
import com.exonoxic.palimpsest.entity.apparition.Apparitions;
import com.exonoxic.palimpsest.registry.ModEntities;
import com.exonoxic.palimpsest.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * A block that is in the wrong place. It looks like the ground it stands on and you can stand
 * on it. When nobody is looking, it is a little closer than it was.
 */
public class ErratumEntity extends PathfinderMob implements Apparition {
    private static final EntityDataAccessor<BlockState> MIMIC = SynchedEntityData.defineId(ErratumEntity.class, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Boolean> MOVING = SynchedEntityData.defineId(ErratumEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> VIEWER = SynchedEntityData.defineId(ErratumEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private final ApparitionState apparition = new ApparitionState();
    private int hits;

    // Client-side presentation state, advanced by ErratumRenderer each frame.
    public float animLegs;
    public float animEye;
    public float animTime = Float.NaN;
    public boolean animWasMoving;
    private float renderLegs;
    private float renderIrisX;
    private float renderIrisY;

    public ErratumEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0D).add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public static boolean spawnMimic(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        return Apparitions.spawn(ModEntities.ERRATUM.get(), level, pos, player, e -> {
            e.entityData.set(MIMIC, state);
            e.entityData.set(VIEWER, Apparitions.viewerFor(player));
            e.apparition.begin(player, 3600, 0);
            e.setYRot(0);
            e.setPersistenceRequired();
        }) != null;
    }

    public BlockState getMimic() {
        return entityData.get(MIMIC);
    }

    public boolean isMoving() {
        return entityData.get(MOVING);
    }

    /** 0 folded under the block, 1 fully unfolded (client, for the model). */
    public float legExtension() {
        return renderLegs;
    }

    public float irisX() {
        return renderIrisX;
    }

    public float irisY() {
        return renderIrisY;
    }

    public void setRenderState(float legs, float irisX, float irisY) {
        this.renderLegs = legs;
        this.renderIrisX = irisX;
        this.renderIrisY = irisY;
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
        entityData.define(MIMIC, Blocks.COBBLESTONE.defaultBlockState());
        entityData.define(MOVING, false);
        entityData.define(VIEWER, Optional.empty());
    }

    private boolean observed() {
        for (Player p : level().players()) {
            if (p.isSpectator() || p.distanceToSqr(this) > 48 * 48) continue;
            Vec3 to = position().add(0, 0.5, 0).subtract(p.getEyePosition()).normalize();
            if (to.dot(p.getLookAngle()) > 0.55 && p.hasLineOfSight(this)) return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (apparition.tick(this)) {
            ApparitionState.vanish(this);
            return;
        }
        Player p = apparition.target(level());
        boolean move = p != null && !observed() && p.distanceToSqr(this) > 6.0D && p.distanceToSqr(this) < 32 * 32;
        if (move) {
            getNavigation().moveTo(p, 1.0D);
        } else if (isMoving() || !getNavigation().isDone()) {
            getNavigation().stop();
            BlockPos b = blockPosition();
            setPos(b.getX() + 0.5, getY(), b.getZ() + 0.5);
            setYRot(0);
            yBodyRot = 0;
            yHeadRot = 0;
        }
        if (move != isMoving()) entityData.set(MOVING, move);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || !(source.getEntity() instanceof Player)) return super.hurt(source, amount);
        hits++;
        playSound(ModSounds.ERRATUM_REVEAL.get(), 1.4F, 0.8F + random.nextFloat() * 0.4F);
        if (hits >= 3) return super.hurt(source, 1000F);
        for (int i = 0; i < 16; i++) {
            if (randomTeleport(getX() + (random.nextDouble() - 0.5) * 24, getY() + random.nextInt(8) - 4, getZ() + (random.nextDouble() - 0.5) * 24, false)) break;
        }
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return isAlive();
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
    protected @Nullable SoundEvent getAmbientSound() {
        return isMoving() ? ModSounds.ERRATUM_AMBIENT.get() : null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ERRATUM_REVEAL.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        apparition.save(tag);
        tag.put("Mimic", NbtUtils.writeBlockState(getMimic()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        apparition.load(tag);
        if (tag.contains("Mimic")) {
            entityData.set(MIMIC, NbtUtils.readBlockState(level().holderLookup(net.minecraft.core.registries.Registries.BLOCK), tag.getCompound("Mimic")));
        }
    }
}
