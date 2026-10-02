package com.exonoxic.palimpsest.client;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.client.model.*;
import com.exonoxic.palimpsest.client.particle.PalimpsestParticles;
import com.exonoxic.palimpsest.client.render.*;
import com.exonoxic.palimpsest.client.screen.ScriptoriumDeskScreen;
import com.exonoxic.palimpsest.client.sky.UndertextEffects;
import com.exonoxic.palimpsest.item.DowsingQuillItem;
import com.exonoxic.palimpsest.item.MarginCompassItem;
import com.exonoxic.palimpsest.registry.*;
import com.exonoxic.palimpsest.world.dimension.ModDimensions;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Palimpsest.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.SCRIPTORIUM_DESK.get(), ScriptoriumDeskScreen::new);
            ItemProperties.register(ModItems.MARGIN_COMPASS.get(), Palimpsest.id("angle"), (stack, level, entity, seed) -> {
                Entity holder = entity != null ? entity : stack.getEntityRepresentation();
                if (holder == null || level == null) return 0F;
                BlockPos target = MarginCompassItem.getTarget(stack, level);
                if (target == null) {
                    // No target: the needle turns slowly, never settling.
                    return (float) ((level.getGameTime() % 64L) / 64.0D);
                }
                double toTarget = Math.atan2(target.getZ() + 0.5 - holder.getZ(), target.getX() + 0.5 - holder.getX()) / (Math.PI * 2);
                double facing = Mth.positiveModulo(holder.getVisualRotationYInDegrees() / 360.0D, 1.0D);
                return (float) Mth.positiveModulo(0.5D - (facing - 0.25D - toTarget), 1.0D);
            });
            ItemProperties.register(ModItems.DOWSING_QUILL.get(), Palimpsest.id("strength"),
                    (stack, level, entity, seed) -> DowsingQuillItem.strength(stack) / 3.0F);
        });
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        ModModelLayers.register(event);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FOXING_MOTH.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new FoxingMothModel<>(ctx.bakeLayer(FoxingMothModel.LAYER)), "foxing_moth", 0.1F, 0.7F));
        event.registerEntityRenderer(ModEntities.BLOTLING.get(), BlotlingRenderer::new);
        event.registerEntityRenderer(ModEntities.SMUDGE.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new SmudgeModel<>(ctx.bakeLayer(SmudgeModel.LAYER)), "smudge", 0.0F, 1.0F));
        event.registerEntityRenderer(ModEntities.MARGIN_CRAWLER.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new MarginCrawlerModel<>(ctx.bakeLayer(MarginCrawlerModel.LAYER)), "margin_crawler", 0.35F, 1.0F).withGlow());
        event.registerEntityRenderer(ModEntities.QUILLCROW.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new QuillcrowModel<>(ctx.bakeLayer(QuillcrowModel.LAYER)), "quillcrow", 0.25F, 1.3F).withGlow());
        event.registerEntityRenderer(ModEntities.PALE_STAG.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new PaleStagModel<>(ctx.bakeLayer(PaleStagModel.LAYER)), "pale_stag", 0.6F, 1.0F));
        event.registerEntityRenderer(ModEntities.INKHOUND.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new InkhoundModel<>(ctx.bakeLayer(InkhoundModel.LAYER)), "inkhound", 0.5F, 1.0F).withGlow());
        event.registerEntityRenderer(ModEntities.RUBRICATOR.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new RubricatorModel<>(ctx.bakeLayer(RubricatorModel.LAYER)), "rubricator", 0.5F, 1.0F).withGlow());
        event.registerEntityRenderer(ModEntities.KNOCKER.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new KnockerModel<>(ctx.bakeLayer(KnockerModel.LAYER)), "knocker", 0.5F, 1.0F).withGlow());
        event.registerEntityRenderer(ModEntities.COPYIST.get(), CopyistRenderer::new);
        event.registerEntityRenderer(ModEntities.LONGHAND.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new LonghandModel<>(ctx.bakeLayer(LonghandModel.LAYER)), "longhand", 0.4F, 1.0F).withGlow());
        event.registerEntityRenderer(ModEntities.REDACTED.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new RedactedModel<>(ctx.bakeLayer(RedactedModel.LAYER)), "redacted", 0.5F, 1.0F).withGlow());
        event.registerEntityRenderer(ModEntities.FAIR_COPY.get(), FairCopyRenderer::new);
        event.registerEntityRenderer(ModEntities.ERRATUM.get(), ErratumRenderer::new);
        event.registerEntityRenderer(ModEntities.PALEHAND.get(), PalehandRenderer::new);
        event.registerEntityRenderer(ModEntities.BOOKBINDER.get(), ctx -> new SimpleMobRenderer<>(ctx,
                new BookbinderModel<>(ctx.bakeLayer(BookbinderModel.LAYER)), "bookbinder", 1.2F, 1.0F).withGlow());
        event.registerEntityRenderer(ModEntities.RASURE.get(), RasureRenderer::new);
        event.registerEntityRenderer(ModEntities.INK_BOMB.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.BINDING_NEEDLE.get(), ThrownItemRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.READING_STAND.get(), ReadingStandRenderer::new);
    }

    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent event) {
        PalimpsestParticles.register(event);
    }

    @SubscribeEvent
    public static void dimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ModDimensions.UNDERTEXT_EFFECTS, new UndertextEffects());
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.VIGNETTE.id(), "ink", new InkOverlay());
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN_CODEX);
    }

    private ClientSetup() {}
}
