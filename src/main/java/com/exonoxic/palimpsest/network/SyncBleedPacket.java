package com.exonoxic.palimpsest.network;

import com.exonoxic.palimpsest.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncBleedPacket(float bleed, int ending) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(bleed);
        buf.writeVarInt(ending);
    }

    public static SyncBleedPacket decode(FriendlyByteBuf buf) {
        return new SyncBleedPacket(buf.readFloat(), buf.readVarInt());
    }

    public static void handle(SyncBleedPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.syncBleed(msg));
    }
}
