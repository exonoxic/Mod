package com.exonoxic.palimpsest.client;

import com.exonoxic.palimpsest.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

/**
 * Accessibility: short captions with a direction arrow for the sounds that matter most
 * (knocking, footsteps behind you, something moving), even with vanilla subtitles off.
 */
public final class CaptionOverlay {
    private static final Set<String> IMPORTANT = Set.of("event.knock", "event.footsteps", "event.whisper", "event.breath",
            "entity.knocker.lunge", "entity.longhand.move", "entity.copyist.reveal", "entity.inkhound.howl", "block.tear.ambient");
    private static final Deque<Caption> CAPTIONS = new ArrayDeque<>();

    private record Caption(Component text, Vec3 pos, long until) {}

    public static void onSound(SoundInstance sound) {
        if (!ClientConfig.CAPTIONS.get() || !"palimpsest".equals(sound.getLocation().getNamespace())) return;
        String path = sound.getLocation().getPath();
        if (!IMPORTANT.contains(path)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        CAPTIONS.removeIf(c -> c.text().getString().equals(Component.translatable("subtitles.palimpsest." + path).getString()));
        CAPTIONS.addLast(new Caption(Component.translatable("subtitles.palimpsest." + path), new Vec3(sound.getX(), sound.getY(), sound.getZ()),
                mc.level.getGameTime() + 60));
        while (CAPTIONS.size() > 3) CAPTIONS.pollFirst();
    }

    static void render(GuiGraphics g, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (CAPTIONS.isEmpty() || mc.level == null || mc.player == null) return;
        long now = mc.level.getGameTime();
        CAPTIONS.removeIf(c -> c.until() < now);
        int y = height / 2 + 22;
        for (Caption c : CAPTIONS) {
            Vec3 to = c.pos().subtract(mc.player.getEyePosition());
            double angle = Mth.wrapDegrees(Math.toDegrees(Math.atan2(to.z, to.x)) - 90.0D - mc.player.getYRot());
            String arrow = Math.abs(angle) < 45 ? "▲ " : Math.abs(angle) > 135 ? "▼ " : angle > 0 ? "▶ " : "◀ ";
            Component line = Component.literal(arrow).append(c.text());
            int w = mc.font.width(line);
            g.fill(width / 2 - w / 2 - 3, y - 2, width / 2 + w / 2 + 3, y + 10, 0x90000000);
            g.drawString(mc.font, line, width / 2 - w / 2, y, 0xE8E2D0, false);
            y += 13;
        }
    }

    private CaptionOverlay() {}
}
