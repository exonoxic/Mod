package com.exonoxic.palimpsest.network;

import com.exonoxic.palimpsest.world.EndingHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sent when a player makes their choice at the Folio Stand. Validated server-side. */
public record EndingChoicePacket(int choice, BlockPos pos) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(choice);
        buf.writeBlockPos(pos);
    }

    public static EndingChoicePacket decode(FriendlyByteBuf buf) {
        return new EndingChoicePacket(buf.readVarInt(), buf.readBlockPos());
    }

    public static void handle(EndingChoicePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) EndingHandler.choose(player, msg.choice(), msg.pos());
    }
}
