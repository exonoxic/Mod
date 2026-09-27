package com.exonoxic.palimpsest.item;

import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.world.ModStructures;
import com.exonoxic.palimpsest.world.dimension.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A compass whose needle is a sliver of undertext. It points at the nearest place where the
 * page is thin: Overworld ruins, the Bindery below, or — once attuned by the Rite of Binding —
 * the Last Folio itself.
 */
public class MarginCompassItem extends Item {
    public static final String TARGET = "Target";
    public static final String DIMENSION = "TargetDimension";
    public static final String ATTUNED = "Attuned";

    public MarginCompassItem(Properties properties) {
        super(properties);
    }

    public static boolean isAttuned(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(ATTUNED);
    }

    @Nullable
    public static BlockPos getTarget(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TARGET)) return null;
        if (!level.dimension().location().toString().equals(tag.getString(DIMENSION))) return null;
        return NbtUtils.readBlockPos(tag.getCompound(TARGET));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isAttuned(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(stack);
        TagKey<Structure> targets;
        if (level.dimension() == ModDimensions.UNDERTEXT) {
            targets = isAttuned(stack) ? ModStructures.COMPASS_ATTUNED : ModStructures.COMPASS_UNDERTEXT;
        } else {
            targets = ModStructures.COMPASS_OVERWORLD;
        }
        BlockPos found = server.findNearestMapStructure(targets, player.blockPosition(), 64, false);
        CompoundTag tag = stack.getOrCreateTag();
        if (found == null) {
            tag.remove(TARGET);
            player.displayClientMessage(Component.translatable("message.palimpsest.compass.spins").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        } else {
            tag.put(TARGET, NbtUtils.writeBlockPos(found));
            tag.putString(DIMENSION, level.dimension().location().toString());
            player.displayClientMessage(Component.translatable("message.palimpsest.compass.settles").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        level.playSound(null, player.blockPosition(), ModSounds.LENS_FOCUS.get(), SoundSource.PLAYERS, 0.6F, 0.7F);
        player.getCooldowns().addCooldown(this, 60);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tooltips.lore(this, tooltip);
        Tooltips.use(this, tooltip);
        if (isAttuned(stack)) tooltip.add(Component.translatable("item.palimpsest.margin_compass.attuned").withStyle(ChatFormatting.DARK_RED));
    }
}
