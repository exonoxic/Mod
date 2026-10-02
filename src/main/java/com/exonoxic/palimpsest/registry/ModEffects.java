package com.exonoxic.palimpsest.registry;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.effect.PalimpsestEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Palimpsest.MODID);

    /** You are being scraped thin: no healing of any kind, and higher levels hurt. */
    public static final RegistryObject<MobEffect> ERASURE = EFFECTS.register("erasure",
            () -> new PalimpsestEffect(MobEffectCategory.HARMFUL, 0xEDEAE0));
    /** Red ink on your skin: Bleed rises slower and inkborn find it harder to hold on to you. */
    public static final RegistryObject<MobEffect> WARDED = EFFECTS.register("warded",
            () -> new PalimpsestEffect(MobEffectCategory.BENEFICIAL, 0x8C1C13));
    /** Ink in the eyes: the world closes in to a few blocks. */
    public static final RegistryObject<MobEffect> INKBLIND = EFFECTS.register("inkblind",
            () -> new PalimpsestEffect(MobEffectCategory.HARMFUL, 0x101014));

    private ModEffects() {}
}
