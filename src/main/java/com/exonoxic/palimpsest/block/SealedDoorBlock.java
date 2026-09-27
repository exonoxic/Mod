package com.exonoxic.palimpsest.block;

import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModItems;
import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Leather and thread stretched across a doorway. Only the Spine Key unpicks the stitching. */
public class SealedDoorBlock extends Block {
    private static final int MAX_BLOCKS = 96;

    public SealedDoorBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).is(ModItems.SPINE_KEY.get())) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.palimpsest.sealed_door").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level instanceof ServerLevel server) unpick(server, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void unpick(ServerLevel level, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty() && seen.size() < MAX_BLOCKS) {
            BlockPos p = queue.poll();
            if (!seen.add(p)) continue;
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (!seen.contains(n) && level.getBlockState(n).is(ModBlocks.SEALED_DOOR.get())) queue.add(n);
            }
        }
        for (BlockPos p : seen) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            level.sendParticles(ModParticles.RUBRIC_SPARK.get(), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
        }
        level.playSound(null, start, ModSounds.SEALED_DOOR_OPEN.get(), SoundSource.BLOCKS, 1.2F, 0.9F);
    }
}
