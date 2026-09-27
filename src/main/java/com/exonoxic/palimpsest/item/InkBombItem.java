package com.exonoxic.palimpsest.item;

import com.exonoxic.palimpsest.entity.projectile.InkBombEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A stoppered bottle of iron-gall ink and powder. Blinds what it splashes; shows what hides. */
public class InkBombItem extends Item {
    public InkBombItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            InkBombEntity bomb = new InkBombEntity(level, player);
            bomb.setItem(stack);
            bomb.shootFromRotation(player, player.getXRot(), player.getYRot(), -10.0F, 0.9F, 1.0F);
            level.addFreshEntity(bomb);
        }
        player.getCooldowns().addCooldown(this, 10);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tooltips.lore(this, tooltip);
    }
}
