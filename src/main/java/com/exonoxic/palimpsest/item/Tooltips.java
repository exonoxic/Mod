package com.exonoxic.palimpsest.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.List;

final class Tooltips {
    static void lore(Item item, List<Component> tooltip) {
        tooltip.add(Component.translatable(item.getDescriptionId() + ".desc").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    static void use(Item item, List<Component> tooltip) {
        tooltip.add(Component.translatable(item.getDescriptionId() + ".use").withStyle(ChatFormatting.GRAY));
    }

    private Tooltips() {}
}
