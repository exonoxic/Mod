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
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Copyist wears a vanilla animal's model while it pretends, and its own model (in the skin
 * of that animal) once revealed.
 * <p>
 * The pretence is almost right: it casts no shadow, it stands a little too tall and a little
 * too narrow, and its head tilts slowly like something curious, then snaps. When it gives up,
 * the animal flickers in and out for a moment and the true form unfolds out of it.
 */
public class CopyistRenderer extends MobRenderer<CopyistEntity, EntityModel<CopyistEntity>> {
    private static final ResourceLocation[] TRUE_FORM = {
            Palimpsest.id("textures/entity/copyist.png"),
            Palimpsest.id("textures/entity/copyist_pig.png"),
            Palimpsest.id("textures/entity/copyist_sheep.png"),
            Palimpsest.id("textures/entity/copyist_chicken.png")};
    private static final RenderType TRUE_EYES = RenderType.eyes(Palimpsest.id("textures/entity/copyist_glow.png"));
    private static final ResourceLocation COW = new ResourceLocation("textures/entity/cow/cow.png");
    private static final ResourceLocation PIG = new ResourceLocation("textures/entity/pig/pig.png");
    private static final ResourceLocation SHEEP = new ResourceLocation("textures/entity/sheep/sheep.png");
    private static final ResourceLocation SHEEP_FUR = new ResourceLocation("textures/entity/sheep/sheep_fur.png");
    private static final ResourceLocation CHICKEN = new ResourceLocation("textures/entity/chicken.png");
    /** Share of the reveal spent flickering between the animal and the true form. */
    private static final float FLICKER = 0.35F;

    private final EntityModel<CopyistEntity> trueForm;
    private final EntityModel<CopyistEntity> cow;
    private final EntityModel<CopyistEntity> pig;
    private final EntityModel<CopyistEntity> sheep;
    private final EntityModel<CopyistEntity> sheepFur;
    private final EntityModel<CopyistEntity> chicken;
    /** Which look is being drawn this frame (a disguise constant, or REVEALED). */
    private int showing = CopyistEntity.REVEALED;

