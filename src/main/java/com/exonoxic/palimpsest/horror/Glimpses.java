package com.exonoxic.palimpsest.horror;

import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.util.ServerScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-only lies. The server tells one player's client that the world looks different, then
 * tells it the truth again a moment later. Nothing on the server ever changes, and a relog or
 * chunk reload always shows the real world.
 */
public final class Glimpses {
    private static final int MAX_BLOCKS = 900;

    /** Shows the player the Undertext version of their surroundings for {@code ticks}. */
    public static int showOtherPage(ServerPlayer player, int radius, int ticks) {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        List<BlockPos> changed = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -5, -radius), center.offset(radius, 6, radius))) {
            if (changed.size() >= MAX_BLOCKS) break;
            if (p.distSqr(center) > (long) radius * radius) continue;
            BlockState real = level.getBlockState(p);
            BlockState fake = undertextVersion(real);
            if (fake == null || !exposed(level, p)) continue;
            BlockPos imm = p.immutable();
            player.connection.send(new ClientboundBlockUpdatePacket(imm, fake));
            changed.add(imm);
        }
        if (!changed.isEmpty()) {
            ServerScheduler.schedule(ticks, () -> restore(player, changed));
        }
        return changed.size();
    }

    public static void restore(ServerPlayer player, List<BlockPos> positions) {
        if (player.hasDisconnected()) return;
        ServerLevel level = player.serverLevel();
        for (BlockPos p : positions) {
            if (level.hasChunkAt(p)) player.connection.send(new ClientboundBlockUpdatePacket(level, p));
        }
    }

    private static boolean exposed(ServerLevel level, BlockPos p) {
        for (Direction d : Direction.values()) {
            if (!level.getBlockState(p.relative(d)).canOcclude()) return true;
        }
        return false;
    }

    @Nullable
    public static BlockState undertextVersion(BlockState s) {
        if (s.isAir()) return null;
        if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.PODZOL) || s.is(Blocks.MYCELIUM) || s.is(Blocks.MOSS_BLOCK))
            return ModBlocks.RULED_VELLUM.get().defaultBlockState();
        if (s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.ROOTED_DIRT) || s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL))
            return ModBlocks.VELLUM_SOIL.get().defaultBlockState();
        if (s.is(BlockTags.BASE_STONE_OVERWORLD)) return ModBlocks.INKSTONE.get().defaultBlockState();
        if (s.is(Tags.Blocks.COBBLESTONE)) return ModBlocks.COBBLED_INKSTONE.get().defaultBlockState();
        if (s.is(BlockTags.STONE_BRICKS)) return ModBlocks.INKSTONE_BRICKS.get().defaultBlockState();
        if (s.is(BlockTags.LOGS) && s.hasProperty(RotatedPillarBlock.AXIS))
            return ModBlocks.BLOTWOOD_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, s.getValue(RotatedPillarBlock.AXIS));
        if (s.is(BlockTags.LEAVES)) return ModBlocks.BLOTWOOD_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        if (s.is(BlockTags.PLANKS)) return ModBlocks.BLOTWOOD_PLANKS.get().defaultBlockState();
        if (s.is(Blocks.GRASS) || s.is(Blocks.FERN) || s.is(BlockTags.SMALL_FLOWERS)) return ModBlocks.ERASED_GRASS.get().defaultBlockState();
        if (s.is(Blocks.TORCH)) return ModBlocks.SPENT_TORCH.get().defaultBlockState();
        if (s.is(Blocks.WALL_TORCH))
            return ModBlocks.SPENT_WALL_TORCH.get().defaultBlockState().setValue(WallTorchBlock.FACING, s.getValue(WallTorchBlock.FACING));
        return null;
    }

    /** Makes a nearby sign read differently, for this player only, for a little while. */
    public static boolean rewriteSign(ServerPlayer player, int radius, String[] lines, int ticks) {
        ServerLevel level = player.serverLevel();
        List<BlockPos> signs = WorldAlterations.scan(level, player.blockPosition(), radius, 4,
                s -> s.is(BlockTags.SIGNS) || s.is(BlockTags.ALL_HANGING_SIGNS));
        for (BlockPos p : signs) {
            if (!(level.getBlockEntity(p) instanceof SignBlockEntity sign)) continue;
            CompoundTag tag = sign.saveWithoutMetadata();
            CompoundTag front = tag.getCompound("front_text");
            ListTag messages = new ListTag();
            for (int i = 0; i < 4; i++) {
                String line = i < lines.length ? lines[i] : "";
                messages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line))));
            }
            front.put("messages", messages);
            front.remove("filtered_messages");
            tag.put("front_text", front);
            player.connection.send(ClientboundBlockEntityDataPacket.create(sign, be -> tag));
            ServerScheduler.schedule(ticks, () -> {
                if (!player.hasDisconnected() && level.getBlockEntity(p) instanceof SignBlockEntity real) {
                    player.connection.send(ClientboundBlockEntityDataPacket.create(real));
                }
            });
            return true;
        }
        return false;
    }

    private Glimpses() {}
}
