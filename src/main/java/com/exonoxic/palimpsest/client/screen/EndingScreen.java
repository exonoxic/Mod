package com.exonoxic.palimpsest.client.screen;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.network.EndingChoicePacket;
import com.exonoxic.palimpsest.network.PacketHandler;
import com.exonoxic.palimpsest.registry.ModItems;
import com.exonoxic.palimpsest.world.EndingHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** The Last Folio, open on the stand. Two choices, sometimes three, and no way to take it back. */
public class EndingScreen extends Screen {
    private static final ResourceLocation SHEET = Palimpsest.id("textures/gui/lore_sheet.png");
    private static final int W = 192;
    private static final int H = 220;
    private final BlockPos pos;
    private int pending = -1;
    private Button confirm;

    public EndingScreen(BlockPos pos) {
        super(Component.translatable("screen.palimpsest.ending.title"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        int left = (width - W) / 2;
        int top = (height - H) / 2;
        boolean canScrape = Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.getInventory().contains(new ItemStack(ModItems.RASURE_EDGE.get()));
        int y = top + H - 88;
        addRenderableWidget(choice(EndingHandler.SEAL, "seal", left + 16, y));
        addRenderableWidget(choice(EndingHandler.READ, "read", left + 16, y + 22));
        if (canScrape) addRenderableWidget(choice(EndingHandler.SCRAPE, "scrape", left + 16, y + 44));
        confirm = addRenderableWidget(Button.builder(Component.translatable("screen.palimpsest.ending.confirm").withStyle(ChatFormatting.DARK_RED),
                b -> {
                    if (pending >= 0) PacketHandler.sendToServer(new EndingChoicePacket(pending, pos));
                    onClose();
                }).bounds(left + 16, top + H - 20, W - 32, 16).build());
        confirm.active = false;
    }

    private Button choice(int id, String key, int x, int y) {
        return Button.builder(Component.translatable("screen.palimpsest.ending." + key), b -> {
                    pending = id;
                    confirm.active = true;
                }).bounds(x, y, W - 32, 18)
                .tooltip(Tooltip.create(Component.translatable("screen.palimpsest.ending." + key + ".desc")))
                .build();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        int left = (width - W) / 2;
        int top = (height - H) / 2;
        g.blit(SHEET, left, top, 0, 0, W, H, 256, 256);
        Component title = getTitle().copy().withStyle(ChatFormatting.ITALIC);
        g.drawString(font, title, left + (W - font.width(title)) / 2, top + 14, 0x8C1C13, false);
        String name = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getGameProfile().getName() : "";
        List<FormattedCharSequence> lines = font.split(Component.translatable("screen.palimpsest.ending.text", name), W - 36);
        int y = top + 30;
        for (FormattedCharSequence l : lines) {
            g.drawString(font, l, left + 18, y, 0x2A2733, false);
            y += 10;
        }
        if (pending >= 0) {
            String key = pending == EndingHandler.SEAL ? "seal" : pending == EndingHandler.READ ? "read" : "scrape";
            Component chosen = Component.translatable("screen.palimpsest.ending.chosen", Component.translatable("screen.palimpsest.ending." + key));
            g.drawString(font, chosen, left + (W - font.width(chosen)) / 2, top + H - 34, 0x8C1C13, false);
        }
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
