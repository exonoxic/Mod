package com.exonoxic.palimpsest.network;

import com.exonoxic.palimpsest.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public record SyncCodexPacket(List<String> entries) {
    public SyncCodexPacket(Collection<String> entries) {
        this(new ArrayList<>(entries));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (String s : entries) buf.writeUtf(s, 128);
    }

    public static SyncCodexPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<String> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(buf.readUtf(128));
        return new SyncCodexPacket(list);
    }

    public static void handle(SyncCodexPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.syncCodex(msg));
    }
}
