package com.exonoxic.palimpsest.item;

import com.exonoxic.palimpsest.entity.CopyistEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The crown of someone who read the Last Folio aloud. You see in the dark and see through
 * disguises; the First Draft sees you just as clearly (Bleed rises faster while worn).
 */
public class CircletItem extends ArmorItem {
    public CircletItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;
        if (player.getItemBySlot(EquipmentSlot.HEAD) != stack) return;
        if (level.getGameTime() % 40 != 0) return;
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
        for (CopyistEntity c : level.getEntitiesOfClass(CopyistEntity.class, player.getBoundingBox().inflate(16))) {
            c.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tooltips.lore(this, tooltip);
    }
}
