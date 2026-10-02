package com.exonoxic.palimpsest.client.screen;

import com.exonoxic.palimpsest.Palimpsest;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * A single sheet: one of Wray's torn folios (neat, hurried, worse as it goes) or a faded page
 * from the First Draft (most words scraped to noise).
 */
public class LoreScreen extends Screen {
    private static final ResourceLocation SHEET = Palimpsest.id("textures/gui/lore_sheet.png");
    private static final ResourceLocation FADED = Palimpsest.id("textures/gui/faded_sheet.png");
    private static final int W = 192;
    private static final int H = 220;
    private final String kind;
    private final int number;
    private int scroll;

    public LoreScreen(String kind, int number) {
        super(Component.translatable("lore.palimpsest." + kind + "." + number + ".title"));
        this.kind = kind;
        this.number = number;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        int left = (width - W) / 2;
        int top = (height - H) / 2;
        boolean faded = "faded".equals(kind);
        g.blit(faded ? FADED : SHEET, left, top, 0, 0, W, H, 256, 256);
        Component title = Component.translatable("lore.palimpsest." + kind + "." + number + ".title").withStyle(ChatFormatting.ITALIC);
        g.drawString(font, title, left + (W - font.width(title)) / 2, top + 16, faded ? 0x7A6E5E : 0x8C1C13, false);
        List<FormattedCharSequence> lines = font.split(Component.translatable("lore.palimpsest." + kind + "." + number + ".text"), W - 40);
        int maxLines = 17;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, lines.size() - maxLines)));
        for (int i = 0; i < maxLines && i + scroll < lines.size(); i++) {
            g.drawString(font, lines.get(i + scroll), left + 20, top + 34 + i * 10, faded ? 0x5E554A : 0x2A2733, false);
        }
        if (lines.size() > maxLines) {
            g.drawString(font, scroll + maxLines < lines.size() ? "▼" : "▲", left + W - 20, top + H - 18, 0x8C1C13, false);
        }
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, scroll - (int) Math.signum(delta));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
