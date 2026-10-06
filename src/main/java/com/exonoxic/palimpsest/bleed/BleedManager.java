package com.exonoxic.palimpsest.bleed;

import com.exonoxic.palimpsest.codex.CodexEntries;
import com.exonoxic.palimpsest.codex.CodexEntry;
import com.exonoxic.palimpsest.config.CommonConfig;
import com.exonoxic.palimpsest.item.CircletItem;
import com.exonoxic.palimpsest.item.RubricArmorItem;
import com.exonoxic.palimpsest.network.PacketHandler;
import com.exonoxic.palimpsest.network.SyncBleedPacket;
import com.exonoxic.palimpsest.network.SyncCodexPacket;
import com.exonoxic.palimpsest.registry.ModEffects;
import com.exonoxic.palimpsest.registry.ModItems;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.util.Advancements;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The single entry point for changing a player's Bleed. Every source funnels through
 * {@link #add}, which applies protection, configuration and ending rules, detects stage
 * changes and schedules a sync to the client.
 */
public final class BleedManager {

    public static BleedStage stage(Player player) {
        return BleedCapability.get(player).getStage();
    }

    /**
     * Adds (or removes, if negative) Bleed. Gains are scaled by protection and config;
     * reductions are applied as-is.
     *
     * @return the amount actually applied
     */
    public static float add(ServerPlayer player, float amount) {
        BleedData data = BleedCapability.get(player);
        float applied = amount > 0 ? amount * gainMultiplier(player, data) : amount;
        if (applied == 0) return 0;

        BleedStage before = data.getStage();
        float next = data.getBleed() + applied;
        float cap = CommonConfig.ENABLE_FINAL_STAGE.get() ? BleedStage.MAX : BleedStage.OVERWRITTEN.threshold - 0.01F;
        next = Math.min(next, cap);
        if (data.getEnding() == Ending.READ) next = Math.max(next, BleedStage.OVERWRITTEN.threshold);
        if (data.getEnding() == Ending.SEALED) next = Math.min(next, BleedStage.FAINT_TRACE.threshold - 1F);
        data.setBleed(next);

        BleedStage after = data.getStage();
        if (after != before) onStageChanged(player, data, before, after);
        return applied;
    }

    /** Adds Bleed only the first time the given flag is seen for this player. */
    public static boolean addOnce(ServerPlayer player, String flag, float amount) {
        BleedData data = BleedCapability.get(player);
        if (!data.setFlag(flag)) return false;
        add(player, amount);
        return true;
    }

    public static float gainMultiplier(ServerPlayer player, BleedData data) {
        if (data.getEnding() == Ending.SEALED) return 0F;
        float m = CommonConfig.BLEED_GAIN_MULTIPLIER.get().floatValue();
        if (player.hasEffect(ModEffects.WARDED.get())) m *= 0.5F;
        int rubric = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack worn = player.getItemBySlot(slot);
            if (worn.getItem() instanceof RubricArmorItem) rubric++;
            if (worn.getItem() instanceof CircletItem) m *= 1.25F;
        }
        m *= 1F - 0.1F * rubric;
        if (data.getEnding() == Ending.UNWRITTEN) m *= 0.5F;
        return m;
    }

    private static void onStageChanged(ServerPlayer player, BleedData data, BleedStage before, BleedStage after) {
        boolean rising = after.index > before.index;
        if (rising && after.index > data.getHighestStage()) {
            data.setHighestStage(after.index);
            unlock(player, "stage_" + after.index);
            player.displayClientMessage(Component.translatable("message.palimpsest.stage." + after.index)
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            if (after.index >= BleedStage.BLEED_THROUGH.index) {
                player.playNotifySound(ModSounds.EVENT_STINGER.get(), SoundSource.AMBIENT, 0.35F, 0.8F);
            }
            switch (after) {
                case FAINT_TRACE -> Advancements.grant(player, "root");
                case BLEED_THROUGH -> Advancements.grant(player, "bleed_through");
                case THE_TEAR -> Advancements.grant(player, "the_tear");
                case OVERWRITTEN -> Advancements.grant(player, "overwritten");
                default -> {}
            }
        }
        // Give the Director a breather after a stage change so the new stage isn't announced by spam.
        data.setNextEventTime(Math.max(data.getNextEventTime(), player.level().getGameTime() + 1200));
        sync(player);
    }

    /**
     * Records a Commonplace Book entry. Players carrying the book are told quietly; the entry is
     * waiting for everyone else when they eventually make one.
     */
    public static void unlock(ServerPlayer player, String entryId) {
        BleedData data = BleedCapability.get(player);
        if (!data.unlock(entryId)) return;
        CodexEntry entry = CodexEntries.get(entryId);
        if (entry != null && !entry.silent() && player.getInventory().contains(new ItemStack(ModItems.COMMONPLACE_BOOK.get()))) {
            player.displayClientMessage(Component.translatable("message.palimpsest.codex_entry",
                    Component.translatable(entry.titleKey())).withStyle(ChatFormatting.DARK_RED), true);
            player.playNotifySound(ModSounds.QUILL_SCRATCH.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
        }
        PacketHandler.sendTo(player, new SyncCodexPacket(data.getCodex()));
        data.clean();
    }

    public static void setEnding(ServerPlayer player, Ending ending) {
        BleedData data = BleedCapability.get(player);
        data.setEnding(ending);
        switch (ending) {
            case SEALED -> data.setBleed(0F);
            case READ -> data.setBleed(Math.max(data.getBleed(), BleedStage.OVERWRITTEN.threshold));
            case UNWRITTEN -> data.setBleed(Math.min(data.getBleed(), BleedStage.GHOSTING.threshold));
            default -> {}
        }
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        BleedData data = BleedCapability.get(player);
        PacketHandler.sendTo(player, new SyncBleedPacket(data.getBleed(), data.getEnding().ordinal()));
        if (data.isCodexDirty()) PacketHandler.sendTo(player, new SyncCodexPacket(data.getCodex()));
        data.clean();
    }

    private BleedManager() {}
}
