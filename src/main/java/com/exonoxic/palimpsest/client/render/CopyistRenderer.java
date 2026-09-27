package com.exonoxic.palimpsest.client.render;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.client.model.CopyistModel;
import com.exonoxic.palimpsest.client.model.GenericQuadrupedModel;
import com.exonoxic.palimpsest.entity.CopyistEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws a Copyist as whatever it is pretending to be, using the real animal's model and
 * texture — minus a shadow — until it stops pretending.
 */
public class CopyistRenderer extends MobRenderer<CopyistEntity, EntityModel<CopyistEntity>> {
    private static final ResourceLocation TRUE_FORM = Palimpsest.id("textures/entity/copyist.png");
    private static final ResourceLocation COW = new ResourceLocation("textures/entity/cow/cow.png");
    private static final ResourceLocation PIG = new ResourceLocation("textures/entity/pig/pig.png");
    private static final ResourceLocation SHEEP = new ResourceLocation("textures/entity/sheep/sheep.png");
    private static final ResourceLocation SHEEP_FUR = new ResourceLocation("textures/entity/sheep/sheep_fur.png");
    private static final ResourceLocation CHICKEN = new ResourceLocation("textures/entity/chicken.png");

    private final EntityModel<CopyistEntity> trueForm;
    private final EntityModel<CopyistEntity> cow;
    private final EntityModel<CopyistEntity> pig;
    private final EntityModel<CopyistEntity> sheep;
    private final EntityModel<CopyistEntity> sheepFur;
    private final EntityModel<CopyistEntity> chicken;

    public CopyistRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CopyistModel<>(ctx.bakeLayer(CopyistModel.LAYER)), 0.7F);
        this.trueForm = this.model;
        this.cow = new CowModel<>(ctx.bakeLayer(ModelLayers.COW));
        this.pig = new PigModel<>(ctx.bakeLayer(ModelLayers.PIG));
        this.sheep = new GenericQuadrupedModel<>(ctx.bakeLayer(ModelLayers.SHEEP), 8.0F);
        this.sheepFur = new GenericQuadrupedModel<>(ctx.bakeLayer(ModelLayers.SHEEP_FUR), 8.0F);
        this.chicken = new ChickenModel<>(ctx.bakeLayer(ModelLayers.CHICKEN));
        addLayer(new FurLayer(this));
    }

    @Override
    public void render(CopyistEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        this.model = switch (entity.getDisguise()) {
            case CopyistEntity.COW -> cow;
            case CopyistEntity.PIG -> pig;
            case CopyistEntity.SHEEP -> sheep;
            case CopyistEntity.CHICKEN -> chicken;
            default -> trueForm;
        };
        // The tell nobody notices until they do: it casts no shadow.
        this.shadowRadius = entity.isRevealed() ? 0.7F : 0.0F;
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(CopyistEntity entity) {
        return switch (entity.getDisguise()) {
            case CopyistEntity.COW -> COW;
            case CopyistEntity.PIG -> PIG;
            case CopyistEntity.SHEEP -> SHEEP;
            case CopyistEntity.CHICKEN -> CHICKEN;
            default -> TRUE_FORM;
        };
    }

    private class FurLayer extends RenderLayer<CopyistEntity, EntityModel<CopyistEntity>> {
        FurLayer(RenderLayerParent<CopyistEntity, EntityModel<CopyistEntity>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, CopyistEntity entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entity.getDisguise() != CopyistEntity.SHEEP || entity.isInvisible()) return;
            sheepFur.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
            sheepFur.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(SHEEP_FUR));
            sheepFur.renderToBuffer(poseStack, vc, light, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), 0.9F, 0.9F, 0.88F, 1.0F);
        }
    }
}
