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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * A deer the colour of blank paper, with no face at all. In the Undertext they graze in herds
 * and bolt when approached. In the Overworld one is sometimes seen standing at the treeline,
 * facing you, and is gone before you can get close.
 */
public class PaleStagEntity extends PathfinderMob implements Apparition {
    private static final EntityDataAccessor<Optional<UUID>> VIEWER = SynchedEntityData.defineId(PaleStagEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private final ApparitionState apparition = new ApparitionState();

    public PaleStagEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    public static boolean spawnApparition(ServerLevel level, BlockPos pos, ServerPlayer player) {
        return Apparitions.spawn(ModEntities.PALE_STAG.get(), level, pos, player, s -> {
            s.apparition.begin(player, 500, 20);
            s.entityData.set(VIEWER, Apparitions.viewerFor(player));
            s.setPersistenceRequired();
        }) != null;
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
        entityData.define(VIEWER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 2.0D));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 10.0F, 1.4D, 1.8D, p -> !apparition.active() && !p.isSpectator()));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D) {
            @Override
            public boolean canUse() {
                return !apparition.active() && super.canUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !apparition.active() && super.canUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !apparition.active()) return;
        Player p = apparition.target(level());
        if (p != null) getLookControl().setLookAt(p, 10F, 10F);
        getNavigation().stop();
        if (apparition.tick(this)) ApparitionState.vanish(this);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return apparition.active() || distance > 16384.0D;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return random.nextInt(5) == 0 ? ModSounds.STAG_AMBIENT.get() : null;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.STAG_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.STAG_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        apparition.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        apparition.load(tag);
    }
}
