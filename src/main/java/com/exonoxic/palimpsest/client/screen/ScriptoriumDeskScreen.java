package com.exonoxic.palimpsest.client.screen;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.menu.ScriptoriumDeskMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ScriptoriumDeskScreen extends AbstractContainerScreen<ScriptoriumDeskMenu> {
    private static final ResourceLocation TEXTURE = Palimpsest.id("textures/gui/scriptorium_desk.png");

    public ScriptoriumDeskScreen(ScriptoriumDeskMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        int progress = (int) (menu.progress() * 24);
        if (progress > 0) g.blit(TEXTURE, x + 89, y + 35, 176, 0, progress, 17);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }
}
