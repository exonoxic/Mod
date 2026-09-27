package com.exonoxic.palimpsest.entity.boss;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** Bosses cannot be erased outright and can react to being revealed by light or ink. */
public interface PalimpsestBoss {
    UUID GROUP_HEALTH_ID = UUID.fromString("5b1f0d4e-7a52-4f0b-9c55-2f6a2f1d9e41");
    String SCALED_KEY = "palimpsest:group_scaled";

    default void onRevealed(Player by) {
    }

    /**
     * The first time players come near, a boss gains +50% health for every player past the
     * first (up to eight). It happens once, so people arriving mid-fight change nothing.
     */
    static void scaleForGroup(Mob boss) {
        CompoundTag data = boss.getPersistentData();
        if (data.getBoolean(SCALED_KEY)) return;
        int players = boss.level().getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(40.0D),
                p -> p.isAlive() && !p.isSpectator()).size();
        if (players == 0) return;
        data.putBoolean(SCALED_KEY, true);
        AttributeInstance health = boss.getAttribute(Attributes.MAX_HEALTH);
        if (players < 2 || health == null || health.getModifier(GROUP_HEALTH_ID) != null) return;
        double bonus = 0.5D * (Math.min(players, 8) - 1);
        health.addPermanentModifier(new AttributeModifier(GROUP_HEALTH_ID, "Palimpsest group scaling", bonus,
                AttributeModifier.Operation.MULTIPLY_BASE));
        boss.setHealth(boss.getMaxHealth());
    }
}
