package com.exonoxic.palimpsest.item;

import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

/**
 * The reward for writing your name over the tear. The pen that wrote this world still works
 * for small things: "and then it was morning", "and then the rain stopped".
 */
public class ScrivenersQuillItem extends Item {
    public static final int COOLDOWN = 2400;

    public ScrivenersQuillItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(stack);
        long dayTime = server.getDayTime() % 24000L;
        boolean night = dayTime > 12800L && dayTime < 23200L;
        String line;
        if (night && server.dimensionType().natural()) {
            long next = server.getDayTime() + (24000L - dayTime) + 1000L;
            server.setDayTime(next);
            line = "message.palimpsest.quill.morning";
        } else if (server.isRaining()) {
            server.setWeatherParameters(12000, 0, false, false);
            line = "message.palimpsest.quill.clear";
        } else {
            player.displayClientMessage(Component.translatable("message.palimpsest.quill.nothing").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResultHolder.fail(stack);
        }
        server.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable(line, player.getDisplayName()).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
        server.sendParticles(ModParticles.GLYPH.get(), player.getX(), player.getY() + 1.5, player.getZ(), 40, 0.6, 0.6, 0.6, 0.03);
        level.playSound(null, player.blockPosition(), ModSounds.QUILL_SCRATCH.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tooltips.lore(this, tooltip);
        Tooltips.use(this, tooltip);
    }
}
