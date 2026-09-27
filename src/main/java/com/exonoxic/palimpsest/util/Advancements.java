package com.exonoxic.palimpsest.util;

import com.exonoxic.palimpsest.Palimpsest;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Most Palimpsest advancements use minecraft:impossible criteria and are granted from code. */
public final class Advancements {
    public static void grant(ServerPlayer player, String path) {
        if (player.server == null) return;
        Advancement adv = player.server.getAdvancements().getAdvancement(Palimpsest.id(path));
        if (adv == null) {
            Palimpsest.LOGGER.debug("Missing advancement palimpsest:{}", path);
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
        if (progress.isDone()) return;
        List<String> remaining = new ArrayList<>();
        progress.getRemainingCriteria().forEach(remaining::add);
        for (String criterion : remaining) {
            player.getAdvancements().award(adv, criterion);
        }
    }

    public static boolean has(ServerPlayer player, String path) {
        if (player.server == null) return false;
        Advancement adv = player.server.getAdvancements().getAdvancement(Palimpsest.id(path));
        return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
    }

    private Advancements() {}
}
