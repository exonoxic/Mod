package com.exonoxic.palimpsest.client.render;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.client.model.RasureModel;
import com.exonoxic.palimpsest.entity.boss.RasureEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

public class RasureRenderer extends MobRenderer<RasureEntity, RasureModel<RasureEntity>> {
    private static final ResourceLocation TEXTURE = Palimpsest.id("textures/entity/rasure.png");
    private static final RenderType GLOW = RenderType.eyes(Palimpsest.id("textures/entity/rasure_glow.png"));

    public RasureRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new RasureModel<>(ctx.bakeLayer(RasureModel.LAYER)), 1.0F);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    protected void scale(RasureEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.35F, 1.35F, 1.35F);
    }

    @Override
    protected float getFlipDegrees(RasureEntity entity) {
        return 0F;
    }

    @Override
    public ResourceLocation getTextureLocation(RasureEntity entity) {
        return TEXTURE;
    }
}
