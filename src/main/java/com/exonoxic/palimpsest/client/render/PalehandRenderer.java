package com.exonoxic.palimpsest.client.render;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.client.ApparitionVisibility;
import com.exonoxic.palimpsest.client.model.PalehandModel;
import com.exonoxic.palimpsest.entity.PalehandEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws the Palehand enormous and far away. The rise and the sink are done here, from the
 * entity's age, so the server only has to say when the sinking starts.
 */
public class PalehandRenderer extends MobRenderer<PalehandEntity, PalehandModel<PalehandEntity>> {
    private static final ResourceLocation TEXTURE = Palimpsest.id("textures/entity/palehand.png");
    private static final float SCALE = 26.0F; // ~45 blocks from wrist to fingertip

    public PalehandRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PalehandModel<>(ctx.bakeLayer(PalehandModel.LAYER)), 0F);
    }

    @Override
    public boolean shouldRender(PalehandEntity entity, Frustum frustum, double x, double y, double z) {
        return ApparitionVisibility.visible(entity);
    }

    @Override
    protected void scale(PalehandEntity entity, PoseStack poseStack, float partialTick) {
        float age = entity.tickCount + partialTick;
        float rise = Mth.clamp(age / PalehandEntity.RISE, 0F, 1F);
        float sink = entity.getSinkAt() < 0 ? 0F : Mth.clamp((age - entity.getSinkAt()) / PalehandEntity.SINK, 0F, 1F);
        float h = (1F - (1F - rise) * (1F - rise)) - sink * sink;
        poseStack.translate(0.0F, (1F - h) * 50F, 0.0F); // y is flipped here: positive is down
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    protected float getFlipDegrees(PalehandEntity entity) {
        return 0F;
    }

    @Override
    public ResourceLocation getTextureLocation(PalehandEntity entity) {
        return TEXTURE;
    }
}
