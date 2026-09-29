package com.exonoxic.palimpsest.entity;

import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.entity.ai.Pursuit;
import com.exonoxic.palimpsest.entity.ai.Squeeze;
import com.exonoxic.palimpsest.entity.ai.SqueezeNavigation;
import com.exonoxic.palimpsest.entity.ai.Squeezer;
import com.exonoxic.palimpsest.registry.ModEntities;
import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.util.Advancements;
import com.exonoxic.palimpsest.world.SpawnRules;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Something that learned what an animal is by watching them, and got it almost right. It wears
 * a cow, or a pig, or a sheep, and stands in the field with the others. Tells: it casts no
 * shadow, it never grazes, it always faces you, and its voice is a little too low. Get close,
 * feed it, hurt it or look at it through a Reading Lens and it stops pretending.
 */
public class CopyistEntity extends Monster implements Squeezer {
    public static final int COW = 0;
    public static final int PIG = 1;
    public static final int SHEEP = 2;
    public static final int CHICKEN = 3;
    public static final int REVEALED = 4;

    private static final EntityDataAccessor<Integer> DISGUISE = SynchedEntityData.defineId(CopyistEntity.class, EntityDataSerializers.INT);
    /** Which animal's skin it wears once revealed: the last thing it pretended to be. */
    private static final EntityDataAccessor<Integer> HIDE = SynchedEntityData.defineId(CopyistEntity.class, EntityDataSerializers.INT);
    private int revealIn = -1;
    private int huntTicks;
    private int idleHunt;

    private static final float REVEAL_TICKS = 16.0F;
    private int revealedAt = Integer.MIN_VALUE;

    /** Once it stops pretending, nothing it can fit its head through is closed to it. */
    public final Squeeze squeeze = Squeeze.of(this, 0.9F, 2.2F);

