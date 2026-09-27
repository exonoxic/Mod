package com.exonoxic.palimpsest.client.sky;

import com.exonoxic.palimpsest.Palimpsest;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Draws the Undertext sky. Also used, faded, over the Overworld sky during a sky-glimpse event.
 */
public final class UndertextSky {
    private static final ResourceLocation INKWELL = Palimpsest.id("textures/environment/inkwell.png");
    private static final ResourceLocation WRITING = Palimpsest.id("textures/environment/sky_writing.png");

    public static void render(ClientLevel level, int ticks, float partial, PoseStack poseStack, Matrix4f projection, float alpha) {
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        // Paper dome: pale overhead, darker and browner toward the horizon.
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f m = poseStack.last().pose();
        float top = 0.74F, topG = 0.70F, topB = 0.61F;
        float hor = 0.36F, horG = 0.32F, horB = 0.27F;
        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(m, 0, 100, 0).color(top, topG, topB, alpha).endVertex();
        for (int i = 0; i <= 16; i++) {
            float a = i * (float) (Math.PI * 2 / 16);
            buf.vertex(m, Mth.cos(a) * 180F, -8F, Mth.sin(a) * 180F).color(hor, horG, horB, alpha).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(m, 0, -100, 0).color(hor * 0.4F, horG * 0.4F, horB * 0.4F, alpha).endVertex();
        for (int i = 16; i >= 0; i--) {
            float a = i * (float) (Math.PI * 2 / 16);
            buf.vertex(m, Mth.cos(a) * 180F, -8F, Mth.sin(a) * 180F).color(hor, horG, horB, alpha).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());

        // Faint lines of writing drifting across the sky.
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, WRITING);
        RenderSystem.setShaderColor(1F, 1F, 1F, 0.22F * alpha);
        float drift = ((ticks + partial) % 24000) / 24000F;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(drift * 360F));
        Matrix4f wm = poseStack.last().pose();
        float s = 150F;
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.vertex(wm, -s, 60F, -s).uv(0F, 0F).endVertex();
        buf.vertex(wm, s, 60F, -s).uv(4F, 0F).endVertex();
        buf.vertex(wm, s, 60F, s).uv(4F, 4F).endVertex();
        buf.vertex(wm, -s, 60F, s).uv(0F, 4F).endVertex();
        BufferUploader.drawWithShader(buf.end());
        poseStack.popPose();

        // The Inkwell: a black sun fixed low in the west, bleeding at the edges.
        RenderSystem.setShaderTexture(0, INKWELL);
        RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-62F));
        Matrix4f sm = poseStack.last().pose();
        float r = 34F;
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.vertex(sm, -r, 100F, -r).uv(0F, 0F).endVertex();
        buf.vertex(sm, r, 100F, -r).uv(1F, 0F).endVertex();
        buf.vertex(sm, r, 100F, r).uv(1F, 1F).endVertex();
        buf.vertex(sm, -r, 100F, r).uv(0F, 1F).endVertex();
        BufferUploader.drawWithShader(buf.end());
        poseStack.popPose();

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
    }

    private UndertextSky() {}
}
