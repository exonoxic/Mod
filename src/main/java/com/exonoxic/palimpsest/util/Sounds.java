package com.exonoxic.palimpsest.util;

import com.exonoxic.palimpsest.config.CommonConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Sounds that only one player hears: the backbone of "did anyone else hear that?". */
public final class Sounds {
    public static void playTo(ServerPlayer player, SoundEvent sound, SoundSource source, Vec3 pos, float volume, float pitch) {
        float scaled = (float) (volume * Math.min(1.5D, 0.5D + 0.5D * CommonConfig.HORROR_INTENSITY.get()));
        player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source,
                pos.x, pos.y, pos.z, scaled, pitch, player.getRandom().nextLong()));
    }

    /** A point {@code distance} blocks behind the player at ear height, with some sideways jitter. */
    public static Vec3 behind(ServerPlayer player, double distance, double jitter) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(0, 0, 1);
        flat = flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0, flat.x);
        double j = (player.getRandom().nextDouble() - 0.5D) * 2D * jitter;
        return player.getEyePosition().subtract(flat.scale(distance)).add(side.scale(j));
    }

    /** Right beside one ear. */
    public static Vec3 atEar(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 side = new Vec3(-look.z, 0, look.x);
        if (side.lengthSqr() < 1.0E-4) side = new Vec3(1, 0, 0);
        side = side.normalize().scale(player.getRandom().nextBoolean() ? 1.2D : -1.2D);
        return player.getEyePosition().add(side);
    }

    private Sounds() {}
}
