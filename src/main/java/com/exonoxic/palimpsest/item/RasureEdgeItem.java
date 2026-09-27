package com.exonoxic.palimpsest.item;

import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.entity.boss.PalimpsestBoss;
import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Forged from the Rasure's own shards. Its right-click erases an ordinary creature from the
 * page outright — no body, no drops, as if it had never been written. Bosses only bleed.
 */
public class RasureEdgeItem extends SwordItem {
    public static final int COOLDOWN = 1200;

    public RasureEdgeItem(Tier tier, int damage, float speed, Properties properties) {
        super(tier, damage, speed, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getLookAngle().scale(12));
        AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(12)).inflate(1);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, reach, box,
                e -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator());
        if (hit == null) return InteractionResultHolder.pass(stack);
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);

        Entity target = hit.getEntity();
        server.sendParticles(ModParticles.ERASURE_MOTE.get(), target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 60,
                target.getBbWidth() / 2, target.getBbHeight() / 2, target.getBbWidth() / 2, 0.02);
        level.playSound(null, target.blockPosition(), ModSounds.BLANK_ERASE.get(), SoundSource.PLAYERS, 1.2F, 0.8F);
        if (target instanceof PalimpsestBoss || !(target instanceof Mob mob) || target instanceof Player) {
            target.hurt(level.damageSources().playerAttack(player), 20F);
        } else {
            mob.discard();
        }
        BleedManager.add(sp, 5F);
        stack.hurtAndBreak(25, player, p -> p.broadcastBreakEvent(hand));
        player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tooltips.lore(this, tooltip);
        Tooltips.use(this, tooltip);
    }
}
