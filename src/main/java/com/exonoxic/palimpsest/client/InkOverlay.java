package com.exonoxic.palimpsest.client;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.config.ClientConfig;
import com.exonoxic.palimpsest.registry.ModEffects;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Ink creeping in from the edges of the screen as Bleed rises; a face in the ink for a few
 * frames when the Director wants one; the white of the Blank Page; the black of Inkblind.
 */
public final class InkOverlay implements IGuiOverlay {
    private static final ResourceLocation VIGNETTE = Palimpsest.id("textures/misc/ink_vignette.png");
    private static final ResourceLocation FACE = Palimpsest.id("textures/misc/vignette_face.png");

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partial, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        float strength = ClientConfig.INK_VIGNETTE.get() ? ClientConfig.VIGNETTE_STRENGTH.get().floatValue() : 0F;
        float ink = ClientBleedState.severity() * 0.75F * strength;
        if (mc.player.hasEffect(ModEffects.INKBLIND.get())) ink = Math.max(ink, 0.95F);
        ink = Math.max(ink, Visions.pageTurn(partial) * 0.8F * Math.max(strength, 0.5F));
        if (ink > 0.01F) {
            g.setColor(1F, 1F, 1F, Math.min(1F, ink));
            g.blit(VIGNETTE, 0, 0, -90, 0F, 0F, width, height, width, height);
        }
        if (Visions.face()) {
            g.setColor(1F, 1F, 1F, 0.55F);
            int size = Math.min(width, height);
            g.blit(FACE, (width - size) / 2, (height - size) / 2, -90, 0F, 0F, size, size, size, size);
        }
        float white = Visions.whiteout(partial);
        if (white > 0.01F) {
            g.fill(0, 0, width, height, ((int) (white * 0.55F * 255) << 24) | 0xF2F0EA);
        }
        if (Visions.flash() > 0F) {
            g.fill(0, 0, width, height, 0x60FFFFFF);
        }
        g.setColor(1F, 1F, 1F, 1F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        CaptionOverlay.render(g, width, height);
    }
}
