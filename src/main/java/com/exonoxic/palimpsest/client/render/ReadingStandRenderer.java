package com.exonoxic.palimpsest.client.render;

import com.exonoxic.palimpsest.block.entity.ReadingStandBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Shows the offering hovering over the stand, turning slowly. */
public class ReadingStandRenderer implements BlockEntityRenderer<ReadingStandBlockEntity> {
    public ReadingStandRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(ReadingStandBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = be.getItem();
        if (stack.isEmpty() || be.getLevel() == null) return;
        float t = be.getLevel().getGameTime() + partialTick;
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.05D + Math.sin(t / 12.0D) * 0.04D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees((t * 1.5F) % 360F));
        poseStack.scale(0.55F, 0.55F, 0.55F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND, light, overlay, poseStack, buffers,
                be.getLevel(), (int) be.getBlockPos().asLong());
        poseStack.popPose();
    }
}
