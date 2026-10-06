package com.exonoxic.palimpsest.item;

import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModItems;
import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.util.Advancements;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The scribe's knife — a rasorium — used to scrape old ink off vellum so it can be written on
 * again. Here it scrapes the new world off to show the old one underneath. Also a quick blade
 * that bites deep into anything made of ink.
 */
public class RasoriumItem extends SwordItem {
    private final float inkbaneBonus;

    public RasoriumItem(Tier tier, int damage, float speed, float inkbaneBonus, Properties properties) {
        super(tier, damage, speed, properties);
        this.inkbaneBonus = inkbaneBonus;
    }

    public float getInkbaneBonus() {
        return inkbaneBonus;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();

        boolean stone = state.is(ModBlocks.PALIMPSEST_STONE.get()) || state.is(ModBlocks.DEEPSLATE_PALIMPSEST_STONE.get());
        boolean ruled = state.is(ModBlocks.RULED_VELLUM.get());
        boolean soil = state.is(ModBlocks.VELLUM_SOIL.get());
        boolean sign = level.getBlockEntity(pos) instanceof SignBlockEntity;
        if (!stone && !ruled && !soil && !sign) return InteractionResult.PASS;

        level.playSound(player, pos, ModSounds.RASORIUM_SCRAPE.get(), SoundSource.BLOCKS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ServerLevel server = (ServerLevel) level;

        if (stone) {
            level.setBlock(pos, ModBlocks.SCRAPED_STONE.get().defaultBlockState(), Block.UPDATE_ALL);
            Block.popResourceFromFace(level, pos, context.getClickedFace(), new ItemStack(ModItems.UNDERTEXT_FRAGMENT.get()));
            server.sendParticles(ModParticles.GLYPH.get(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.4, 0.4, 0.4, 0.02);
            if (player instanceof ServerPlayer sp) {
                BleedManager.add(sp, 3F);
                BleedManager.unlock(sp, "palimpsest_stone");
                Advancements.grant(sp, "under_the_surface");
            }
            damage(context, 2);
        } else if (ruled || soil) {
            if (ruled) level.setBlock(pos, ModBlocks.VELLUM_SOIL.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.random.nextFloat() < 0.5F) {
                Block.popResourceFromFace(level, pos, context.getClickedFace(), new ItemStack(ModItems.VELLUM_SCRAP.get()));
            }
            damage(context, 1);
        } else if (level.getBlockEntity(pos) instanceof SignBlockEntity signBe) {
            // What the sign said before anyone wrote on it.
            int line = Math.floorMod(pos.hashCode() * 31 + level.dimension().location().hashCode(), 10);
            signBe.setText(new SignText(), true);
            signBe.setText(new SignText(), false);
            signBe.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.palimpsest.sign_undertext",
                        Component.translatable("message.palimpsest.sign_undertext." + line)).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
            }
            damage(context, 1);
        }
        return InteractionResult.CONSUME;
    }

    private static void damage(UseOnContext context, int amount) {
        Player player = context.getPlayer();
        if (player != null) context.getItemInHand().hurtAndBreak(amount, player, p -> p.broadcastBreakEvent(context.getHand()));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tooltips.lore(this, tooltip);
        Tooltips.use(this, tooltip);
    }
}
