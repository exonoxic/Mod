package com.exonoxic.palimpsest.network;

import com.exonoxic.palimpsest.Palimpsest;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PacketHandler {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Palimpsest.id("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static boolean initialised;

    public static void init() {
        if (initialised) return;
        initialised = true;
        int id = 0;
        CHANNEL.messageBuilder(SyncBleedPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncBleedPacket::encode).decoder(SyncBleedPacket::decode)
                .consumerMainThread(SyncBleedPacket::handle).add();
        CHANNEL.messageBuilder(SyncCodexPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCodexPacket::encode).decoder(SyncCodexPacket::decode)
                .consumerMainThread(SyncCodexPacket::handle).add();
        CHANNEL.messageBuilder(VisionPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(VisionPacket::encode).decoder(VisionPacket::decode)
                .consumerMainThread(VisionPacket::handle).add();
        CHANNEL.messageBuilder(OpenScreenPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenScreenPacket::encode).decoder(OpenScreenPacket::decode)
                .consumerMainThread(OpenScreenPacket::handle).add();
        CHANNEL.messageBuilder(EndingChoicePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EndingChoicePacket::encode).decoder(EndingChoicePacket::decode)
                .consumerMainThread(EndingChoicePacket::handle).add();
    }

    public static void sendTo(ServerPlayer player, Object message) {
        if (player.connection == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    private PacketHandler() {}
}
