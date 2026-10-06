package com.exonoxic.palimpsest.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A torch that went out on its own. Relight it with flint and steel, a fire charge or another torch. */
public class SpentTorchBlock extends TorchBlock {
    public SpentTorchBlock(Properties properties) {
        super(properties, ParticleTypes.SMOKE);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.62, pos.getZ() + 0.5, 0, 0.01, 0);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!isIgniter(held)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            level.setBlock(pos, Blocks.TORCH.defaultBlockState(), 3);
            consumeIgniter(player, hand, held);
        }
        level.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    static boolean isIgniter(ItemStack stack) {
        return stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE) || stack.is(Items.TORCH);
    }

    static void consumeIgniter(Player player, InteractionHand hand, ItemStack held) {
        if (player.getAbilities().instabuild) return;
        if (held.is(Items.FLINT_AND_STEEL)) held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        else if (held.is(Items.FIRE_CHARGE)) held.shrink(1);
    }
}
