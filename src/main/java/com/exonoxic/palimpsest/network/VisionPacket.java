package com.exonoxic.palimpsest.network;

import com.exonoxic.palimpsest.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Tells one client to play a purely visual/aural effect. The server never trusts these back. */
public record VisionPacket(int type, int duration, float intensity) {
    public static final int FOG = 0;
    public static final int SKY_GLIMPSE = 1;
    public static final int SILENCE = 2;
    public static final int INK_RAIN = 3;
    public static final int FLASH = 4;
    public static final int SHAKE = 5;
    public static final int WHITEOUT = 6;
    public static final int PAGE_TURN = 7;
    public static final int VIGNETTE_FACE = 8;

    public static VisionPacket of(int type, int duration, float intensity) {
        return new VisionPacket(type, duration, intensity);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(type);
        buf.writeVarInt(duration);
        buf.writeFloat(intensity);
    }

    public static VisionPacket decode(FriendlyByteBuf buf) {
        return new VisionPacket(buf.readVarInt(), buf.readVarInt(), buf.readFloat());
    }

    public static void handle(VisionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.vision(msg));
    }
}
