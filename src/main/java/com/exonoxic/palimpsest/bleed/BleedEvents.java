package com.exonoxic.palimpsest.bleed;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.config.CommonConfig;
import com.exonoxic.palimpsest.entity.QuillcrowEntity;
import com.exonoxic.palimpsest.entity.SmudgeEntity;
import com.exonoxic.palimpsest.horror.HorrorDirector;
import com.exonoxic.palimpsest.horror.WorldAlterations;
import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModEffects;
import com.exonoxic.palimpsest.registry.ModItems;
import com.exonoxic.palimpsest.registry.ModPoiTypes;
import com.exonoxic.palimpsest.registry.ModTags;
import com.exonoxic.palimpsest.util.Advancements;
import com.exonoxic.palimpsest.world.WardHelper;
import com.exonoxic.palimpsest.world.dimension.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Wires the Bleed capability into the game: lifecycle, passive sources and sleep. */
@Mod.EventBusSubscriber(modid = Palimpsest.MODID)
public final class BleedEvents {
    public static final TagKey<EntityType<?>> INKBORN = ModTags.INKBORN;

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            BleedCapability.Provider provider = new BleedCapability.Provider();
            event.addCapability(BleedCapability.KEY, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        BleedData old = BleedCapability.get(event.getOriginal());
        BleedData fresh = BleedCapability.get(event.getEntity());
        fresh.copyFrom(old);
        event.getOriginal().invalidateCaps();
        // Dying doesn't wash the ink out, but it does lose a little of what you'd read.
        if (event.isWasDeath()) fresh.setBleed(fresh.getBleed() * 0.95F);
        fresh.setNextEventTime(-1L);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            BleedData data = BleedCapability.get(sp);
            if (data.getFirstSeen() < 0) data.setFirstSeen(sp.level().getGameTime());
            data.markAllDirty();
            BleedManager.sync(sp);
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            BleedCapability.get(sp).markAllDirty();
            BleedManager.sync(sp);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        BleedData data = BleedCapability.get(sp);
        if (event.getTo() == ModDimensions.UNDERTEXT) {
            if (BleedManager.addOnce(sp, "entered_undertext", 80F)) {
                BleedManager.unlock(sp, "undertext");
                Advancements.grant(sp, "the_undertext");
                sp.displayClientMessage(Component.translatable("message.palimpsest.first_undertext")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
            }
        }
        data.setNextEventTime(-1L);
        data.markAllDirty();
        BleedManager.sync(sp);
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer sp)) return;
        BleedData data = BleedCapability.get(sp);
        long t = sp.level().getGameTime() + (sp.getId() * 7L); // stagger players across ticks

        if (t % 20 == 0) HorrorDirector.tick(sp, data);
        if (t % 100 == 0) {
            Discovery.tick(sp, data);
            if (WardHelper.isWarded(sp.level(), sp.blockPosition(), 6)) {
                sp.addEffect(new MobEffectInstance(ModEffects.WARDED.get(), 240, 0, true, false, true));
            }
        }
        if (t % 600 == 0) passive(sp, data);
        if (data.isDirty() && t % 10 == 0) BleedManager.sync(sp);
    }

    /** Slow sources applied every 30 seconds. */
    private static void passive(ServerPlayer sp, BleedData data) {
        long grace = CommonConfig.GRACE_PERIOD_DAYS.get() * 24000L;
        boolean graceOver = data.getFirstSeen() >= 0 && sp.level().getGameTime() - data.getFirstSeen() >= grace;
        if (sp.level().dimension() == ModDimensions.UNDERTEXT) {
            BleedManager.add(sp, 1.0F);
            return;
        }
        if (!graceOver || sp.isCreative() && data.getBleed() == 0F) return;
        // The page thins on its own, slowly, up to Bleed-through; past that it takes a reason.
        // Roughly: Faint Trace an hour after the grace period, Ghosting a few hours later.
        float drift = data.getBleed() < BleedStage.GHOSTING.threshold ? 0.4F
                : data.getBleed() < BleedStage.BLEED_THROUGH.threshold ? 0.15F : 0F;
        if (drift > 0F) {
            boolean underSky = sp.level().canSeeSky(sp.blockPosition());
            if (underSky && sp.level().isNight() || !underSky && sp.getY() < 0) drift *= 1.5F;
            BleedManager.add(sp, drift);
        }
        int sinceRest = sp.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        if (sinceRest > 72000 && data.getStage().atLeast(BleedStage.FAINT_TRACE)) {
            BleedManager.add(sp, 0.5F);
            if (data.setFlag("insomnia_hint")) {
                sp.displayClientMessage(Component.translatable("message.palimpsest.insomnia").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
        }
        if (sp.serverLevel().getPoiManager().getCountInRange(h -> h.is(ModPoiTypes.TEAR.getKey()), sp.blockPosition(), 16,
                PoiManager.Occupancy.ANY) > 0) {
            BleedManager.add(sp, 1.0F);
        }
        if (sp.getInventory().contains(new net.minecraft.world.item.ItemStack(ModItems.LAST_FOLIO.get()))) BleedManager.add(sp, 0.5F);
    }

    @SubscribeEvent
    public static void sleepCheck(PlayerSleepInBedEvent event) {
        Player player = event.getEntity();
        if (player.level().dimension() == ModDimensions.UNDERTEXT) {
            event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
            player.displayClientMessage(Component.translatable("message.palimpsest.no_sleep_undertext")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }

    @SubscribeEvent
    public static void wake(PlayerWakeUpEvent event) {
        // Slept through the night: the level woke everyone (not an early exit from bed).
        if (event.wakeImmediately() || event.updateLevel()) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        BleedData data = BleedCapability.get(sp);
        boolean warded = WardHelper.isWarded(sp.level(), sp.blockPosition(), 8);
        if (warded && CommonConfig.BLEED_DECAYS_WHILE_RESTING.get()) {
            BleedManager.add(sp, -25F);
            if (data.getStage().atLeast(BleedStage.FAINT_TRACE)) {
                sp.displayClientMessage(Component.translatable("message.palimpsest.dream_warded").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
            return;
        }
        if (!data.getStage().atLeast(BleedStage.FAINT_TRACE)) return;
        BleedManager.add(sp, 4F);
        int line = sp.getRandom().nextInt(6);
        sp.displayClientMessage(Component.translatable("message.palimpsest.dream." + line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        // Late on, things move while you sleep.
        if (data.getStage().atLeast(BleedStage.RUNNING_INK) && CommonConfig.ALLOW_WORLD_ALTERATION.get() && sp.getRandom().nextFloat() < 0.4F) {
            WorldAlterations.openDoor(sp.serverLevel(), sp, 12);
            WorldAlterations.snuffTorches(sp.serverLevel(), sp, 10, 2, true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        Entity dead = event.getEntity();
        if (dead instanceof QuillcrowEntity) {
            // It will be missed. Something will notice who did it.
            BleedManager.add(sp, 15F);
            BleedManager.unlock(sp, "watchers");
        } else if (dead instanceof SmudgeEntity) {
            BleedManager.add(sp, 6F);
        } else if (dead.getType().is(INKBORN) && sp.level().dimension() != ModDimensions.UNDERTEXT) {
            BleedManager.add(sp, 2F);
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity().hasEffect(ModEffects.ERASURE.get())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer sp
                && (event.getState().is(ModBlocks.PALIMPSEST_STONE.get()) || event.getState().is(ModBlocks.DEEPSLATE_PALIMPSEST_STONE.get()))) {
            BleedManager.add(sp, 2F);
            BleedManager.unlock(sp, "palimpsest_stone");
        }
    }

    private BleedEvents() {}
}