    public CopyistEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 10;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new SqueezeNavigation(this, level);
    }

    @Override
    public Squeeze squeeze() {
        return squeeze;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return squeeze == null ? super.getBoundingBoxForCulling() : squeeze.cullingBox(super.getBoundingBoxForCulling());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0D).add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D).add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    public static boolean checkSpawn(EntityType<CopyistEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && SpawnRules.undertextOr(level, pos, 3) && random.nextInt(4) == 0
                && checkMobSpawnRules(type, level, reason, pos, random);
    }

    public static boolean replaceAnimal(ServerLevel level, Animal animal, ServerPlayer player) {
        CopyistEntity c = ModEntities.COPYIST.get().create(level);
        if (c == null) return false;
        int d = animal instanceof Pig ? PIG : animal instanceof Sheep ? SHEEP : animal instanceof Chicken ? CHICKEN : COW;
        c.entityData.set(DISGUISE, d);
        c.moveTo(animal.getX(), animal.getY(), animal.getZ(), animal.getYRot(), animal.getXRot());
        c.setYHeadRot(animal.getYHeadRot());
        c.setPersistenceRequired();
        animal.discard();
        return level.addFreshEntity(c);
    }

    public static boolean spawnDisguised(ServerLevel level, BlockPos pos, ServerPlayer player) {
        CopyistEntity c = ModEntities.COPYIST.get().create(level);
        if (c == null) return false;
        c.entityData.set(DISGUISE, level.random.nextInt(4));
        c.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360F, 0);
        c.setPersistenceRequired();
        return level.addFreshEntity(c);
    }

    public int getDisguise() {
        return entityData.get(DISGUISE);
    }

    public boolean isRevealed() {
        return getDisguise() == REVEALED;
    }

    public int getHide() {
        return entityData.get(HIDE);
    }

    /** Client only: 0..1 through the unfolding that follows a reveal; 1 when there is none. */
    public float revealProgress(float partialTick) {
        if (revealedAt == Integer.MIN_VALUE) return 1.0F;
        return Math.min(1.0F, (tickCount - revealedAt + partialTick) / REVEAL_TICKS);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                                  @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        if (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION) entityData.set(DISGUISE, random.nextInt(4));
        else if (isRevealed()) entityData.set(HIDE, random.nextInt(4));
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DISGUISE, REVEALED);
        entityData.define(HIDE, COW);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (DISGUISE.equals(key)) {
            refreshDimensions();
            if (!level().isClientSide && !isRevealed()) entityData.set(HIDE, getDisguise());
            // Only animate reveals that happen while we watch, not ones loaded with the chunk.
            if (level().isClientSide && isRevealed() && tickCount > 1) revealedAt = tickCount;
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return switch (getDisguise()) {
            case COW -> EntityDimensions.scalable(0.9F, 1.4F);
            case PIG -> EntityDimensions.scalable(0.9F, 0.9F);
            case SHEEP -> EntityDimensions.scalable(0.9F, 1.3F);
            case CHICKEN -> EntityDimensions.scalable(0.4F, 0.7F);
            default -> squeeze == null ? EntityDimensions.scalable(0.9F, 2.2F) : squeeze.dimensions(pose);
        };
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dims) {
        return dims.height * 0.85F;
    }

    @Override
    protected void registerGoals() {
        Pursuit.install(this, goalSelector, 0, this::isRevealed, 40, 90);
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.45D, true) {
            @Override
            public boolean canUse() {
                return isRevealed() && super.canUse();
            }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D) {
            @Override
            public boolean canUse() {
                return !isRevealed() && super.canUse();
            }
        });
        // It always, always watches you. That's the first tell.
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F, 1.0F));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return isRevealed() && super.canUse();
            }
        });
    }

    @Override
    public void tick() {
        // An animal does not crawl: only the true form squeezes.
        squeeze.tick(isRevealed());
        super.tick();
        if (level().isClientSide) return;
        if (!isRevealed()) {
            if (revealIn > 0 && --revealIn == 0) {
                reveal(level().getNearestPlayer(this, 24));
                return;
            }
            if (tickCount % 10 == 0) {
                Player p = level().getNearestPlayer(this, 8);
                if (p != null && !p.isCreative() && !p.isSpectator()) {
                    boolean close = p.distanceToSqr(this) < 9.0D;
                    long day = level().getDayTime() % 24000L;
                    boolean night = day > 12800L && day < 23200L;
                    if (close || (night && random.nextInt(40) == 0)) reveal(p);
                }
            }
        } else {
            huntTicks++;
            idleHunt = getTarget() == null ? idleHunt + 1 : 0;
            if (idleHunt > 600) {
                // It goes back into the ground it came out of.
                ((ServerLevel) level()).sendParticles(ModParticles.INK_DRIP.get(), getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.02);
                discard();
            }
        }
    }

    public void onLensed(Player player) {
        if (!isRevealed() && revealIn < 0) revealIn = 20;
    }

    private void reveal(@Nullable Player cause) {
        if (isRevealed()) return;
        entityData.set(DISGUISE, REVEALED);
        playSound(ModSounds.COPYIST_REVEAL.get(), 1.6F, 1.0F);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ModParticles.INK_DRIP.get(), getX(), getY() + 1, getZ(), 40, 0.5, 1.0, 0.5, 0.05);
        }
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(24))) {
            if (p instanceof ServerPlayer sp) {
                Advancements.grant(sp, "fourteen_cows");
                BleedManager.unlock(sp, "copyist");
                BleedManager.add(sp, 3F);
            }
        }
        if (cause != null && !cause.isCreative() && !cause.isSpectator()) setTarget(cause);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!isRevealed() && (held.is(Items.WHEAT) || held.is(Items.WHEAT_SEEDS) || held.is(Items.CARROT) || held.is(Items.BEETROOT_SEEDS))) {
            if (!level().isClientSide) reveal(player);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && !isRevealed()) reveal(source.getEntity() instanceof Player p ? p : null);
        return hurt;
    }

    @Override
    public float getVoicePitch() {
        // Almost right. Not quite.
        return isRevealed() ? super.getVoicePitch() : 0.72F + random.nextFloat() * 0.06F;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return switch (getDisguise()) {
            case COW -> SoundEvents.COW_AMBIENT;
            case PIG -> SoundEvents.PIG_AMBIENT;
            case SHEEP -> SoundEvents.SHEEP_AMBIENT;
            case CHICKEN -> SoundEvents.CHICKEN_AMBIENT;
            default -> ModSounds.COPYIST_AMBIENT.get();
        };
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.COPYIST_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.COPYIST_DEATH.get();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return isRevealed() && distance > 64 * 64;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Disguise", getDisguise());
        tag.putInt("Hide", getHide());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DISGUISE, tag.contains("Disguise") ? tag.getInt("Disguise") : REVEALED);
        if (tag.contains("Hide")) entityData.set(HIDE, Math.floorMod(tag.getInt("Hide"), 4));
    }
}
