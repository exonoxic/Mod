package com.exonoxic.palimpsest.client.dev;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.io.InputStream;
import java.util.List;

/** A trailer title card: lines of text (or the logo) fading in and out of black. Development only. */
final class TitleCardScreen extends Screen {
    private static final int FADE = 10;
    private static ResourceLocation logo;
    private static int logoW, logoH;

    private final List<String> lines;
    private final int scale;
    private final boolean showLogo;
    private final int length;
    private int age;

    TitleCardScreen(List<String> lines, int scale, boolean showLogo, int length) {
        super(Component.empty());
        this.lines = lines;
        this.scale = scale;
        this.showLogo = showLogo;
        this.length = length;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void tick() {
        age++;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, 0xFF000000);
        float t = age + partial;
        float alpha = Mth.clamp(Math.min(t / FADE, (length - t) / FADE), 0.0F, 1.0F);
        if (alpha <= 0.02F) return;
        int y = height / 2;
        if (showLogo && logo() != null) {
            int w = Math.min(width * 3 / 5, logoW * 2);
            int h = w * logoH / logoW;
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            g.blit(logo, (width - w) / 2, y - h / 2 - 10, w, h, 0.0F, 0.0F, logoW, logoH, logoW, logoH);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            y += h / 2 + 4;
        } else {
            y -= lines.size() * (font.lineHeight + 3) * scale / 2;
        }
        int color = ((int) (alpha * 255) << 24) | 0xD9CFBF;
        for (int i = 0; i < lines.size(); i++) {
            int s = showLogo ? 1 : (i == 0 ? scale : Math.max(1, scale - 1));
            g.pose().pushPose();
            g.pose().translate(width / 2.0F, y, 0.0F);
            g.pose().scale(s, s, 1.0F);
            String line = lines.get(i);
            g.drawString(font, line, -font.width(line) / 2, 0, color, false);
            g.pose().popPose();
            y += (font.lineHeight + 3) * s;
        }
    }

    private static ResourceLocation logo() {
        if (logo == null) {
            try (InputStream in = TitleCardScreen.class.getResourceAsStream("/palimpsest_logo.png")) {
                if (in == null) return null;
                NativeImage image = NativeImage.read(in);
                logoW = image.getWidth();
                logoH = image.getHeight();
                logo = Minecraft.getInstance().getTextureManager().register("palimpsest_trailer_logo", new DynamicTexture(image));
            } catch (Exception e) {
                return null;
            }
        }
        return logo;
    }
}
