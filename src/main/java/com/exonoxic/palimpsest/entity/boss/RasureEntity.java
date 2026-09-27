package com.exonoxic.palimpsest.entity.boss;

import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.block.BlankBlock;
import com.exonoxic.palimpsest.block.FolioStandBlock;
import com.exonoxic.palimpsest.block.RubricPillarBlock;
import com.exonoxic.palimpsest.entity.RedactedEntity;
import com.exonoxic.palimpsest.horror.HorrorEvents;
import com.exonoxic.palimpsest.horror.Spots;
import com.exonoxic.palimpsest.network.PacketHandler;
import com.exonoxic.palimpsest.network.VisionPacket;
import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModEffects;
import com.exonoxic.palimpsest.registry.ModEntities;
import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.util.Advancements;
import com.exonoxic.palimpsest.util.ServerScheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Rasure — the knife that scraped the First Draft off the page, left behind and still
 * working. A tall robed shape with a face of blank vellum and a blade for a hand.
 *
 * <ol>
 *   <li><b>The First Stroke</b> (100–66%): blade sweeps; red lines appear on the floor and a
 *   moment later the floor along them is scraped hollow. Watch your feet.</li>
 *   <li><b>Redaction</b>: it rises out of reach behind a guard and blanks the four Rubric
 *   Pillars. Re-ink each with vermilion or chalk while Redacted come for you. When all four
 *   burn again its guard breaks and it falls.</li>
 *   <li><b>Exposed</b> (66–33%): faster scraping.</li>
 *   <li><b>The Blank Page</b> (33–0%): everything goes white. It is invisible unless close, lit
 *   by a censer or splashed with ink, and it moves between breaths.</li>
 * </ol>
 */
public class RasureEntity extends Monster implements PalimpsestBoss {
    public static final int FIRST_STROKE = 1;
    public static final int REDACTION = 2;
    public static final int EXPOSED = 3;
    public static final int BLANK_PAGE = 4;
    private static final int ARENA_RADIUS = 22;

    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(RasureEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SHIELDED = SynchedEntityData.defineId(RasureEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(Component.translatable("entity.palimpsest.rasure"),
            BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(true).setCreateWorldFog(true);
    @Nullable
    private BlockPos home;
    private int floorY;
    private final List<BlockPos> pillars = new ArrayList<>();
    private int scrapeCooldown = 100;
    private int summonCooldown = 100;
    private int blinkCooldown = 120;
    private int revealed;
    private int emptyTicks;

    public RasureEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 500;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 600.0D).add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D).add(Attributes.ARMOR, 12.0D).add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    /** Called by the Rite of the Last Reading at the arena's altar. */
    public static void summon(ServerLevel level, BlockPos pos, @Nullable ServerPlayer reader) {
        RasureEntity r = ModEntities.RASURE.get().create(level);
        if (r == null) return;
        BlockPos altar = pos.below(2);
        r.home = altar;
        r.floorY = altar.getY() - 1;
        r.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360F, 0);
        r.findPillars(level);
        r.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
        level.addFreshEntity(r);
        level.playSound(null, pos, ModSounds.RASURE_PHASE.get(), SoundSource.HOSTILE, 4.0F, 0.7F);
        r.announce("message.palimpsest.rasure.arrive");
    }

    /** A pillar was re-inked. The nearest Rasure takes note. */
    public static void onPillarLit(ServerLevel level, BlockPos pos) {
        for (RasureEntity r : level.getEntitiesOfClass(RasureEntity.class, new AABB(pos).inflate(48))) r.pillarLit(pos);
    }

    public int getPhase() {
        return entityData.get(PHASE);
    }

