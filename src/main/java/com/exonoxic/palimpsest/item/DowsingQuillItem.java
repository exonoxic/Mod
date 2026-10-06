package com.exonoxic.palimpsest.item;

import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModPoiTypes;
import com.exonoxic.palimpsest.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * A black quill on a thread. Held in hand it twitches toward palimpsest stone and swings
 * hard near Tears and gates. Strength 0-3 drives the item model.
 */
public class DowsingQuillItem extends Item {
    public static final String STRENGTH = "Strength";

    public DowsingQuillItem(Properties properties) {
        super(properties);
    }

    public static int strength(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(STRENGTH) : 0;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player player)) return;
        boolean held = selected || player.getOffhandItem() == stack;
        if (!held || (level.getGameTime() + entity.getId()) % 40 != 0) return;
        int strength = 0;
        BlockPos pos = player.blockPosition();
        Optional<BlockPos> rift = server.getPoiManager().findClosest(h -> h.is(ModPoiTypes.TEAR.getKey()) || h.is(ModPoiTypes.UNDERTEXT_VEIL.getKey()),
                pos, 48, PoiManager.Occupancy.ANY);
        if (rift.isPresent()) {
            double d = Math.sqrt(rift.get().distSqr(pos));
            strength = d < 12 ? 3 : d < 28 ? 2 : 1;
        }
        if (strength < 2) {
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-6, -6, -6), pos.offset(6, 6, 6))) {
                BlockState s = level.getBlockState(p);
                if (s.is(ModBlocks.PALIMPSEST_STONE.get()) || s.is(ModBlocks.DEEPSLATE_PALIMPSEST_STONE.get())) {
                    strength = Math.max(strength, p.distSqr(pos) < 16 ? 2 : 1);
                    if (strength >= 2) break;
                }
            }
        }
        if (strength(stack) != strength) stack.getOrCreateTag().putInt(STRENGTH, strength);
        if (strength == 3) level.playSound(null, pos, ModSounds.QUILL_SCRATCH.get(), SoundSource.PLAYERS, 0.4F, 1.4F);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !oldStack.is(newStack.getItem());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tooltips.lore(this, tooltip);
        Tooltips.use(this, tooltip);
    }
}
