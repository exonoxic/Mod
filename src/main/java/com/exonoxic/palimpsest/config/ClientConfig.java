package com.exonoxic.palimpsest.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Per-player presentation and accessibility settings. */
public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue INK_VIGNETTE;
    public static final ForgeConfigSpec.DoubleValue VIGNETTE_STRENGTH;
    public static final ForgeConfigSpec.BooleanValue SCREEN_DISTORTION;
    public static final ForgeConfigSpec.BooleanValue FLASHING_EFFECTS;
    public static final ForgeConfigSpec.BooleanValue FOG_EFFECTS;
    public static final ForgeConfigSpec.BooleanValue VISUAL_HORROR;
    public static final ForgeConfigSpec.DoubleValue DARKNESS_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue HORROR_VOLUME;
    public static final ForgeConfigSpec.BooleanValue SOFTEN_STINGERS;
    public static final ForgeConfigSpec.BooleanValue CAPTIONS;
    public static final ForgeConfigSpec.BooleanValue PARTICLES;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Visual effects.").push("visuals");
        INK_VIGNETTE = b.comment("Ink creeping in at the edges of the screen as things get worse.").define("inkVignette", true);
        VIGNETTE_STRENGTH = b.defineInRange("vignetteStrength", 1.0D, 0.0D, 1.0D);
        SCREEN_DISTORTION = b.comment("Camera sway, tilt and shake during some events.").define("screenDistortion", true);
        FLASHING_EFFECTS = b.comment("Sudden sky and screen flashes. Disable if you are sensitive to flashing light.").define("flashingEffects", true);
        FOG_EFFECTS = b.comment("Event-driven fog in the Overworld and the dense fog of the Undertext.").define("fogEffects", true);
        VISUAL_HORROR = b.comment("Brief visual apparitions (faces in the vignette, the sky changing). Creatures are unaffected.").define("visualHorror", true);
        DARKNESS_INTENSITY = b.comment("How dark and desaturated the Undertext is. 1 = intended look.").defineInRange("darknessIntensity", 1.0D, 0.25D, 1.0D);
        PARTICLES = b.comment("Ambient ink/ash particles.").define("ambientParticles", true);
        b.pop();

        b.comment("Audio.").push("audio");
        HORROR_VOLUME = b.comment("Volume multiplier for Palimpsest sounds (ambience, events, creatures).").defineInRange("horrorVolume", 1.0D, 0.0D, 1.0D);
        SOFTEN_STINGERS = b.comment("Play loud stingers at reduced volume.").define("softenStingers", false);
        b.pop();

        b.comment("Accessibility.").push("accessibility");
        CAPTIONS = b.comment("Show a small caption for important directional horror sounds (knocking, footsteps behind you) even when vanilla subtitles are off.")
                .define("importantSoundCaptions", false);
        b.pop();

        SPEC = b.build();
    }

    private ClientConfig() {}
}
