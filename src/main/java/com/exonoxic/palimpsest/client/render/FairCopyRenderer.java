package com.exonoxic.palimpsest.client.render;

import com.exonoxic.palimpsest.client.ApparitionVisibility;
import com.exonoxic.palimpsest.entity.FairCopyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.UUID;

/** Renders the Fair Copy with the skin of the player it copies. */
public class FairCopyRenderer extends LivingEntityRenderer<FairCopyEntity, PlayerModel<FairCopyEntity>> {
    private final PlayerModel<FairCopyEntity> wide;
    private final PlayerModel<FairCopyEntity> slim;

    public FairCopyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wide = this.model;
        this.slim = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
        addLayer(new FairCopyFaceLayer(this));
    }

    @Override
    public boolean shouldRender(FairCopyEntity entity, Frustum frustum, double x, double y, double z) {
        return ApparitionVisibility.visible(entity) && super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(FairCopyEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        this.model = isSlim(entity) ? slim : wide;
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    private static PlayerInfo info(FairCopyEntity entity) {
        Optional<UUID> id = entity.copyOf();
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        return id.isPresent() && conn != null ? conn.getPlayerInfo(id.get()) : null;
    }

    private static boolean isSlim(FairCopyEntity entity) {
        PlayerInfo info = info(entity);
        if (info != null) return "slim".equals(info.getModelName());
        return entity.copyOf().map(u -> "slim".equals(DefaultPlayerSkin.getSkinModelName(u))).orElse(false);
    }

    @Override
    public ResourceLocation getTextureLocation(FairCopyEntity entity) {
        PlayerInfo info = info(entity);
        if (info != null) return info.getSkinLocation();
        return entity.copyOf().map(DefaultPlayerSkin::getDefaultSkin).orElse(DefaultPlayerSkin.getDefaultSkin());
    }

    @Override
    protected void scale(FairCopyEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }
}
