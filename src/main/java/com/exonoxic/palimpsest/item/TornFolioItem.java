package com.exonoxic.palimpsest.item;

import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.client.ClientHooks;
import com.exonoxic.palimpsest.codex.CodexEntries;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.util.Advancements;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One of Idris Wray's twelve surviving folios. The number lives in NBT ({@code Folio}) so loot
 * tables and the Rubricator can hand out specific pages.
 */
public class TornFolioItem extends Item {
    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};

    public TornFolioItem(Properties properties) {
        super(properties);
    }

    public static int number(ItemStack stack) {
        int n = stack.hasTag() ? stack.getTag().getInt("Folio") : 0;
        return n < 1 || n > CodexEntries.FOLIO_COUNT ? 1 : n;
    }

    public static ItemStack of(Item item, int number) {
        ItemStack s = new ItemStack(item);
        s.getOrCreateTag().putInt("Folio", number);
        return s;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.palimpsest.torn_folio.numbered", ROMAN[number(stack) - 1]);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int n = number(stack);
        if (player instanceof ServerPlayer sp) {
            BleedManager.addOnce(sp, "read:folio_" + n, 12F);
            BleedManager.unlock(sp, "folio_" + n);
            Advancements.grant(sp, "wray_was_here");
            if (n == CodexEntries.FOLIO_COUNT) Advancements.grant(sp, "cartographer");
        } else {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openLore("folio", n));
        }
        level.playSound(player, player.blockPosition(), ModSounds.PAGE_TURN.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tooltips.lore(this, tooltip);
    }
}
