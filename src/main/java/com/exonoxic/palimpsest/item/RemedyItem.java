package com.exonoxic.palimpsest.item;

import com.exonoxic.palimpsest.bleed.BleedCapability;
import com.exonoxic.palimpsest.bleed.BleedData;
import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Things that settle the page. Candlewort tea lowers Bleed a little (once every five minutes);
 * sealing wax, chewed, stops Erasure and clears ink from the eyes.
 */
public class RemedyItem extends Item {
    public enum Kind { TEA, WAX }

    private static final long TEA_COOLDOWN = 6000L;
    private final Kind kind;

    public RemedyItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return kind == Kind.TEA ? UseAnim.DRINK : UseAnim.EAT;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) {
            BleedData data = BleedCapability.get(sp);
            if (kind == Kind.TEA) {
                long now = level.getGameTime();
                if (now - data.getLastRemedyTime() >= TEA_COOLDOWN) {
                    BleedManager.add(sp, -25F);
                    data.setLastRemedyTime(now);
                    sp.displayClientMessage(Component.translatable("message.palimpsest.tea.calm").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                } else {
                    sp.displayClientMessage(Component.translatable("message.palimpsest.tea.too_soon").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                }
                sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
            } else {
                sp.removeEffect(ModEffects.ERASURE.get());
                sp.removeEffect(ModEffects.INKBLIND.get());
                BleedManager.add(sp, -15F);
            }
        }
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
            if (kind == Kind.TEA) {
                if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
                if (!player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tooltips.lore(this, tooltip);
    }
}
