package com.exonoxic.palimpsest.client.render;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.client.ApparitionVisibility;
import com.exonoxic.palimpsest.client.model.ErratumLegsModel;
import com.exonoxic.palimpsest.entity.ErratumEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An Erratum is drawn as the block it copies. Only while it moves do the legs show — thin,
 * many-jointed, gone again the instant it stops.
 */
public class ErratumRenderer extends EntityRenderer<ErratumEntity> {
    private static final ResourceLocation LEGS = Palimpsest.id("textures/entity/erratum_legs.png");
    private final ErratumLegsModel<ErratumEntity> legs;

    public ErratumRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.legs = new ErratumLegsModel<>(ctx.bakeLayer(ErratumLegsModel.LAYER));
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(ErratumEntity entity, Frustum frustum, double x, double y, double z) {
        return ApparitionVisibility.visible(entity) && super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(ErratumEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        BlockState state = entity.getMimic();
        boolean moving = entity.isMoving();
        float lift = moving ? 0.3F : 0.0F;
        if (state.getRenderShape() == RenderShape.MODEL) {
            poseStack.pushPose();
            poseStack.translate(-0.5D, lift, -0.5D);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, buffers, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        if (moving) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180F - yaw));
            poseStack.scale(-1F, -1F, 1F);
            poseStack.translate(0F, -1.501F, 0F);
            float age = entity.tickCount + partialTick;
            legs.setupAnim(entity, age * 0.6F, 1.0F, age, 0F, 0F);
            legs.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(LEGS)), light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
            poseStack.popPose();
        }
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ErratumEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
