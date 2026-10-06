package com.exonoxic.palimpsest.item;

import com.exonoxic.palimpsest.bleed.BleedEvents;
import com.exonoxic.palimpsest.entity.boss.PalimpsestBoss;
import com.exonoxic.palimpsest.registry.ModEffects;
import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A swinging censer of illumine leaf. Its smoke is light made thick: it scalds inkborn, shows
 * the invisible, and clears ink from your own eyes.
 */
public class IllumineCenserItem extends Item {
    public static final double RADIUS = 6.0D;

    public IllumineCenserItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.swing(hand);
        level.playSound(player, player.blockPosition(), ModSounds.CENSER_SWING.get(), SoundSource.PLAYERS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(stack);
        server.sendParticles(ModParticles.ERASURE_MOTE.get(), player.getX(), player.getY() + 1.0, player.getZ(), 80, RADIUS / 2, 0.8, RADIUS / 2, 0.01);
        player.removeEffect(ModEffects.INKBLIND.get());
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS),
                e -> e != player && (e.getType().is(BleedEvents.INKBORN) || e instanceof PalimpsestBoss))) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0, false, false));
            e.hurt(level.damageSources().indirectMagic(player, player), e instanceof PalimpsestBoss ? 4F : 6F);
            if (e instanceof PalimpsestBoss boss) boss.onRevealed(player);
            double dx = e.getX() - player.getX();
            double dz = e.getZ() - player.getZ();
            e.knockback(0.6F, -dx, -dz);
        }
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        player.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tooltips.lore(this, tooltip);
        Tooltips.use(this, tooltip);
    }
}
