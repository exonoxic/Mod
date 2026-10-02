package com.exonoxic.palimpsest.client.sky;

import com.exonoxic.palimpsest.config.ClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The look of the Undertext: a sky like old paper lit from nowhere, an ink-black sun that never
 * moves, faint lines of writing drifting overhead, dense sepia fog, and a lightmap drained of
 * most of its colour.
 */
public class UndertextEffects extends DimensionSpecialEffects {
    public UndertextEffects() {
        super(Float.NaN, true, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return color.multiply(0.55 + brightness * 0.35, 0.53 + brightness * 0.33, 0.50 + brightness * 0.30);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return true;
    }

    @Override
    public @Nullable float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix,
                             boolean isFoggy, Runnable setupFog) {
        UndertextSky.render(level, ticks, partialTick, poseStack, projectionMatrix, 1.0F);
        return true;
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix) {
        return true;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, net.minecraft.client.renderer.LightTexture lightTexture,
                                     double camX, double camY, double camZ) {
        return true;
    }

    /** Pull colour out of the light and tint what remains toward sepia. */
    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker, float skyLight,
                                     int pixelX, int pixelY, Vector3f colors) {
        float strength = ClientConfig.DARKNESS_INTENSITY.get().floatValue();
        float grey = colors.x() * 0.3F + colors.y() * 0.59F + colors.z() * 0.11F;
        float desat = 0.75F * strength;
        float r = colors.x() + (grey * 1.04F - colors.x()) * desat;
        float g = colors.y() + (grey * 0.97F - colors.y()) * desat;
        float b = colors.z() + (grey * 0.86F - colors.z()) * desat;
        float dim = 1.0F - 0.2F * strength;
        colors.set(r * dim, g * dim, b * dim);
    }
}
