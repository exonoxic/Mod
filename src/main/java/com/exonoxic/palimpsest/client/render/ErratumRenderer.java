package com.exonoxic.palimpsest.client.render;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.client.ApparitionVisibility;
import com.exonoxic.palimpsest.client.model.ErratumEyeModel;
import com.exonoxic.palimpsest.client.model.ErratumModel;
import com.exonoxic.palimpsest.entity.ErratumEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The Erratum is a block that is in the wrong place.
 * <p>
 * Still, it is almost perfect: it sits a pixel off the grid and a hair out of square, which is
 * the only tell. Come close and an eye splits open in the side that faces you, follows you,
 * blinks, and opens wider when you look away. When nobody is watching it rises on six jointed
 * legs, shedding crumbs of whatever it is pretending to be, and underneath it is all mouth.
 */
public class ErratumRenderer extends EntityRenderer<ErratumEntity> {
    private static final ResourceLocation BODY = Palimpsest.id("textures/entity/erratum.png");
    private static final ResourceLocation EYE = Palimpsest.id("textures/entity/erratum_eye.png");
    private static final float LIFT = 5.0F / 16.0F;
    private static final float OFF_X = 1.0F / 16.0F;
    private static final float OFF_Z = -1.0F / 32.0F;
    private static final float TWIST = 1.8F;

    private final ErratumModel<ErratumEntity> body;
    private final ErratumEyeModel<ErratumEntity> eye;

    public ErratumRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.body = new ErratumModel<>(ctx.bakeLayer(ErratumModel.LAYER));
        this.eye = new ErratumEyeModel<>(ctx.bakeLayer(ErratumEyeModel.LAYER));
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(ErratumEntity entity, Frustum frustum, double x, double y, double z) {
        return ApparitionVisibility.visible(entity) && super.shouldRender(entity, frustum, x, y, z);
    }

    private static float approach(float from, float to, float step) {
        return from < to ? Math.min(to, from + step) : Math.max(to, from - step);
    }

    @Override
    public void render(ErratumEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        float now = entity.tickCount + partialTick;
        float dt = Float.isNaN(entity.animTime) ? 0.0F : Mth.clamp(now - entity.animTime, 0.0F, 5.0F);
        entity.animTime = now;
        boolean moving = entity.isMoving();
        BlockState state = entity.getMimic();

        entity.animLegs = approach(entity.animLegs, moving ? 1.0F : 0.0F, dt * 0.18F);
        if (moving && !entity.animWasMoving) crumble(entity, state);
        entity.animWasMoving = moving;
        float ext = entity.animLegs * entity.animLegs * (3.0F - 2.0F * entity.animLegs);
        float lift = LIFT * ext;

        Camera camera = this.entityRenderDispatcher.camera;
        Vec3 center = entity.getPosition(partialTick).add(0.0D, 0.5D + lift, 0.0D);
        Vec3 toCam = camera.getPosition().subtract(center);
        double dist = toCam.length();
        Vector3f look = camera.getLookVector();
        boolean stared = dist > 0.01D && (look.x() * -toCam.x + look.y() * -toCam.y + look.z() * -toCam.z) / dist > 0.93D;
        float eyeTarget = !moving && dist < 7.0D ? (stared ? 0.35F : 1.0F) : 0.0F;
        entity.animEye = approach(entity.animEye, eyeTarget, dt * 0.08F);
        float open = entity.animEye;
        if (Mth.sin(now * 0.07F + entity.getId()) > 0.985F) open *= 0.1F;

        // The block, a pixel off the grid and a hair out of square.
        if (state.getRenderShape() == RenderShape.MODEL) {
            poseStack.pushPose();
            poseStack.translate(0.0F, lift, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(TWIST));
            poseStack.translate(-0.5F + OFF_X, 0.0F, -0.5F + OFF_Z);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, buffers, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        // Legs and mouth, only once it has started to rise.
        if (ext > 0.01F) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot)));
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(0.0F, -1.501F - lift, 0.0F);
            entity.setRenderState(ext, 0.0F, 0.0F);
            body.setupAnim(entity, entity.walkAnimation.position(partialTick), entity.walkAnimation.speed(partialTick), now, 0.0F, 0.0F);
            body.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(BODY)), light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }

        // The eye, in whichever side faces you.
        if (open > 0.02F && state.getRenderShape() == RenderShape.MODEL) {
            float faceYaw;
            if (Math.abs(toCam.x) > Math.abs(toCam.z)) faceYaw = toCam.x > 0 ? -90.0F : 90.0F;
            else faceYaw = toCam.z > 0 ? 180.0F : 0.0F;
            double rad = Math.toRadians(faceYaw + TWIST);
            double localX = Math.cos(rad) * toCam.x - Math.sin(rad) * toCam.z;
            float irisX = Mth.clamp((float) (-localX / Math.max(dist, 0.5D)) * 4.0F, -2.5F, 2.5F);
            float irisY = Mth.clamp((float) (-toCam.y / Math.max(dist, 0.5D)) * 2.0F, -0.5F, 0.5F);
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.55F + lift, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(TWIST));
            poseStack.translate(OFF_X, 0.0F, OFF_Z);
            poseStack.mulPose(Axis.YP.rotationDegrees(faceYaw));
            poseStack.translate(0.0F, 0.0F, -0.503F);
            poseStack.scale(-1.0F, -open, 1.0F);
            entity.setRenderState(ext, irisX, irisY);
            eye.setupAnim(entity, 0.0F, 0.0F, now, 0.0F, 0.0F);
            eye.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(EYE)), light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    /** Crumbs of the block it is imitating fall away as it rises. */
    private static void crumble(ErratumEntity entity, BlockState state) {
        if (state.isAir()) return;
        BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < 14; i++) {
            double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5D) * 1.05D;
            double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5D) * 1.05D;
            entity.level().addParticle(dust, x, entity.getY() + entity.getRandom().nextDouble() * 0.4D, z, 0.0D, 0.05D, 0.0D);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ErratumEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
