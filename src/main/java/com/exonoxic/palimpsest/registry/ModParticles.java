package com.exonoxic.palimpsest.registry;

import com.exonoxic.palimpsest.Palimpsest;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Palimpsest.MODID);

    public static final RegistryObject<SimpleParticleType> INK_DRIP = PARTICLES.register("ink_drip", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> ASH_FLECK = PARTICLES.register("ash_fleck", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> RUBRIC_SPARK = PARTICLES.register("rubric_spark", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> ERASURE_MOTE = PARTICLES.register("erasure_mote", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> GLYPH = PARTICLES.register("glyph", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> MOTH_DUST = PARTICLES.register("moth_dust", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
