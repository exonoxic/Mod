package com.exonoxic.palimpsest.client;

import com.exonoxic.palimpsest.client.screen.CodexScreen;
import com.exonoxic.palimpsest.client.screen.LoreScreen;
import net.minecraft.client.Minecraft;

/** Entry points common code may call through DistExecutor. */
public final class ClientHooks {
    public static void openLore(String kind, int number) {
        Minecraft.getInstance().setScreen(new LoreScreen(kind, number));
    }

    public static void openCodex() {
        Minecraft.getInstance().setScreen(new CodexScreen());
    }

    private ClientHooks() {}
}
