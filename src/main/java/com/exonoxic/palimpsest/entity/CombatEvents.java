package com.exonoxic.palimpsest.entity;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.bleed.BleedCapability;
import com.exonoxic.palimpsest.bleed.Ending;
import com.exonoxic.palimpsest.item.RasoriumItem;
import com.exonoxic.palimpsest.registry.ModTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Combat rules shared by all inkborn. */
@Mod.EventBusSubscriber(modid = Palimpsest.MODID)
public final class CombatEvents {

    /** Rasoria bite deep into ink; other inkbane weapons cut half again as hard. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (!target.getType().is(ModTags.INKBORN)) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (event.getSource().getDirectEntity() != attacker) return;
        ItemStack weapon = attacker.getMainHandItem();
        if (weapon.getItem() instanceof RasoriumItem rasorium) {
            event.setAmount(event.getAmount() + rasorium.getInkbaneBonus());
        } else if (weapon.is(ModTags.INKBANE)) {
            event.setAmount(event.getAmount() * 1.5F);
        }
    }

    public static boolean isInkbane(LivingEntity attacker) {
        return attacker.getMainHandItem().is(ModTags.INKBANE);
    }

    /** Players who scraped their own name away are no longer on the page for inkborn to find. */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewTarget() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Mob mob) || !mob.getType().is(ModTags.INKBORN)) return;
        if (BleedCapability.get(player).getEnding() == Ending.UNWRITTEN) event.setCanceled(true);
    }

    private CombatEvents() {}
}
