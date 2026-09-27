package com.exonoxic.palimpsest.client;

import com.exonoxic.palimpsest.entity.apparition.Apparition;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.UUID;

/** Private apparitions are simply not drawn for anyone but the player they came for. */
public final class ApparitionVisibility {
    public static boolean visible(Entity entity) {
        if (!(entity instanceof Apparition a)) return true;
        Optional<UUID> viewer = a.exclusiveViewer();
        if (viewer.isEmpty()) return true;
        var player = Minecraft.getInstance().player;
        return player != null && player.getUUID().equals(viewer.get());
    }

    private ApparitionVisibility() {}
}
