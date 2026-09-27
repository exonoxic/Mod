package com.exonoxic.palimpsest.block.entity;

import com.exonoxic.palimpsest.registry.ModBlockEntities;
import com.exonoxic.palimpsest.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlankBlockEntity extends BlockEntity {
    private BlockState original = Blocks.STONE.defaultBlockState();
    private long restoreAt;

    public BlankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLANK.get(), pos, state);
    }

    public void remember(BlockState state, long when) {
        this.original = state;
        this.restoreAt = when;
        setChanged();
    }

    public BlockState getOriginal() {
        return original;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlankBlockEntity be) {
        if ((level.getGameTime() + pos.hashCode()) % 10 != 0) return;
        if (level.getGameTime() < be.restoreAt) return;
        level.setBlock(pos, be.original, Block.UPDATE_ALL);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ModParticles.GLYPH.get(), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 2, 0.3, 0.1, 0.3, 0.0);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Original", NbtUtils.writeBlockState(original));
        tag.putLong("RestoreAt", restoreAt);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        HolderGetter<Block> blocks = level != null ? level.holderLookup(Registries.BLOCK)
                : net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup();
        original = NbtUtils.readBlockState(blocks, tag.getCompound("Original"));
        restoreAt = tag.getLong("RestoreAt");
    }
}
