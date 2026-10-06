package com.exonoxic.palimpsest.item;

import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.util.Advancements;
import com.exonoxic.palimpsest.world.dimension.ModDimensions;
import com.exonoxic.palimpsest.world.gate.GateTravel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Keeps your place. Used above, it remembers where you are standing. Used below, in the
 * Undertext, it takes you back to the marked place (or home) — and is used up.
 */
public class BookmarkItem extends Item {
    private static final String MARK = "Mark";

    public BookmarkItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(MARK);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 40;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.dimension() == ModDimensions.UNDERTEXT) {
            player.startUsingItem(hand);
            level.playSound(player, player.blockPosition(), ModSounds.PAGE_TURN.get(), SoundSource.PLAYERS, 0.8F, 0.6F);
            return InteractionResultHolder.consume(stack);
        }
        if (level.dimension() != Level.OVERWORLD) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide) {
            stack.getOrCreateTag().put(MARK, NbtUtils.writeBlockPos(player.blockPosition()));
            player.displayClientMessage(Component.translatable("message.palimpsest.bookmark.marked").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        level.playSound(player, player.blockPosition(), ModSounds.BOOKMARK_USE.get(), SoundSource.PLAYERS, 0.8F, 1.2F);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level instanceof ServerLevel server && entity instanceof ServerPlayer player && level.dimension() == ModDimensions.UNDERTEXT) {
            CompoundTag tag = stack.getTag();
            BlockPos mark = tag != null && tag.contains(MARK) ? NbtUtils.readBlockPos(tag.getCompound(MARK)) : null;
            server.sendParticles(ModParticles.GLYPH.get(), player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
            server.playSound(null, player.blockPosition(), ModSounds.BOOKMARK_USE.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
            GateTravel.returnToOverworld(player, mark);
            Advancements.grant(player, "keep_your_place");
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tooltips.lore(this, tooltip);
        Tooltips.use(this, tooltip);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(MARK)) {
            BlockPos p = NbtUtils.readBlockPos(tag.getCompound(MARK));
            tooltip.add(Component.translatable("item.palimpsest.bookmark.marked", p.getX(), p.getY(), p.getZ()).withStyle(ChatFormatting.DARK_RED));
        }
    }
}
