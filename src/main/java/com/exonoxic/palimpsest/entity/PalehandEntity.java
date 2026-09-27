package com.exonoxic.palimpsest.entity;

import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.entity.apparition.Apparition;
import com.exonoxic.palimpsest.entity.apparition.ApparitionState;
import com.exonoxic.palimpsest.entity.apparition.Apparitions;
import com.exonoxic.palimpsest.registry.ModEntities;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.util.Advancements;
import com.exonoxic.palimpsest.util.Sounds;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * Very rarely, at night, very far away: a hand the size of a hill rises behind the horizon,
 * holding nothing, and moves its fingers as if writing. Then it goes back down. That is all.
 * It cannot be reached. The rise-hold-sink cycle is driven by {@code tickCount} and the synced
 * sink tick so the client animates it without extra packets.
 */
public class PalehandEntity extends Mob implements Apparition {
    public static final int RISE = 80;
    public static final int HOLD = 220;
    public static final int SINK = 70;
    private static final EntityDataAccessor<Integer> SINK_AT = SynchedEntityData.defineId(PalehandEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> VIEWER = SynchedEntityData.defineId(PalehandEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private final ApparitionState apparition = new ApparitionState();
    private boolean seen;

    public PalehandEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
        setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1000.0D);
    }

    public static boolean spawnSighting(ServerLevel level, BlockPos pos, ServerPlayer player) {
        PalehandEntity hand = Apparitions.spawn(ModEntities.PALEHAND.get(), level, pos.below(6), player, h -> {
            h.apparition.begin(player, RISE + HOLD + SINK + 40, 0);
            h.entityData.set(VIEWER, Apparitions.viewerFor(player));
            h.setPersistenceRequired();
        });
        if (hand == null) return false;
        Sounds.playTo(player, ModSounds.PALEHAND_AMBIENT.get(), SoundSource.AMBIENT, hand.position().add(0, 20, 0), 9.0F, 0.6F);
        return true;
    }

    /** The tick at which it began sinking, or -1. */
    public int getSinkAt() {
        return entityData.get(SINK_AT);
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
        entityData.define(SINK_AT, -1);
        entityData.define(VIEWER, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        Player p = apparition.target(level());
        if (getSinkAt() < 0 && (tickCount > RISE + HOLD || p == null || p.distanceToSqr(this) < 80 * 80)) {
            entityData.set(SINK_AT, tickCount);
        }
        if (getSinkAt() >= 0 && tickCount - getSinkAt() > SINK) {
            discard();
            return;
        }
        if (!seen && p instanceof ServerPlayer sp && tickCount % 10 == 0 && tickCount > RISE / 2) {
            Vec3 top = position().add(0, 30, 0);
            HitResult clip = level().clip(new ClipContext(sp.getEyePosition(), top, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, sp));
            boolean facing = top.subtract(sp.getEyePosition()).normalize().dot(sp.getLookAngle()) > 0.8;
            if (clip.getType() == HitResult.Type.MISS && facing) {
                seen = true;
                BleedManager.unlock(sp, "palehand");
                BleedManager.addOnce(sp, "saw:palehand", 10F);
                Advancements.grant(sp, "the_hand");
            }
        }
        if (apparition.tick(this)) discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
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
