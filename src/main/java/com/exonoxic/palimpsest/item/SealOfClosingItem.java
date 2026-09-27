package com.exonoxic.palimpsest.item;

import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.block.entity.BlankBlockEntity;
import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.util.Advancements;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Red wax pressed over a rip in the page. Closes a Tear, or writes a blank back at once. */
public class SealOfClosingItem extends Item {
    public SealOfClosingItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        boolean tear = state.is(ModBlocks.TEAR.get());
        boolean blank = state.is(ModBlocks.BLANK.get());
        if (!tear && !blank) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (tear) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            if (context.getPlayer() instanceof ServerPlayer sp) {
                BleedManager.add(sp, -10F);
                BleedManager.unlock(sp, "tears");
                BleedManager.unlock(sp, "seals");
                Advancements.grant(sp, "a_tear_in_the_page");
            }
        } else if (level.getBlockEntity(pos) instanceof BlankBlockEntity be) {
            level.setBlock(pos, be.getOriginal(), 3);
        }
        server.sendParticles(ModParticles.RUBRIC_SPARK.get(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 25, 0.3, 0.5, 0.3, 0.05);
        level.playSound(null, pos, ModSounds.TEAR_SEAL.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tooltips.lore(this, tooltip);
        Tooltips.use(this, tooltip);
    }
}
