package com.exonoxic.palimpsest.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** What an Erratum leaves behind. Its name never reads the same way twice. */
public class MisprintItem extends Item {
    private static final String GLYPHS = "abcdefghijklmnopqrstuvwxyz#%&?!";

    public MisprintItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        long t = System.currentTimeMillis() / 400L;
        StringBuilder sb = new StringBuilder();
        java.util.Random r = new java.util.Random(t * 31L + stack.getCount());
        int len = 7 + r.nextInt(3);
        for (int i = 0; i < len; i++) sb.append(GLYPHS.charAt(r.nextInt(GLYPHS.length())));
        return Component.literal(sb.toString()).withStyle(ChatFormatting.OBFUSCATED);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tooltips.lore(this, tooltip);
    }
}