    public boolean isShielded() {
        return entityData.get(SHIELDED);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(PHASE, FIRST_STROKE);
        entityData.define(SHIELDED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true) {
            @Override
            public boolean canUse() {
                return !isShielded() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !isShielded() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 32.0F));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    private void findPillars(ServerLevel level) {
        pillars.clear();
        if (home == null) return;
        for (BlockPos p : BlockPos.betweenClosed(home.offset(-ARENA_RADIUS, -4, -ARENA_RADIUS), home.offset(ARENA_RADIUS, 8, ARENA_RADIUS))) {
            if (level.getBlockState(p).is(ModBlocks.RUBRIC_PILLAR.get())) {
                BlockPos imm = p.immutable();
                // Only the top block of each pillar counts.
                if (!level.getBlockState(imm.above()).is(ModBlocks.RUBRIC_PILLAR.get())) pillars.add(imm);
            }
        }
    }

    private List<ServerPlayer> arenaPlayers() {
        List<ServerPlayer> list = new ArrayList<>();
        BlockPos c = home != null ? home : blockPosition();
        for (Player p : level().getEntitiesOfClass(Player.class, new AABB(c).inflate(48))) {
            if (p instanceof ServerPlayer sp && !sp.isSpectator()) list.add(sp);
        }
        return list;
    }

    private void announce(String key) {
        for (ServerPlayer p : arenaPlayers()) {
            p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC), true);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        PalimpsestBoss.scaleForGroup(this);
        ServerLevel level = (ServerLevel) level();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (home == null) {
            home = blockPosition();
            floorY = home.getY() - 1;
            findPillars(level);
        }

        List<ServerPlayer> players = arenaPlayers();
        if (players.isEmpty()) {
            if (++emptyTicks > 600) resetFight(level);
            return;
        }
        emptyTicks = 0;

        int phase = getPhase();
        if (phase == FIRST_STROKE && getHealth() < getMaxHealth() * 0.66F) enterRedaction(level);
        else if (phase == EXPOSED && getHealth() < getMaxHealth() * 0.33F) enterBlankPage();
        if (revealed > 0) revealed--;

        switch (getPhase()) {
            case FIRST_STROKE -> {
                if (--scrapeCooldown <= 0) {
                    scrapeLines(level, 2);
                    scrapeCooldown = 160;
                }
            }
            case REDACTION -> tickRedaction(level);
            case EXPOSED -> {
                if (--scrapeCooldown <= 0) {
                    scrapeLines(level, 3);
                    scrapeCooldown = 110;
                }
            }
            case BLANK_PAGE -> tickBlankPage(level, players);
            default -> {}
        }
    }

