package com.exonoxic.palimpsest.client.render;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.client.ApparitionVisibility;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * Shared renderer for most creatures: one model, one texture, an optional emissive layer
 * ({@code <name>_glow.png}), a scale, and respect for private apparitions.
 */
public class SimpleMobRenderer<T extends Mob, M extends EntityModel<T>> extends MobRenderer<T, M> {
    private final ResourceLocation texture;
    private final String name;
    private final float scale;

    public SimpleMobRenderer(EntityRendererProvider.Context ctx, M model, String name, float shadow, float scale) {
        super(ctx, model, shadow * scale);
        this.name = name;
        this.texture = Palimpsest.id("textures/entity/" + name + ".png");
        this.scale = scale;
    }

    public SimpleMobRenderer<T, M> withGlow() {
        RenderType glow = RenderType.eyes(Palimpsest.id("textures/entity/" + name + "_glow.png"));
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return glow;
            }
        });
        return this;
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        if (scale != 1.0F) poseStack.scale(scale, scale, scale);
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
        return ApparitionVisibility.visible(entity) && super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
