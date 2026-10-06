package com.exonoxic.palimpsest.client;

import com.exonoxic.palimpsest.client.screen.EndingScreen;
import com.exonoxic.palimpsest.network.OpenScreenPacket;
import com.exonoxic.palimpsest.network.SyncBleedPacket;
import com.exonoxic.palimpsest.network.SyncCodexPacket;
import com.exonoxic.palimpsest.network.VisionPacket;
import net.minecraft.client.Minecraft;

/** Client-side packet handling. Only ever class-loaded on the physical client. */
public final class ClientPacketHandlers {
    public static void syncBleed(SyncBleedPacket msg) {
        ClientBleedState.set(msg.bleed(), msg.ending());
    }

    public static void syncCodex(SyncCodexPacket msg) {
        ClientBleedState.setCodex(msg.entries());
    }

    public static void vision(VisionPacket msg) {
        Visions.start(msg.type(), msg.duration(), msg.intensity());
    }

    public static void openScreen(OpenScreenPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        switch (msg.screen()) {
            case OpenScreenPacket.ENDING_CHOICE -> mc.setScreen(new EndingScreen(msg.pos()));
            case OpenScreenPacket.CODEX -> ClientHooks.openCodex();
            default -> {}
        }
    }

    private ClientPacketHandlers() {}
}
