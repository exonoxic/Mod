package com.exonoxic.palimpsest.entity;

import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.item.TornFolioItem;
import com.exonoxic.palimpsest.registry.ModItems;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.util.Advancements;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The last scribe of the First Draft. Rubricators wrote the red headings in old books, and red
 * ink — cinnabar, a mineral — did not scrape away with the rest. So he is still here, mostly.
 * He trades what he has kept for what you have found, and says a few things, not all of them
 * to you.
 */
public class RubricatorEntity extends AbstractVillager {
    private static final int LINES = 12;
    private final Set<UUID> greeted = new HashSet<>();
    private int visitorTicks = -1;

    public RubricatorEntity(EntityType<? extends AbstractVillager> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0D).add(Attributes.MOVEMENT_SPEED, 0.45D);
    }

    /** Summoned by the Rite of Invitation: he stays for a while, then goes back down. */
    public void setVisitor(int ticks) {
        this.visitorTicks = ticks;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, LonghandEntity.class, 16.0F, 0.6D, 0.8D));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, RedactedEntity.class, 12.0F, 0.6D, 0.8D));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.35D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && visitorTicks > 0 && --visitorTicks == 0 && !isTrading()) discard();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive() || isTrading() || player.isSecondaryUseActive()) return super.mobInteract(player, hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            if (greeted.add(sp.getUUID())) {
                int line = random.nextInt(LINES);
                sp.sendSystemMessage(Component.translatable("message.palimpsest.rubricator.say",
                        getDisplayName(), Component.translatable("message.palimpsest.rubricator." + line)).withStyle(ChatFormatting.GRAY));
                BleedManager.unlock(sp, "rubricator");
            }
            Advancements.grant(sp, "the_rubricator");
        }
        if (getOffers().isEmpty()) return InteractionResult.CONSUME;
        setTradingPlayer(player);
        openTradingScreen(player, getDisplayName(), 1);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = getOffers();
        ItemStack page = new ItemStack(ModItems.FADED_PAGE.get());
        ItemStack fragment = new ItemStack(ModItems.UNDERTEXT_FRAGMENT.get());
        offers.add(new MerchantOffer(page.copyWithCount(4), TornFolioItem.of(ModItems.TORN_FOLIO.get(), 7), 1, 10, 0.0F));
        offers.add(new MerchantOffer(page.copyWithCount(6), TornFolioItem.of(ModItems.TORN_FOLIO.get(), 8), 1, 10, 0.0F));
        offers.add(new MerchantOffer(page.copyWithCount(8), TornFolioItem.of(ModItems.TORN_FOLIO.get(), 9), 1, 10, 0.0F));
        offers.add(new MerchantOffer(new ItemStack(ModItems.VELLUM_SCRAP.get(), 6), new ItemStack(ModItems.VERMILION.get(), 2), 16, 2, 0.05F));
        offers.add(new MerchantOffer(fragment.copyWithCount(2), new ItemStack(ModItems.BOOKMARK.get()), 6, 5, 0.05F));
        offers.add(new MerchantOffer(new ItemStack(ModItems.BLOTBERRIES.get(), 8), new ItemStack(ModItems.SEALING_WAX.get(), 2), 12, 2, 0.05F));
        offers.add(new MerchantOffer(new ItemStack(ModItems.ILLUMINE_LEAF.get(), 2), fragment.copyWithCount(1), new ItemStack(ModItems.READING_LENS.get()), 3, 8, 0.05F));
        offers.add(new MerchantOffer(new ItemStack(ModItems.PALE_ANTLER.get(), 1), page.copyWithCount(4), new ItemStack(ModItems.MARGIN_COMPASS.get()), 2, 8, 0.05F));
        offers.add(new MerchantOffer(fragment.copyWithCount(5), new ItemStack(ModItems.ILLUMINE_LEAF.get()), new ItemStack(ModItems.FOLIO_OF_DESCENT.get()), 1, 15, 0.0F));
        offers.add(new MerchantOffer(page.copyWithCount(12), new ItemStack(Items.MUSIC_DISC_13), new ItemStack(ModItems.MUSIC_DISC_LOWER_WRITING.get()), 1, 20, 0.0F));
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        if (offer.shouldRewardExp()) {
            level().addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(level(), getX(), getY() + 0.5D, getZ(), 3 + random.nextInt(4)));
        }
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public void die(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer sp) {
            // The last of the rubricators. The Undertext remembers who ended him.
            BleedManager.add(sp, 40F);
            sp.displayClientMessage(Component.translatable("message.palimpsest.rubricator.killed").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), false);
        }
        super.die(source);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isTrading() ? ModSounds.RUBRICATOR_YES.get() : ModSounds.RUBRICATOR_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.RUBRICATOR_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.RUBRICATOR_DEATH.get();
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return ModSounds.RUBRICATOR_YES.get();
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean positive) {
        return positive ? ModSounds.RUBRICATOR_YES.get() : ModSounds.RUBRICATOR_NO.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("VisitorTicks", visitorTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        visitorTicks = tag.contains("VisitorTicks") ? tag.getInt("VisitorTicks") : -1;
    }
}
