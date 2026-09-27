package com.exonoxic.palimpsest.world.gate;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.block.UndertextVeilBlock;
import com.exonoxic.palimpsest.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A Folio Gate is a doorway: a two-wide, three-tall opening framed in rubricated vellum
 * (corners optional). Reading a Folio of Descent aloud at the frame fills it with the veil.
 */
public final class FolioGate {
    public static final TagKey<Block> FRAME = TagKey.create(Registries.BLOCK, Palimpsest.id("gate_frame"));
    public static final int WIDTH = 2;
    public static final int HEIGHT = 3;

    /** The bottom-left interior block and the direction the gate is wide in. */
    public record Shape(BlockPos origin, Direction widthDir) {
        public Direction.Axis axis() {
            return widthDir.getAxis();
        }

        public Iterable<BlockPos> interior() {
            java.util.List<BlockPos> list = new java.util.ArrayList<>();
            for (int w = 0; w < WIDTH; w++) for (int h = 0; h < HEIGHT; h++) list.add(origin.relative(widthDir, w).above(h));
            return list;
        }

        /** Where to stand in front of this gate, facing into it. */
        public BlockPos front(boolean positiveSide) {
            Direction normal = widthDir.getClockWise();
            return origin.relative(positiveSide ? normal : normal.getOpposite(), 1);
        }
    }

    public static boolean isFrame(BlockState state) {
        return state.is(FRAME);
    }

    /** Looks for a complete, empty frame that includes (or touches) the clicked block. */
    @Nullable
    public static Shape find(Level level, BlockPos clicked) {
        for (Direction widthDir : new Direction[]{Direction.EAST, Direction.SOUTH}) {
            for (int dw = -WIDTH - 1; dw <= 1; dw++) {
                for (int dh = -HEIGHT - 1; dh <= 1; dh++) {
                    BlockPos origin = clicked.relative(widthDir, dw).above(dh);
                    if (valid(level, origin, widthDir, false)) return new Shape(origin, widthDir);
                }
            }
        }
        return null;
    }

    public static boolean valid(Level level, BlockPos origin, Direction widthDir, boolean allowVeil) {
        for (int w = 0; w < WIDTH; w++) {
            for (int h = 0; h < HEIGHT; h++) {
                BlockState s = level.getBlockState(origin.relative(widthDir, w).above(h));
                boolean ok = s.isAir() || (allowVeil && s.is(ModBlocks.UNDERTEXT_VEIL.get()));
                if (!ok) return false;
            }
            if (!isFrame(level.getBlockState(origin.relative(widthDir, w).below()))) return false;
            if (!isFrame(level.getBlockState(origin.relative(widthDir, w).above(HEIGHT)))) return false;
        }
        for (int h = 0; h < HEIGHT; h++) {
            if (!isFrame(level.getBlockState(origin.relative(widthDir, -1).above(h)))) return false;
            if (!isFrame(level.getBlockState(origin.relative(widthDir, WIDTH).above(h)))) return false;
        }
        return true;
    }

    public static void fill(Level level, Shape shape) {
        BlockState veil = ModBlocks.UNDERTEXT_VEIL.get().defaultBlockState().setValue(UndertextVeilBlock.AXIS, shape.axis());
        for (BlockPos p : shape.interior()) level.setBlock(p, veil, Block.UPDATE_CLIENTS);
    }

    /** From any veil block, walk down and back to the gate's origin. */
    public static Shape fromVeil(Level level, BlockPos veilPos) {
        BlockState state = level.getBlockState(veilPos);
        Direction.Axis axis = state.hasProperty(UndertextVeilBlock.AXIS) ? state.getValue(UndertextVeilBlock.AXIS) : Direction.Axis.X;
        Direction widthDir = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        BlockPos p = veilPos;
        for (int i = 0; i < 4 && level.getBlockState(p.below()).is(ModBlocks.UNDERTEXT_VEIL.get()); i++) p = p.below();
        for (int i = 0; i < 3 && level.getBlockState(p.relative(widthDir.getOpposite())).is(ModBlocks.UNDERTEXT_VEIL.get()); i++) {
            p = p.relative(widthDir.getOpposite());
        }
        return new Shape(p, widthDir);
    }

    /** Builds a complete gate (frame, veil, a small landing) around {@code origin}. */
    public static Shape build(Level level, BlockPos origin, Direction widthDir) {
        BlockState frame = ModBlocks.RUBRICATED_VELLUM_BRICKS.get().defaultBlockState();
        BlockState floor = ModBlocks.INKSTONE_BRICKS.get().defaultBlockState();
        Direction normal = widthDir.getClockWise();
        // Clear a space and lay a landing on both sides.
        for (int w = -2; w <= WIDTH + 1; w++) {
            for (int d = -2; d <= 2; d++) {
                BlockPos base = origin.relative(widthDir, w).relative(normal, d);
                level.setBlock(base.below(), floor, Block.UPDATE_ALL);
                for (int h = 0; h <= HEIGHT + 1; h++) level.setBlock(base.above(h), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        for (int w = -1; w <= WIDTH; w++) {
            level.setBlock(origin.relative(widthDir, w).below(), frame, Block.UPDATE_ALL);
            level.setBlock(origin.relative(widthDir, w).above(HEIGHT), frame, Block.UPDATE_ALL);
        }
        for (int h = 0; h < HEIGHT; h++) {
            level.setBlock(origin.relative(widthDir, -1).above(h), frame, Block.UPDATE_ALL);
            level.setBlock(origin.relative(widthDir, WIDTH).above(h), frame, Block.UPDATE_ALL);
        }
        Shape shape = new Shape(origin, widthDir);
        fill(level, shape);
        return shape;
    }

    private FolioGate() {}
}
