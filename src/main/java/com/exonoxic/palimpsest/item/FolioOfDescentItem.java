package com.exonoxic.palimpsest.item;

import com.exonoxic.palimpsest.config.CommonConfig;
import com.exonoxic.palimpsest.world.dimension.ModDimensions;
import com.exonoxic.palimpsest.world.gate.FolioGate;
import com.exonoxic.palimpsest.world.gate.GateTravel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Read aloud at a complete Folio Gate frame, at night, it opens the way down. */
public class FolioOfDescentItem extends Item {
    public FolioOfDescentItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!FolioGate.isFrame(level.getBlockState(context.getClickedPos()))) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server) || !(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
        if (!CommonConfig.ALLOW_GATES.get()) {
            player.displayClientMessage(Component.translatable("message.palimpsest.gate.disabled").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        FolioGate.Shape shape = FolioGate.find(level, context.getClickedPos());
        if (shape == null) {
            player.displayClientMessage(Component.translatable("message.palimpsest.gate.incomplete").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResult.FAIL;
        }
        long dayTime = level.getDayTime() % 24000L;
        boolean night = dayTime > 12800L && dayTime < 23200L;
        if (level.dimension() != ModDimensions.UNDERTEXT && !night) {
            player.displayClientMessage(Component.translatable("message.palimpsest.gate.need_night").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResult.FAIL;
        }
        GateTravel.openGate(server, shape, player);
        if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tooltips.lore(this, tooltip);
        Tooltips.use(this, tooltip);
    }
}
