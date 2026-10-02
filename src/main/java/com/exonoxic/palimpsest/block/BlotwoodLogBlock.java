package com.exonoxic.palimpsest.block;

import com.exonoxic.palimpsest.registry.ModBlocks;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.Nullable;

public class BlotwoodLogBlock extends RotatedPillarBlock {
    public BlotwoodLogBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ToolAction action, boolean simulate) {
        if (action == ToolActions.AXE_STRIP) {
            return ModBlocks.STRIPPED_BLOTWOOD_LOG.get().defaultBlockState().setValue(AXIS, state.getValue(AXIS));
        }
        return super.getToolModifiedState(state, context, action, simulate);
    }
}
