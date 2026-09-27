package com.exonoxic.palimpsest.client.screen;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.client.ClientBleedState;
import com.exonoxic.palimpsest.codex.CodexEntries;
import com.exonoxic.palimpsest.codex.CodexEntry;
import com.exonoxic.palimpsest.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * The Commonplace Book. Six ribbons down the left edge pick a section; the left page lists what
 * has been noticed (and how much has not); the right page shows the chosen entry. Nothing is
 * written here until the player has actually found it.
 */
public class CodexScreen extends Screen {
    private static final ResourceLocation BOOK = Palimpsest.id("textures/gui/codex.png");
    private static final int W = 256;
    private static final int H = 180;
    private static final int LIST_ROWS = 12;
    private static final int ROW_H = 12;

    private int left;
    private int top;
    private CodexEntry.Category category = CodexEntry.Category.THE_PAGE;
    private CodexEntry selected;
    private int listScroll;
    private int textScroll;

    public CodexScreen() {
        super(Component.translatable("item.palimpsest.commonplace_book"));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        selectFirst();
    }

    private List<CodexEntry> entries() {
        return CodexEntries.inCategory(category);
    }

    private void selectFirst() {
        selected = null;
        listScroll = 0;
        textScroll = 0;
        for (CodexEntry e : entries()) {
            if (ClientBleedState.knows(e.id())) {
                selected = e;
                break;
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        g.blit(BOOK, left, top, 0, 0, W, H, 256, 256);

        CodexEntry.Category[] cats = CodexEntry.Category.values();
        for (int i = 0; i < cats.length; i++) {
            int tx = left - 22;
            int ty = top + 10 + i * 26;
            boolean active = cats[i] == category;
            g.blit(BOOK, tx, ty, active ? 24 : 0, 184, 24, 24, 256, 256);
            g.renderItem(iconFor(cats[i]), tx + 5, ty + 4);
            if (mouseX >= tx && mouseX < tx + 24 && mouseY >= ty && mouseY < ty + 24) {
                g.renderTooltip(font, Component.translatable(cats[i].titleKey()), mouseX, mouseY);
            }
        }

        g.drawString(font, Component.translatable(category.titleKey()).withStyle(ChatFormatting.DARK_RED), left + 18, top + 12, 0x8C1C13, false);
        if (category == CodexEntry.Category.THE_PAGE) {
            List<FormattedCharSequence> lines = font.split(ClientBleedState.stage().describe().copy().withStyle(ChatFormatting.ITALIC), 104);
            int y = top + 26;
            for (FormattedCharSequence l : lines) {
                g.drawString(font, l, left + 18, y, 0x3B2F2A, false);
                y += 10;
            }
        } else {
            List<CodexEntry> list = entries();
            int known = 0;
            for (CodexEntry e : list) if (ClientBleedState.knows(e.id())) known++;
            g.drawString(font, known + " / " + list.size(), left + 104, top + 12, 0x6B5A48, false);
            for (int row = 0; row < LIST_ROWS; row++) {
                int idx = row + listScroll;
                if (idx >= list.size()) break;
                CodexEntry e = list.get(idx);
                int y = top + 28 + row * ROW_H;
                boolean knows = ClientBleedState.knows(e.id());
                Component label = knows ? Component.translatable(e.titleKey()) : Component.literal("~ ~ ~ ~ ~");
                int color = knows ? (e == selected ? 0x8C1C13 : 0x2A2733) : 0xA89F8C;
                g.drawString(font, font.plainSubstrByWidth(label.getString(), 108), left + 18, y, color, false);
            }
        }

        if (selected != null) {
            g.renderItem(iconFor(selected), left + 136, top + 10);
            g.drawString(font, Component.translatable(selected.titleKey()).withStyle(ChatFormatting.BOLD), left + 156, top + 14, 0x8C1C13, false);
            List<FormattedCharSequence> text = font.split(Component.translatable(selected.textKey()), 104);
            int maxLines = 13;
            textScroll = Math.max(0, Math.min(textScroll, Math.max(0, text.size() - maxLines)));
            for (int i = 0; i < maxLines && i + textScroll < text.size(); i++) {
                g.drawString(font, text.get(i + textScroll), left + 138, top + 32 + i * 10, 0x2A2733, false);
            }
            if (text.size() > maxLines) {
                g.drawString(font, (textScroll + maxLines < text.size()) ? "▼" : "▲", left + 236, top + 160, 0x8C1C13, false);
            }
        } else if (category != CodexEntry.Category.THE_PAGE) {
            g.drawString(font, Component.translatable("codex.palimpsest.empty").withStyle(ChatFormatting.ITALIC), left + 138, top + 32, 0x6B5A48, false);
        }
        super.render(g, mouseX, mouseY, partial);
    }

    private ItemStack iconFor(CodexEntry.Category c) {
        return switch (c) {
            case THE_PAGE -> stack("palimpsest:blank_vellum");
            case OBSERVATIONS -> stack("palimpsest:reading_lens");
            case CREATURES -> stack("palimpsest:black_quill");
            case PLACES -> stack("palimpsest:margin_compass");
            case CRAFT -> stack("palimpsest:rubric_altar");
            case FOLIOS -> stack("palimpsest:torn_folio");
        };
    }

    private ItemStack iconFor(CodexEntry e) {
        return stack(e.icon());
    }

    private static ItemStack stack(String id) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(id));
        return new ItemStack(item == null ? Items.PAPER : item);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        CodexEntry.Category[] cats = CodexEntry.Category.values();
        for (int i = 0; i < cats.length; i++) {
            int tx = left - 22;
            int ty = top + 10 + i * 26;
            if (mouseX >= tx && mouseX < tx + 24 && mouseY >= ty && mouseY < ty + 24) {
                category = cats[i];
                selectFirst();
                click();
                return true;
            }
        }
        if (category != CodexEntry.Category.THE_PAGE && mouseX >= left + 16 && mouseX < left + 128) {
            int row = (int) ((mouseY - (top + 28)) / ROW_H);
            List<CodexEntry> list = entries();
            int idx = row + listScroll;
            if (row >= 0 && row < LIST_ROWS && idx < list.size() && ClientBleedState.knows(list.get(idx).id())) {
                selected = list.get(idx);
                textScroll = 0;
                click();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < left + 128) {
            int max = Math.max(0, entries().size() - LIST_ROWS);
            listScroll = Math.max(0, Math.min(max, listScroll - (int) Math.signum(delta)));
        } else {
            textScroll = Math.max(0, textScroll - (int) Math.signum(delta));
        }
        return true;
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.PAGE_TURN.get(), 1.0F, 0.6F));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
