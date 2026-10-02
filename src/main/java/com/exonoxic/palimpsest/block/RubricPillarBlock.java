package com.exonoxic.palimpsest.block;

import com.exonoxic.palimpsest.entity.boss.RasureEntity;
import com.exonoxic.palimpsest.registry.ModItems;
import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The four pillars of the Last Folio. The Rasure scrapes them blank; re-inking them with
 * vermilion (or rubric chalk) is how its guard is broken.
 */
public class RubricPillarBlock extends Block {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public RubricPillarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(LIT)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        boolean vermilion = held.is(ModItems.VERMILION.get());
        boolean chalk = held.is(ModItems.RUBRIC_CHALK.get());
        if (!vermilion && !chalk) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            level.setBlock(pos, state.setValue(LIT, true), 3);
            if (!player.getAbilities().instabuild) {
                if (vermilion) held.shrink(1);
                else held.hurtAndBreak(4, player, p -> p.broadcastBreakEvent(hand));
            }
            server.sendParticles(ModParticles.RUBRIC_SPARK.get(), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 30, 0.3, 0.6, 0.3, 0.05);
            level.playSound(null, pos, ModSounds.RITUAL_COMPLETE.get(), SoundSource.BLOCKS, 1.0F, 1.4F);
            RasureEntity.onPillarLit(server, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && random.nextInt(4) == 0) {
            level.addParticle(ModParticles.RUBRIC_SPARK.get(), pos.getX() + random.nextDouble(), pos.getY() + 1.02, pos.getZ() + random.nextDouble(), 0, 0.03, 0);
        }
    }
}
