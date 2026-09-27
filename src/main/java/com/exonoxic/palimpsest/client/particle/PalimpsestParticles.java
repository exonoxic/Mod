package com.exonoxic.palimpsest.client.particle;

import com.exonoxic.palimpsest.registry.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;

/** All six particles share one class and differ only in their tuning. */
public final class PalimpsestParticles {

    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.INK_DRIP.get(), s -> new Provider(s, Tuning.INK_DRIP));
        event.registerSpriteSet(ModParticles.ASH_FLECK.get(), s -> new Provider(s, Tuning.ASH));
        event.registerSpriteSet(ModParticles.RUBRIC_SPARK.get(), s -> new Provider(s, Tuning.SPARK));
        event.registerSpriteSet(ModParticles.ERASURE_MOTE.get(), s -> new Provider(s, Tuning.ERASURE));
        event.registerSpriteSet(ModParticles.GLYPH.get(), s -> new Provider(s, Tuning.GLYPH));
        event.registerSpriteSet(ModParticles.MOTH_DUST.get(), s -> new Provider(s, Tuning.DUST));
    }

    enum Tuning {
        //        gravity lifeMin lifeMax size  fade  bright  friction
        INK_DRIP(0.9F, 30, 50, 0.08F, false, false, 0.98F),
        ASH(-0.02F, 60, 120, 0.06F, true, false, 0.96F),
        SPARK(-0.05F, 15, 30, 0.07F, true, true, 0.9F),
        ERASURE(-0.03F, 20, 40, 0.1F, true, true, 0.92F),
        GLYPH(-0.01F, 30, 60, 0.12F, true, false, 0.95F),
        DUST(0.05F, 20, 40, 0.04F, true, false, 0.95F);

        final float gravity;
        final int lifeMin, lifeMax;
        final float size;
        final boolean fade, bright;
        final float friction;

        Tuning(float gravity, int lifeMin, int lifeMax, float size, boolean fade, boolean bright, float friction) {
            this.gravity = gravity;
            this.lifeMin = lifeMin;
            this.lifeMax = lifeMax;
            this.size = size;
            this.fade = fade;
            this.bright = bright;
            this.friction = friction;
        }
    }

    static final class PageParticle extends TextureSheetParticle {
        private final boolean fade;
        private final boolean bright;

        PageParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, Tuning t) {
            super(level, x, y, z, vx, vy, vz);
            this.xd = vx;
            this.yd = vy;
            this.zd = vz;
            this.gravity = t.gravity;
            this.friction = t.friction;
            this.lifetime = t.lifeMin + random.nextInt(Math.max(1, t.lifeMax - t.lifeMin));
            this.quadSize = t.size * (0.7F + random.nextFloat() * 0.6F);
            this.fade = t.fade;
            this.bright = t.bright;
            this.hasPhysics = t == Tuning.INK_DRIP;
            pickSprite(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (fade) alpha = 1.0F - (float) age / lifetime;
            if (onGround && gravity > 0.5F) remove();
        }

        @Override
        protected int getLightColor(float partial) {
            return bright ? 0xF000F0 : super.getLightColor(partial);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    record Provider(SpriteSet sprites, Tuning tuning) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new PageParticle(level, x, y, z, vx, vy, vz, sprites, tuning);
        }
    }

    private PalimpsestParticles() {}
}
