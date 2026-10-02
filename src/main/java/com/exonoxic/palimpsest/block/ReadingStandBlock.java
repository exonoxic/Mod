package com.exonoxic.palimpsest.block;

import com.exonoxic.palimpsest.block.entity.ReadingStandBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** A slanted wooden stand. Offerings for a rite are laid on these around a Rubric Altar. */
public class ReadingStandBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(3, 0, 3, 13, 2, 13), Block.box(6, 2, 6, 10, 11, 10), Block.box(2, 11, 2, 14, 13, 14));

    public ReadingStandBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ReadingStandBlockEntity stand)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (stand.getItem().isEmpty()) {
            if (held.isEmpty()) return InteractionResult.PASS;
            if (!level.isClientSide) {
                stand.setItem(held.split(1));
                if (player.getAbilities().instabuild && held.isEmpty()) held.grow(1);
                level.playSound(null, pos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 0.8F, 1.1F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            ItemStack taken = stand.takeItem();
            if (!player.getInventory().add(taken)) player.drop(taken, false);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ReadingStandBlockEntity stand && !stand.getItem().isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stand.getItem());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReadingStandBlockEntity(pos, state);
    }
}