    private void tickRedaction(ServerLevel level) {
        getNavigation().stop();
        Vec3 hover = Vec3.atBottomCenterOf(home).add(0, 6, 0);
        Vec3 d = hover.subtract(position());
        setDeltaMovement(d.scale(0.05));
        if (tickCount % 10 == 0) level.sendParticles(ModParticles.ERASURE_MOTE.get(), getX(), getY() + 2, getZ(), 12, 1.0, 1.5, 1.0, 0.01);
        if (--summonCooldown <= 0) {
            summonCooldown = 300;
            if (level.getEntitiesOfClass(RedactedEntity.class, new AABB(home).inflate(32)).size() < 3) {
                for (int i = 0; i < 2; i++) {
                    RedactedEntity r = ModEntities.REDACTED.get().create(level);
                    if (r == null) continue;
                    BlockPos at = Spots.localFloor(level, home.getX() + random.nextInt(13) - 6, floorY + 1, home.getZ() + random.nextInt(13) - 6, 3);
                    if (at == null) continue;
                    r.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360, 0);
                    r.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.MOB_SUMMONED, null, null);
                    level.addFreshEntity(r);
                }
            }
        }
        if (--scrapeCooldown <= 0) {
            scrapeLines(level, 1);
            scrapeCooldown = 220;
        }
    }

    private void tickBlankPage(ServerLevel level, List<ServerPlayer> players) {
        if (tickCount % 80 == 0) {
            for (ServerPlayer p : players) PacketHandler.sendTo(p, VisionPacket.of(VisionPacket.WHITEOUT, 100, 1.0F));
        }
        boolean someoneClose = level.getNearestPlayer(this, 5.0D) != null;
        boolean visible = revealed > 0 || someoneClose || hasEffect(MobEffects.GLOWING);
        if (isInvisible() == visible) setInvisible(!visible);
        LivingEntity target = getTarget();
        if (target != null && --blinkCooldown <= 0) {
            blinkCooldown = 120 + random.nextInt(60);
            Vec3 behind = target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(5));
            BlockPos floor = Spots.localFloor(level, (int) Math.floor(behind.x), floorY + 1, (int) Math.floor(behind.z), 3);
            if (floor != null && Spots.standable(level, floor, 5)) {
                teleportTo(floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5);
                level.playSound(null, floor, ModSounds.EVENT_FOOTSTEPS.get(), SoundSource.HOSTILE, 1.4F, 0.7F);
            }
        }
    }

    private void enterRedaction(ServerLevel level) {
        if (pillars.isEmpty()) {
            entityData.set(PHASE, EXPOSED);
            return;
        }
        entityData.set(PHASE, REDACTION);
        entityData.set(SHIELDED, true);
        setNoGravity(true);
        for (BlockPos p : pillars) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.RUBRIC_PILLAR.get())) level.setBlock(p, s.setValue(RubricPillarBlock.LIT, false), 3);
            level.sendParticles(ModParticles.ERASURE_MOTE.get(), p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5, 30, 0.4, 0.8, 0.4, 0.02);
        }
        level.playSound(null, blockPosition(), ModSounds.RASURE_PHASE.get(), SoundSource.HOSTILE, 4.0F, 1.0F);
        announce("message.palimpsest.rasure.redaction");
    }

    private void pillarLit(BlockPos pos) {
        if (getPhase() != REDACTION || !(level() instanceof ServerLevel level)) return;
        Vec3 from = Vec3.atCenterOf(pos).add(0, 0.6, 0);
        Vec3 to = position().add(0, getBbHeight() * 0.6, 0);
        for (int i = 0; i < 40; i++) {
            Vec3 p = from.lerp(to, i / 40.0);
            level.sendParticles(ModParticles.RUBRIC_SPARK.get(), p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
        }
        playSound(ModSounds.RASURE_HURT.get(), 3.0F, 1.3F);
        for (BlockPos p : pillars) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.RUBRIC_PILLAR.get()) && !s.getValue(RubricPillarBlock.LIT)) return;
        }
        breakShield();
    }

    private void breakShield() {
        entityData.set(SHIELDED, false);
        entityData.set(PHASE, EXPOSED);
        setNoGravity(false);
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 5));
        playSound(ModSounds.RASURE_PHASE.get(), 4.0F, 0.6F);
        announce("message.palimpsest.rasure.exposed");
    }

    private void enterBlankPage() {
        entityData.set(PHASE, BLANK_PAGE);
        setInvisible(true);
        playSound(ModSounds.RASURE_PHASE.get(), 4.0F, 1.4F);
        announce("message.palimpsest.rasure.blank_page");
    }

    private void resetFight(ServerLevel level) {
        setHealth(getMaxHealth());
        entityData.set(PHASE, FIRST_STROKE);
        entityData.set(SHIELDED, false);
        setNoGravity(false);
        setInvisible(false);
        emptyTicks = 0;
        for (BlockPos p : pillars) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.RUBRIC_PILLAR.get())) level.setBlock(p, s.setValue(RubricPillarBlock.LIT, true), 3);
        }
        if (home != null) teleportTo(home.getX() + 0.5, home.getY() + 2, home.getZ() + 0.5);
    }

    /** Red lines through the target's position; a moment later the floor along them goes hollow. */
    private void scrapeLines(ServerLevel level, int count) {
        LivingEntity target = getTarget();
        if (target == null || home == null) return;
        playSound(ModSounds.RASURE_SCRAPE.get(), 3.0F, 0.9F + random.nextFloat() * 0.2F);
        for (int i = 0; i < count; i++) {
            float yaw = random.nextFloat() * 180F;
            BlockPos origin = new BlockPos(target.getBlockX(), floorY, target.getBlockZ());
            HorrorEvents.telegraphLine(level, origin.above(), yaw, 30);
            BlockPos center = home;
            int fy = floorY;
            ServerScheduler.schedule(30, () -> {
                if (isRemoved()) return;
                double dx = -Math.sin(Math.toRadians(yaw));
                double dz = Math.cos(Math.toRadians(yaw));
                for (int s = -15; s <= 15; s++) {
                    for (int w = 0; w < 2; w++) {
                        int x = (int) Math.floor(origin.getX() + 0.5 + dx * s + (w == 1 ? dz : 0));
                        int z = (int) Math.floor(origin.getZ() + 0.5 + dz * s - (w == 1 ? dx : 0));
                        double hx = x - center.getX(), hz = z - center.getZ();
                        double r2 = hx * hx + hz * hz;
                        if (r2 < 9 || r2 > ARENA_RADIUS * ARENA_RADIUS) continue;
                        BlankBlock.scrape(level, new BlockPos(x, fy, z), 200, true);
                    }
                }
                level.playSound(null, origin, ModSounds.BLANK_ERASE.get(), SoundSource.HOSTILE, 2.0F, 0.8F);
            });
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (!hit) return false;
        // The blade sweeps: anyone else in front of it is caught too.
        Vec3 look = getLookAngle();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.0D), e -> e != this && e != target)) {
            Vec3 to = e.position().subtract(position());
            if (to.normalize().dot(look) > 0.3) e.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5F);
        }
        if (target instanceof LivingEntity le && getPhase() == BLANK_PAGE) {
            le.addEffect(new MobEffectInstance(ModEffects.ERASURE.get(), 100, 1));
        }
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isShielded() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (source.getEntity() instanceof Player) playSound(ModSounds.RASURE_SCRAPE.get(), 1.0F, 1.8F);
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && getPhase() == BLANK_PAGE) revealed = Math.max(revealed, 20);
        return hurt;
    }

    @Override
    public void onRevealed(Player by) {
        revealed = 100;
        if (getPhase() == BLANK_PAGE) setInvisible(false);
    }

    @Override
    protected void tickDeath() {
        ++deathTime;
        if (level() instanceof ServerLevel server && deathTime % 2 == 0) {
            server.sendParticles(ModParticles.GLYPH.get(), getX(), getY() + deathTime * 0.05, getZ(), 6, 0.6, 1.2, 0.6, 0.02);
            server.sendParticles(ModParticles.ERASURE_MOTE.get(), getX(), getY() + 1.5, getZ(), 6, 0.8, 1.5, 0.8, 0.05);
        }
        setDeltaMovement(0, 0.02, 0);
        if (deathTime >= 60 && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel level)) return;
        setInvisible(false);
        if (home != null) {
            BlockPos stand = home.above();
            if (level.getBlockState(stand).canBeReplaced()) {
                level.setBlock(stand, ModBlocks.FOLIO_STAND.get().defaultBlockState().setValue(FolioStandBlock.FACING, Direction.NORTH), 3);
            }
        }
        for (ServerPlayer p : arenaPlayers()) {
            PacketHandler.sendTo(p, VisionPacket.of(VisionPacket.WHITEOUT, 0, 0F));
            Advancements.grant(p, "rasure");
            BleedManager.unlock(p, "rasure");
            BleedManager.unlock(p, "the_choice");
            BleedManager.add(p, 40F);
            p.sendSystemMessage(Component.translatable("message.palimpsest.rasure.death").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
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
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.RASURE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.RASURE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.RASURE_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 3.0F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", getPhase());
        tag.putBoolean("Shielded", isShielded());
        if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
        tag.putInt("FloorY", floorY);
        ListTag list = new ListTag();
        for (BlockPos p : pillars) list.add(NbtUtils.writeBlockPos(p));
        tag.put("Pillars", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(PHASE, Math.max(FIRST_STROKE, tag.getInt("Phase")));
        entityData.set(SHIELDED, tag.getBoolean("Shielded"));
        if (tag.contains("Home")) home = NbtUtils.readBlockPos(tag.getCompound("Home"));
        floorY = tag.getInt("FloorY");
        pillars.clear();
        ListTag list = tag.getList("Pillars", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) pillars.add(NbtUtils.readBlockPos(list.getCompound(i)));
        if (isShielded()) setNoGravity(true);
    }
}
