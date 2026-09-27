package com.exonoxic.palimpsest.entity.boss;

import net.minecraft.world.entity.player.Player;

/** Bosses cannot be erased outright and can react to being revealed by light or ink. */
public interface PalimpsestBoss {
    default void onRevealed(Player by) {
    }
}
