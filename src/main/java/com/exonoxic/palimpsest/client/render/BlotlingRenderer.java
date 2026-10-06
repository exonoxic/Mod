package com.exonoxic.palimpsest.client.render;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.client.model.BlotlingModel;
import com.exonoxic.palimpsest.entity.BlotlingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Squashes and stretches like a slime, sized by the blot's size. */
public class BlotlingRenderer extends MobRenderer<BlotlingEntity, BlotlingModel<BlotlingEntity>> {
    private static final ResourceLocation TEXTURE = Palimpsest.id("textures/entity/blotling.png");
    private static final RenderType GLOW = RenderType.eyes(Palimpsest.id("textures/entity/blotling_glow.png"));

    public BlotlingRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BlotlingModel<>(ctx.bakeLayer(BlotlingModel.LAYER)), 0.25F);
        // Its eyes catch the light even where there is none.
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public void render(BlotlingEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        shadowRadius = 0.25F * entity.getSize();
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    protected void scale(BlotlingEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.999F, 0.999F, 0.999F);
        poseStack.translate(0.0F, 0.001F, 0.0F);
        float size = entity.getSize();
        float squish = Mth.lerp(partialTick, entity.oSquish, entity.squish) / (size * 0.5F + 1.0F);
        float inv = 1.0F / (squish + 1.0F);
        poseStack.scale(inv * size, 1.0F / inv * size, inv * size);
    }

    @Override
    public ResourceLocation getTextureLocation(BlotlingEntity entity) {
        return TEXTURE;
    }
}
