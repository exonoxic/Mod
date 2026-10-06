package com.exonoxic.palimpsest.block;

import com.exonoxic.palimpsest.block.entity.TearBlockEntity;
import com.exonoxic.palimpsest.registry.ModBlockEntities;
import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A rip in the Overworld's page. The Undertext leaks through it: ink creatures, cold, the
 * sound of scraping. It stays open until sealed with a Seal of Closing.
 */
public class TearBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 16, 11);

    public TearBlock(Properties properties) {
        super(properties);
    }

    public static boolean open(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).canBeReplaced()) return false;
        level.setBlock(pos, ModBlocks.TEAR.get().defaultBlockState(), 3);
        level.playSound(null, pos, ModSounds.TEAR_AMBIENT.get(), SoundSource.HOSTILE, 1.5F, 0.6F);
        level.sendParticles(ModParticles.INK_DRIP.get(), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 40, 0.4, 0.8, 0.4, 0.02);
        return true;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TearBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.TEAR.get(), TearBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            level.addParticle(ModParticles.INK_DRIP.get(), pos.getX() + 0.35 + random.nextDouble() * 0.3, pos.getY() + 0.2 + random.nextDouble() * 1.4,
                    pos.getZ() + 0.35 + random.nextDouble() * 0.3, 0, 0, 0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ModParticles.ASH_FLECK.get(), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.1, 0.03, (random.nextDouble() - 0.5) * 0.1);
        }
        if (random.nextInt(100) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.TEAR_AMBIENT.get(), SoundSource.HOSTILE, 0.8F, 0.8F + random.nextFloat() * 0.3F, false);
        }
    }
}
