package com.exonoxic.palimpsest.network;

import com.exonoxic.palimpsest.client.ClientPacketHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenScreenPacket(int screen, int arg, BlockPos pos) {
    public static final int ENDING_CHOICE = 0;
    public static final int CODEX = 1;

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(screen);
        buf.writeVarInt(arg);
        buf.writeBlockPos(pos);
    }

    public static OpenScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenScreenPacket(buf.readVarInt(), buf.readVarInt(), buf.readBlockPos());
    }

    public static void handle(OpenScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.openScreen(msg));
    }
}
