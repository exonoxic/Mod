package com.exonoxic.palimpsest.client.render;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.entity.FairCopyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The one thing the Fair Copy gets wrong: over the face it copied, the eyes are black hollows,
 * and something black has run down from them. Drawn as a skin-shaped overlay on the head alone,
 * a hair larger than it so it sits on top of whatever face the copied skin has.
 */
public class FairCopyFaceLayer extends RenderLayer<FairCopyEntity, PlayerModel<FairCopyEntity>> {
    private static final ResourceLocation TEXTURE = Palimpsest.id("textures/entity/fair_copy_face.png");

    public FairCopyFaceLayer(RenderLayerParent<FairCopyEntity, PlayerModel<FairCopyEntity>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, FairCopyEntity entity, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;
        poseStack.pushPose();
        // The head's pivot is the model origin, so this grows the head outward from its neck.
        poseStack.scale(1.02F, 1.02F, 1.02F);
        getParentModel().head.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
