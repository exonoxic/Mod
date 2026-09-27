package com.exonoxic.palimpsest.item;

import com.exonoxic.palimpsest.bleed.BleedEvents;
import com.exonoxic.palimpsest.entity.CopyistEntity;
import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A gold-rimmed lens ground with a sliver of undertext in the glass. Focus it to make anything
 * made of ink stand out — including things pretending not to be.
 */
public class ReadingLensItem extends Item {
    public ReadingLensItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(player, player.blockPosition(), ModSounds.LENS_FOCUS.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(stack);

        int found = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(24),
                e -> e instanceof Mob && (e.getType().is(BleedEvents.INKBORN) || e instanceof CopyistEntity))) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
            if (e instanceof CopyistEntity copyist) copyist.onLensed(player);
            found++;
        }
        BlockHitResult look = level.clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(8)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (look.getType() == HitResult.Type.BLOCK) {
            BlockPos p = look.getBlockPos();
            BlockState s = level.getBlockState(p);
            String key = null;
            if (s.is(ModBlocks.PALIMPSEST_STONE.get()) || s.is(ModBlocks.DEEPSLATE_PALIMPSEST_STONE.get())) key = "message.palimpsest.lens.stone";
            else if (s.is(ModBlocks.SCRAPED_STONE.get())) key = "message.palimpsest.lens.scraped";
            else if (s.is(ModBlocks.TEAR.get())) key = "message.palimpsest.lens.tear";
            else if (s.is(ModBlocks.BLANK.get())) key = "message.palimpsest.lens.blank";
            if (key != null) {
                player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                server.sendParticles(ModParticles.GLYPH.get(), p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.01);
            }
        }
        if (found == 0 && look.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(Component.translatable("message.palimpsest.lens.nothing").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
        }
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        player.getCooldowns().addCooldown(this, 60);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tooltips.lore(this, tooltip);
        Tooltips.use(this, tooltip);
    }
}
