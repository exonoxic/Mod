package com.exonoxic.palimpsest.horror;

import com.exonoxic.palimpsest.block.SpentTorchBlock;
import com.exonoxic.palimpsest.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Real, persistent (but non-destructive) changes to the world. Everything here can be undone
 * by the player: torches relit, doors closed, chests re-sorted, grass slowly heals.
 */
public final class WorldAlterations {

    public static List<BlockPos> scan(ServerLevel level, BlockPos center, int radius, int vertical, Predicate<BlockState> match) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -vertical, -radius), center.offset(radius, vertical, radius))) {
            if (match.test(level.getBlockState(p))) out.add(p.immutable());
        }
        return out;
    }

    public static boolean isTorch(BlockState s) {
        return s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH);
    }

    /** Snuffs up to {@code max} torches, preferring ones the player is not looking at. */
    public static int snuffTorches(ServerLevel level, ServerPlayer player, int radius, int max, boolean allowInView) {
        List<BlockPos> torches = scan(level, player.blockPosition(), radius, radius / 2 + 2, WorldAlterations::isTorch);
        Collections.shuffle(torches, new java.util.Random(player.getRandom().nextLong()));
        int done = 0;
        for (BlockPos p : torches) {
            if (done >= max) break;
            if (!allowInView && Spots.inView(player, p)) continue;
            if (snuff(level, p)) done++;
        }
        return done;
    }

    public static boolean snuff(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        BlockState spent;
        if (s.is(Blocks.WALL_TORCH)) {
            spent = ModBlocks.SPENT_WALL_TORCH.get().defaultBlockState().setValue(WallTorchBlock.FACING, s.getValue(WallTorchBlock.FACING));
        } else if (s.is(Blocks.TORCH)) {
            spent = ModBlocks.SPENT_TORCH.get().defaultBlockState();
        } else {
            return false;
        }
        level.setBlock(pos, spent, 3);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 6, 0.05, 0.1, 0.05, 0.01);
        level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 0.8F);
        return true;
    }

    @Nullable
    public static BlockPos findDoor(ServerLevel level, BlockPos center, int radius, boolean closedOnly) {
        List<BlockPos> doors = scan(level, center, radius, 3, s -> s.getBlock() instanceof DoorBlock
                && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                && (!closedOnly || !s.getValue(DoorBlock.OPEN)));
        if (doors.isEmpty()) return null;
        doors.sort((a, b) -> Double.compare(a.distSqr(center), b.distSqr(center)));
        return doors.get(0);
    }

    public static boolean openDoor(ServerLevel level, ServerPlayer player, int radius) {
        List<BlockPos> doors = scan(level, player.blockPosition(), radius, 3, s -> s.getBlock() instanceof DoorBlock door
                && door.type().canOpenByHand() && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER && !s.getValue(DoorBlock.OPEN));
        for (BlockPos p : doors) {
            if (Spots.inView(player, p)) continue;
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof DoorBlock door) {
                door.setOpen(null, level, s, p, true);
                return true;
            }
        }
        return false;
    }

    /** Rearranges (never removes) a nearby chest's contents. */
    public static boolean shuffleChest(ServerLevel level, ServerPlayer player, int radius) {
        List<BlockPos> chests = scan(level, player.blockPosition(), radius, 4, s -> s.is(Blocks.CHEST) || s.is(Blocks.BARREL));
        Collections.shuffle(chests, new java.util.Random(player.getRandom().nextLong()));
        for (BlockPos p : chests) {
            BlockEntity be = level.getBlockEntity(p);
            if (!(be instanceof Container container) || container.isEmpty()) continue;
            if (be instanceof ChestBlockEntity && Spots.inView(player, p)) continue;
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < container.getContainerSize(); i++) items.add(container.getItem(i).copy());
            Collections.shuffle(items, new java.util.Random(player.getRandom().nextLong()));
            for (int i = 0; i < items.size(); i++) container.setItem(i, items.get(i));
            container.setChanged();
            return true;
        }
        return false;
    }

    public static int fadeGrass(ServerLevel level, BlockPos center, int radius, int max) {
        List<BlockPos> grass = scan(level, center, radius, 3, s -> s.is(Blocks.GRASS_BLOCK));
        Collections.shuffle(grass, new java.util.Random(level.random.nextLong()));
        int done = 0;
        for (BlockPos p : grass) {
            if (done >= max) break;
            if (p.distSqr(center) > radius * radius) continue;
            level.setBlock(p, ModBlocks.FADED_GRASS_BLOCK.get().defaultBlockState(), 3);
            BlockPos above = p.above();
            BlockState a = level.getBlockState(above);
            if (a.is(Blocks.GRASS) || a.is(Blocks.FERN)) level.setBlock(above, ModBlocks.ERASED_GRASS.get().defaultBlockState(), 3);
            done++;
        }
        return done;
    }

    /** Relights spent torches (used by the Rite of Unmaking). */
    public static int relight(ServerLevel level, BlockPos center, int radius) {
        List<BlockPos> spent = scan(level, center, radius, radius, s -> s.getBlock() instanceof SpentTorchBlock
                || s.is(ModBlocks.SPENT_WALL_TORCH.get()));
        for (BlockPos p : spent) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.SPENT_WALL_TORCH.get())) {
                level.setBlock(p, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, s.getValue(WallTorchBlock.FACING)), 3);
            } else {
                level.setBlock(p, Blocks.TORCH.defaultBlockState(), 3);
            }
        }
        return spent.size();
    }

    private WorldAlterations() {}
}
