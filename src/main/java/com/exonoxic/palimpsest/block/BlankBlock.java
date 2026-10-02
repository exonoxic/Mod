package com.exonoxic.palimpsest.block;

import com.exonoxic.palimpsest.block.entity.BlankBlockEntity;
import com.exonoxic.palimpsest.registry.ModBlockEntities;
import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A place where the world has been scraped off the page. It remembers what was there and
 * writes it back when its time runs out. Hollow blanks cannot be stood on.
 */
public class BlankBlock extends BaseEntityBlock {
    public static final BooleanProperty HOLLOW = BooleanProperty.create("hollow");

    public BlankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HOLLOW, false));
    }

    /**
     * Scrapes one block. Refuses anything with a block entity, anything unbreakable, fluids and
     * air, so no item or machine can ever be lost to a blank.
     */
    public static boolean scrape(ServerLevel level, BlockPos pos, int restoreTicks, boolean hollow) {
        BlockState original = level.getBlockState(pos);
        if (original.isAir() || original.is(ModBlocks.BLANK.get()) || original.hasBlockEntity()) return false;
        if (!original.getFluidState().isEmpty() || original.getDestroySpeed(level, pos) < 0) return false;
        if (original.is(Blocks.BEDROCK)) return false;
        level.setBlock(pos, ModBlocks.BLANK.get().defaultBlockState().setValue(HOLLOW, hollow), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(pos) instanceof BlankBlockEntity blank) {
            blank.remember(original, level.getGameTime() + restoreTicks);
            return true;
        }
        level.setBlock(pos, original, Block.UPDATE_CLIENTS);
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HOLLOW);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HOLLOW) ? Shapes.empty() : Shapes.block();
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.BLANK.get(), BlankBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(ModParticles.ERASURE_MOTE.get(), pos.getX() + random.nextDouble(), pos.getY() + 1.02, pos.getZ() + random.nextDouble(), 0, 0.02, 0);
        }
    }
}