    public CopyistRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CopyistModel<>(ctx.bakeLayer(CopyistModel.LAYER)), 0.7F);
        this.trueForm = this.model;

        ModelPart cowRoot = ctx.bakeLayer(ModelLayers.COW);
        ModelPart cowHead = cowRoot.getChild("head");
        this.cow = new CowModel<>(cowRoot) {
            @Override
            public void setupAnim(CopyistEntity e, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
                super.setupAnim(e, limbSwing, limbSwingAmount, age, yaw, pitch);
                tell(e, age, cowHead);
            }
        };
        ModelPart pigRoot = ctx.bakeLayer(ModelLayers.PIG);
        ModelPart pigHead = pigRoot.getChild("head");
        this.pig = new PigModel<>(pigRoot) {
            @Override
            public void setupAnim(CopyistEntity e, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
                super.setupAnim(e, limbSwing, limbSwingAmount, age, yaw, pitch);
                tell(e, age, pigHead);
            }
        };
        ModelPart sheepRoot = ctx.bakeLayer(ModelLayers.SHEEP);
        ModelPart sheepHead = sheepRoot.getChild("head");
        this.sheep = new GenericQuadrupedModel<>(sheepRoot, 8.0F) {
            @Override
            public void setupAnim(CopyistEntity e, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
                super.setupAnim(e, limbSwing, limbSwingAmount, age, yaw, pitch);
                tell(e, age, sheepHead);
            }
        };
        ModelPart furRoot = ctx.bakeLayer(ModelLayers.SHEEP_FUR);
        ModelPart furHead = furRoot.getChild("head");
        this.sheepFur = new GenericQuadrupedModel<>(furRoot, 8.0F) {
            @Override
            public void setupAnim(CopyistEntity e, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
                super.setupAnim(e, limbSwing, limbSwingAmount, age, yaw, pitch);
                tell(e, age, furHead);
            }
        };
        ModelPart chickenRoot = ctx.bakeLayer(ModelLayers.CHICKEN);
        ModelPart chickenHead = chickenRoot.getChild("head");
        ModelPart chickenBeak = chickenRoot.getChild("beak");
        ModelPart chickenWattle = chickenRoot.getChild("red_thing");
        this.chicken = new ChickenModel<>(chickenRoot) {
            @Override
            public void setupAnim(CopyistEntity e, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
                super.setupAnim(e, limbSwing, limbSwingAmount, age, yaw, pitch);
                tell(e, age, chickenHead, chickenBeak, chickenWattle);
            }
        };
        addLayer(new FurLayer(this));
        addLayer(new TrueEyesLayer(this));
    }

    /** The animal's head does something no animal's head does: tilts slowly, then snaps. */
    private static void tell(CopyistEntity e, float age, ModelPart... head) {
        float phase = e.getId() * 1.7F;
        float tilt = Mth.sin(age * 0.045F + phase) * 0.3F;
        float snap = Mth.sin(age * 0.17F + phase) > 0.97F ? 0.55F : 0.0F;
        for (ModelPart p : head) {
            p.zRot += tilt;
            p.yRot += snap;
        }
    }

    @Override
    public void render(CopyistEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        showing = entity.getDisguise();
        if (entity.isRevealed()) {
            float t = entity.revealProgress(partialTick);
            // For the first moments of a reveal the animal flickers back in, two ticks at a time.
            if (t < FLICKER && (entity.tickCount / 2) % 2 == 0) showing = entity.getHide();
        }
        this.model = switch (showing) {
            case CopyistEntity.COW -> cow;
            case CopyistEntity.PIG -> pig;
            case CopyistEntity.SHEEP -> sheep;
            case CopyistEntity.CHICKEN -> chicken;
            default -> trueForm;
        };
        // The tell nobody notices until they do: while pretending it casts no shadow.
        this.shadowRadius = entity.isRevealed() ? 0.7F : 0.0F;
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    protected void scale(CopyistEntity entity, PoseStack poseStack, float partialTick) {
        if (!entity.isRevealed()) {
            // Almost the right shape.
            poseStack.scale(0.96F, 1.07F, 0.96F);
            return;
        }
        float t = entity.revealProgress(partialTick);
        if (t >= 1.0F || showing != CopyistEntity.REVEALED) return;
        // The true form unfolds out of the animal: squashed low and wide, then stretching up
        // past its full height and settling.
        float k = Math.max(0.0F, (t - FLICKER * 0.5F) / (1.0F - FLICKER * 0.5F));
        float back = 1.0F + 2.2F * (float) Math.pow(k - 1.0F, 3) + 1.2F * (float) Math.pow(k - 1.0F, 2);
        float height = Mth.lerp(back, 0.45F, 1.0F);
        float width = Mth.lerp(k, 1.25F, 1.0F);
        float jitter = (1.0F - k) * 0.04F;
        poseStack.translate(Mth.sin(entity.tickCount * 2.1F) * jitter, 0.0F, Mth.cos(entity.tickCount * 1.7F) * jitter);
        poseStack.scale(width, height, width);
    }

    @Override
    public ResourceLocation getTextureLocation(CopyistEntity entity) {
        return switch (showing) {
            case CopyistEntity.COW -> COW;
            case CopyistEntity.PIG -> PIG;
            case CopyistEntity.SHEEP -> SHEEP;
            case CopyistEntity.CHICKEN -> CHICKEN;
            default -> TRUE_FORM[Math.floorMod(entity.getHide(), TRUE_FORM.length)];
        };
    }

    private class FurLayer extends RenderLayer<CopyistEntity, EntityModel<CopyistEntity>> {
        FurLayer(RenderLayerParent<CopyistEntity, EntityModel<CopyistEntity>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, CopyistEntity entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (showing != CopyistEntity.SHEEP || entity.isInvisible()) return;
            sheepFur.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
            sheepFur.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(SHEEP_FUR));
            sheepFur.renderToBuffer(poseStack, vc, light, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), 0.9F, 0.9F, 0.88F, 1.0F);
        }
    }

    /** Its own eyes, glinting through the eyeholes of the mask. */
    private class TrueEyesLayer extends RenderLayer<CopyistEntity, EntityModel<CopyistEntity>> {
        TrueEyesLayer(RenderLayerParent<CopyistEntity, EntityModel<CopyistEntity>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int light, CopyistEntity entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (showing != CopyistEntity.REVEALED || entity.isInvisible()) return;
            getParentModel().renderToBuffer(poseStack, buffers.getBuffer(TRUE_EYES), 15728640, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
