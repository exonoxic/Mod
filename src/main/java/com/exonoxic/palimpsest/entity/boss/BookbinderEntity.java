package com.exonoxic.palimpsest.entity.boss;

import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.entity.MarginCrawlerEntity;
import com.exonoxic.palimpsest.entity.ai.Pursuit;
import com.exonoxic.palimpsest.entity.projectile.BindingNeedleEntity;
import com.exonoxic.palimpsest.horror.Spots;
import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModEntities;
import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.util.Advancements;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Bookbinder: what the Bindery made of Idris Wray when he tried to sew the tear shut from
 * the inside. A hunched man grown into a heap of stitched books, walking on six needles.
 *
 * <p>Phase one: needles thrown from range, a stabbing lunge up close, margin-crawlers shaken out
 * of its pages. At half health it <em>rebinds</em>: it becomes untouchable and draws itself back
 * together through three Stitched Tomes placed around the room — break them to cut the thread,
 * which leaves it stunned and open.</p>
 */
public class BookbinderEntity extends Monster implements PalimpsestBoss {
    private static final EntityDataAccessor<Boolean> REBINDING = SynchedEntityData.defineId(BookbinderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> STUNNED = SynchedEntityData.defineId(BookbinderEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(Component.translatable("entity.palimpsest.bookbinder"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6).setDarkenScreen(true);
    private final List<BlockPos> tomes = new ArrayList<>();
    private boolean rebound;
    private int needleCooldown = 60;
    private int crawlerCooldown = 200;

    public BookbinderEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 120;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 240.0D).add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D).add(Attributes.ARMOR, 10.0D).add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D);
    }

    public boolean isRebinding() {
        return entityData.get(REBINDING);
    }

    public boolean isStunned() {
        return entityData.get(STUNNED) > 0;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(REBINDING, false);
        entityData.define(STUNNED, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new Pursuit.Surface(this, () -> !isRebinding() && !isStunned()));
        goalSelector.addGoal(0, new Pursuit.Chase(this, () -> !isRebinding() && !isStunned()));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1D, true) {
            @Override
            public boolean canUse() {
                return !isRebinding() && !isStunned() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !isRebinding() && !isStunned() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        PalimpsestBoss.scaleForGroup(this);
        bossEvent.setProgress(getHealth() / getMaxHealth());
        ServerLevel level = (ServerLevel) level();

        int stun = entityData.get(STUNNED);
        if (stun > 0) {
            entityData.set(STUNNED, stun - 1);
            getNavigation().stop();
            return;
        }

        if (!rebound && getHealth() < getMaxHealth() * 0.5F) beginRebinding(level);
        if (isRebinding()) {
            tickRebinding(level);
            return;
        }

        LivingEntity target = getTarget();
        if (target == null) return;
        double d2 = distanceToSqr(target);
        if (--needleCooldown <= 0 && d2 > 16 && d2 < 24 * 24 && hasLineOfSight(target)) {
            throwNeedles(target);
            needleCooldown = 50 + random.nextInt(40);
        }
        if (--crawlerCooldown <= 0) {
            crawlerCooldown = 280 + random.nextInt(120);
            if (level.getEntitiesOfClass(MarginCrawlerEntity.class, getBoundingBox().inflate(16)).size() < 4) {
                for (int i = 0; i < 2; i++) {
                    MarginCrawlerEntity c = ModEntities.MARGIN_CRAWLER.get().create(level);
                    if (c == null) continue;
                    c.moveTo(getX() + random.nextInt(3) - 1, getY() + 1, getZ() + random.nextInt(3) - 1, random.nextFloat() * 360, 0);
                    c.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
                    level.addFreshEntity(c);
                }
                playSound(ModSounds.BOOKBINDER_AMBIENT.get(), 2.0F, 1.2F);
            }
        }
    }

    private void throwNeedles(LivingEntity target) {
        for (int i = -1; i <= 1; i++) {
            BindingNeedleEntity needle = new BindingNeedleEntity(level(), this);
            needle.setPos(getX(), getY() + getBbHeight() * 0.8, getZ());
            Vec3 to = target.getEyePosition().subtract(needle.position());
            double yawOffset = Math.toRadians(i * 9.0);
            double cos = Math.cos(yawOffset), sin = Math.sin(yawOffset);
            Vec3 dir = new Vec3(to.x * cos - to.z * sin, to.y + to.horizontalDistance() * 0.06, to.x * sin + to.z * cos);
            needle.shoot(dir.x, dir.y, dir.z, 1.4F, 2.0F);
            level().addFreshEntity(needle);
        }
        playSound(ModSounds.BOOKBINDER_NEEDLE.get(), 1.6F, 0.9F);
    }

    private void beginRebinding(ServerLevel level) {
        rebound = true;
        entityData.set(REBINDING, true);
        playSound(ModSounds.BOOKBINDER_REBIND.get(), 3.0F, 0.8F);
        tomes.clear();
        for (int i = 0; i < 3; i++) {
            double angle = (Math.PI * 2 / 3) * i + random.nextDouble() * 0.6;
            for (int attempt = 0; attempt < 8 && tomes.size() <= i; attempt++) {
                double r = 6 + random.nextDouble() * 4;
                int x = (int) Math.floor(getX() + Math.cos(angle) * r);
                int z = (int) Math.floor(getZ() + Math.sin(angle) * r);
                BlockPos floor = Spots.localFloor(level, x, (int) Math.floor(getY()), z, 4);
                if (floor != null && level.isEmptyBlock(floor)) {
                    level.setBlock(floor, ModBlocks.STITCHED_TOME.get().defaultBlockState(), 3);
                    tomes.add(floor);
                }
                angle += 0.3;
            }
        }
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(32))) {
            p.displayClientMessage(Component.translatable("message.palimpsest.bookbinder.rebind").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
        }
        if (tomes.isEmpty()) endRebinding();
    }

    private void tickRebinding(ServerLevel level) {
        getNavigation().stop();
        tomes.removeIf(p -> !level.getBlockState(p).is(ModBlocks.STITCHED_TOME.get()));
        if (tomes.isEmpty()) {
            endRebinding();
            return;
        }
        if (tickCount % 20 == 0 && getHealth() < getMaxHealth() * 0.65F) heal(2.0F);
        if (tickCount % 5 == 0) {
            for (BlockPos t : tomes) {
                Vec3 a = Vec3.atCenterOf(t);
                Vec3 b = position().add(0, getBbHeight() * 0.6, 0);
                for (int i = 0; i < 6; i++) {
                    Vec3 p = a.lerp(b, random.nextDouble());
                    level.sendParticles(ModParticles.RUBRIC_SPARK.get(), p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    private void endRebinding() {
        entityData.set(REBINDING, false);
        entityData.set(STUNNED, 120);
        playSound(ModSounds.BOOKBINDER_HURT.get(), 3.0F, 0.6F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isRebinding() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (source.getEntity() instanceof Player) playSound(ModSounds.BOOKBINDER_NEEDLE.get(), 1.0F, 1.6F);
            return false;
        }
        return super.hurt(source, isStunned() ? amount * 1.5F : amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            for (BlockPos t : tomes) if (level.getBlockState(t).is(ModBlocks.STITCHED_TOME.get())) level.removeBlock(t, false);
            for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(40))) {
                if (p instanceof ServerPlayer sp) {
                    Advancements.grant(sp, "spine");
                    BleedManager.unlock(sp, "bookbinder");
                    BleedManager.add(sp, 20F);
                    sp.sendSystemMessage(Component.translatable("message.palimpsest.bookbinder.death").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                }
            }
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean canRide(net.minecraft.world.entity.Entity vehicle) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.BOOKBINDER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.BOOKBINDER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BOOKBINDER_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Rebound", rebound);
        tag.putBoolean("Rebinding", isRebinding());
        ListTag list = new ListTag();
        for (BlockPos p : tomes) list.add(NbtUtils.writeBlockPos(p));
        tag.put("Tomes", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        rebound = tag.getBoolean("Rebound");
        entityData.set(REBINDING, tag.getBoolean("Rebinding"));
        tomes.clear();
        ListTag list = tag.getList("Tomes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) tomes.add(NbtUtils.readBlockPos(list.getCompound(i)));
        if (hasCustomName()) bossEvent.setName(getDisplayName());
    }
}
