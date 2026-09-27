package com.exonoxic.palimpsest.item;

import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.client.ClientHooks;
import com.exonoxic.palimpsest.codex.CodexEntries;
import com.exonoxic.palimpsest.registry.ModSounds;
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
 * A page from the First Draft: the world before this one. Most of the words have been scraped
 * off. Which page it is gets decided the first time someone reads it.
 */
public class FadedPageItem extends Item {
    public FadedPageItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            if (!stack.getOrCreateTag().contains("Page")) {
                stack.getOrCreateTag().putInt("Page", 1 + level.random.nextInt(CodexEntries.FADED_PAGE_COUNT));
            }
            int page = stack.getTag().getInt("Page");
            BleedManager.addOnce(sp, "read:faded_" + page, 6F);
            BleedManager.unlock(sp, "faded_pages");
            return InteractionResultHolder.success(stack);
        }
        int page = stack.hasTag() ? stack.getTag().getInt("Page") : 0;
        if (page > 0) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openLore("faded", page));
            level.playSound(player, player.blockPosition(), ModSounds.PAGE_TURN.get(), SoundSource.PLAYERS, 0.8F, 0.8F);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tooltips.lore(this, tooltip);
    }
}
