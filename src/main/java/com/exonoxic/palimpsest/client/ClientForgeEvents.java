package com.exonoxic.palimpsest.client;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.client.sky.UndertextSky;
import com.exonoxic.palimpsest.config.ClientConfig;
import com.exonoxic.palimpsest.entity.boss.RasureEntity;
import com.exonoxic.palimpsest.registry.ModEffects;
import com.exonoxic.palimpsest.registry.ModItems;
import com.exonoxic.palimpsest.world.dimension.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Palimpsest.MODID, value = Dist.CLIENT)
public final class ClientForgeEvents {

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Visions.tick();
        Minecraft mc = Minecraft.getInstance();
        while (KeyBindings.OPEN_CODEX.consumeClick()) {
            if (mc.player != null && mc.screen == null && mc.player.getInventory().contains(new ItemStack(ModItems.COMMONPLACE_BOOK.get()))) {
                ClientHooks.openCodex();
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientBleedState.reset();
        Visions.reset();
    }

    /** The Rasure brings its own music. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide && event.getEntity() instanceof RasureEntity rasure) {
            Minecraft.getInstance().getSoundManager().play(new BossMusicInstance(rasure));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onSound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) return;
        if (Visions.silenced() && !sound.getLocation().getPath().equals("event.silence_break")) {
            event.setSound(null);
            return;
        }
        if (!"palimpsest".equals(sound.getLocation().getNamespace())) return;
        CaptionOverlay.onSound(sound);
        float scale = ClientConfig.HORROR_VOLUME.get().floatValue();
        if (ClientConfig.SOFTEN_STINGERS.get() && sound.getLocation().getPath().startsWith("event.stinger")) scale *= 0.4F;
        if (scale < 0.999F && !(sound instanceof TickableSoundInstance)) {
            event.setSound(new ScaledSoundInstance(sound, scale));
        }
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        float partial = (float) event.getPartialTick();
        float far = event.getFarPlaneDistance();
        float near = event.getNearPlaneDistance();
        boolean changed = false;

        if (mc.level.dimension() == ModDimensions.UNDERTEXT && ClientConfig.FOG_EFFECTS.get() && event.getMode() == FogRenderer.FogMode.FOG_TERRAIN) {
            float dark = ClientConfig.DARKNESS_INTENSITY.get().floatValue();
            float limit = 110F - 62F * dark;
            far = Math.min(far, limit);
            // Clear for a few blocks, then a steady fade: close threats stay readable, distance does not.
            near = Math.min(near, far * 0.12F);
            changed = true;
        }
        float vision = Visions.fog(partial);
        if (vision > 0F) {
            far = far + (10F - far) * vision;
            near = near * (1F - vision);
            changed = true;
        }
        float white = Visions.whiteout(partial);
        if (white > 0F) {
            far = far + (7F - far) * white;
            near = near * (1F - white);
            changed = true;
        }
        if (mc.player.hasEffect(ModEffects.INKBLIND.get())) {
            far = Math.min(far, 5F);
            near = 0F;
            changed = true;
        }
        if (changed) {
            event.setFarPlaneDistance(far);
            event.setNearPlaneDistance(near);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        float partial = (float) event.getPartialTick();
        float r = event.getRed(), g = event.getGreen(), b = event.getBlue();
        if (mc.level.dimension() == ModDimensions.UNDERTEXT) {
            float grey = (r + g + b) / 3F;
            r = grey * 1.05F + 0.08F;
            g = grey * 0.98F + 0.06F;
            b = grey * 0.86F + 0.03F;
        }
        float vision = Visions.fog(partial);
        if (vision > 0) {
            r += (0.55F - r) * vision;
            g += (0.55F - g) * vision;
            b += (0.58F - b) * vision;
        }
        float sky = Visions.sky(partial);
        if (sky > 0) {
            r += (0.62F - r) * sky;
            g += (0.57F - g) * sky;
            b += (0.48F - b) * sky;
        }
        float white = Visions.whiteout(partial);
        if (white > 0) {
            r += (0.95F - r) * white;
            g += (0.94F - g) * white;
            b += (0.91F - b) * white;
        }
        if (mc.player.hasEffect(ModEffects.INKBLIND.get())) {
            r = 0.02F;
            g = 0.02F;
            b = 0.03F;
        }
        event.setRed(r);
        event.setGreen(g);
        event.setBlue(b);
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !ClientConfig.SCREEN_DISTORTION.get()) return;
        float partial = (float) event.getPartialTick();
        float roll = Visions.shake(partial, mc.player.getRandom()) + Visions.pageTurn(partial) * 4F;
        if (roll != 0F) event.setRoll(event.getRoll() + roll);
    }

    /** During a sky glimpse the Undertext's sky is drawn, fading in and out, over the real one. */
    @SubscribeEvent
    public static void afterSky(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() == ModDimensions.UNDERTEXT) return;
        float sky = Visions.sky(event.getPartialTick());
        if (sky <= 0F) return;
        UndertextSky.render(mc.level, event.getRenderTick(), event.getPartialTick(), event.getPoseStack(), event.getProjectionMatrix(), sky);
    }

    private ClientForgeEvents() {}
}
