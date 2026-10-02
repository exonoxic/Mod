package com.exonoxic.palimpsest.client;

import com.exonoxic.palimpsest.config.ClientConfig;
import com.exonoxic.palimpsest.network.VisionPacket;
import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Timed, client-only effects the server asks for. Each respects the player's accessibility
 * settings: fog can be disabled, flashes and sky changes are skipped when flashing effects are
 * off, and camera motion honours the distortion toggle.
 */
public final class Visions {
    private static int fogTicks, fogMax;
    private static float fogStrength;
    private static int skyTicks, skyMax;
    private static int silenceTicks;
    private static int inkRainTicks;
    private static int flashTicks;
    private static int shakeTicks, shakeMax;
    private static float shakeStrength;
    private static int whiteoutTicks, whiteoutMax;
    private static int pageTurnTicks, pageTurnMax;
    private static int faceTicks;

    public static void start(int type, int duration, float intensity) {
        switch (type) {
            case VisionPacket.FOG -> {
                if (!ClientConfig.FOG_EFFECTS.get()) return;
                fogTicks = fogMax = duration;
                fogStrength = intensity;
            }
            case VisionPacket.SKY_GLIMPSE -> {
                if (!ClientConfig.VISUAL_HORROR.get()) return;
                skyTicks = skyMax = duration;
            }
            case VisionPacket.SILENCE -> {
                silenceTicks = duration;
                Minecraft.getInstance().getSoundManager().stop();
            }
            case VisionPacket.INK_RAIN -> inkRainTicks = duration;
            case VisionPacket.FLASH -> {
                if (ClientConfig.FLASHING_EFFECTS.get()) flashTicks = duration;
            }
            case VisionPacket.SHAKE -> {
                if (!ClientConfig.SCREEN_DISTORTION.get()) return;
                shakeTicks = shakeMax = duration;
                shakeStrength = intensity;
            }
            case VisionPacket.WHITEOUT -> {
                if (duration <= 0) {
                    whiteoutTicks = Math.min(whiteoutTicks, 20);
                    return;
                }
                if (whiteoutTicks <= 0) whiteoutMax = duration;
                whiteoutTicks = Math.max(whiteoutTicks, duration);
                whiteoutMax = Math.max(whiteoutMax, whiteoutTicks);
            }
            case VisionPacket.PAGE_TURN -> {
                pageTurnTicks = pageTurnMax = duration;
            }
            case VisionPacket.VIGNETTE_FACE -> {
                if (ClientConfig.VISUAL_HORROR.get() && ClientConfig.INK_VIGNETTE.get()) faceTicks = duration;
            }
            default -> {}
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        if (fogTicks > 0) fogTicks--;
        if (skyTicks > 0) skyTicks--;
        if (flashTicks > 0) flashTicks--;
        if (shakeTicks > 0) shakeTicks--;
        if (whiteoutTicks > 0) whiteoutTicks--;
        if (pageTurnTicks > 0) pageTurnTicks--;
        if (faceTicks > 0) faceTicks--;
        if (silenceTicks > 0 && --silenceTicks == 0 && mc.player != null) {
            float vol = ClientConfig.SOFTEN_STINGERS.get() ? 0.35F : 0.9F;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.EVENT_SILENCE_BREAK.get(), 1.0F, vol));
        }
        if (inkRainTicks > 0) {
            inkRainTicks--;
            LocalPlayer p = mc.player;
            if (p != null && mc.level != null && ClientConfig.PARTICLES.get()) {
                RandomSource r = p.getRandom();
                for (int i = 0; i < 6; i++) {
                    double x = p.getX() + (r.nextDouble() - 0.5) * 24;
                    double z = p.getZ() + (r.nextDouble() - 0.5) * 24;
                    double y = p.getY() + 10 + r.nextDouble() * 6;
                    mc.level.addParticle(ModParticles.INK_DRIP.get(), x, y, z, 0, -0.6, 0);
                }
            }
        }
    }

    public static void reset() {
        fogTicks = skyTicks = silenceTicks = inkRainTicks = flashTicks = shakeTicks = whiteoutTicks = pageTurnTicks = faceTicks = 0;
    }

    private static float envelope(int ticks, int max, float partial, int fade) {
        if (ticks <= 0 || max <= 0) return 0F;
        float t = ticks - partial;
        float in = Mth.clamp((max - t) / fade, 0F, 1F);
        float out = Mth.clamp(t / fade, 0F, 1F);
        return Math.min(in, out);
    }

    public static float fog(float partial) {
        return envelope(fogTicks, fogMax, partial, 100) * fogStrength;
    }

    public static float sky(float partial) {
        return envelope(skyTicks, skyMax, partial, 20);
    }

    public static float whiteout(float partial) {
        return envelope(whiteoutTicks, whiteoutMax, partial, 20);
    }

    public static float pageTurn(float partial) {
        if (pageTurnTicks <= 0 || pageTurnMax <= 0) return 0F;
        float t = 1F - (pageTurnTicks - partial) / pageTurnMax;
        return Mth.sin(t * (float) Math.PI);
    }

    public static boolean silenced() {
        return silenceTicks > 0;
    }

    public static float flash() {
        return flashTicks > 0 ? 1F : 0F;
    }

    public static boolean face() {
        return faceTicks > 0;
    }

    /** Camera roll/pitch jitter in degrees. */
    public static float shake(float partial, RandomSource random) {
        if (shakeTicks <= 0) return 0F;
        float env = envelope(shakeTicks, shakeMax, partial, 10);
        return (random.nextFloat() - 0.5F) * 6F * shakeStrength * env;
    }

    private Visions() {}
}
