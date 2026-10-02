package com.exonoxic.palimpsest.entity.apparition;

import com.exonoxic.palimpsest.registry.ModParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Bookkeeping for something that was put in the world for one particular player to glimpse.
 * It has a lifetime, and it can be told to vanish when that player gets too close.
 */
public final class ApparitionState {
    @Nullable
    private UUID haunted;
    private int life = -1;
    private double vanishDistance;

    public boolean active() {
        return haunted != null;
    }

    @Nullable
    public UUID haunted() {
        return haunted;
    }

    public void begin(ServerPlayer player, int lifetime, double vanishDistance) {
        this.haunted = player.getUUID();
        this.life = lifetime;
        this.vanishDistance = vanishDistance;
    }

    public void setVanishDistance(double d) {
        this.vanishDistance = d;
    }

    @Nullable
    public Player target(Level level) {
        return haunted == null ? null : level.getPlayerByUUID(haunted);
    }

    /** @return true when the apparition should go. */
    public boolean tick(Mob mob) {
        if (haunted == null) return false;
        if (life > 0 && --life == 0) return true;
        Player p = target(mob.level());
        if (p == null || !p.isAlive() || p.level() != mob.level()) return true;
        return vanishDistance > 0 && p.distanceToSqr(mob) < vanishDistance * vanishDistance;
    }

    public void save(CompoundTag tag) {
        if (haunted != null) {
            CompoundTag a = new CompoundTag();
            a.putUUID("Haunted", haunted);
            a.putInt("Life", life);
            a.putDouble("Vanish", vanishDistance);
            tag.put("Apparition", a);
        }
    }

    public void load(CompoundTag tag) {
        if (!tag.contains("Apparition")) return;
        CompoundTag a = tag.getCompound("Apparition");
        haunted = a.hasUUID("Haunted") ? a.getUUID("Haunted") : null;
        life = a.getInt("Life");
        vanishDistance = a.getDouble("Vanish");
    }

    /** Leaves without a body: a little ash, nothing else. */
    public static void vanish(Mob mob) {
        if (mob.level() instanceof ServerLevel server) {
            server.sendParticles(ModParticles.ASH_FLECK.get(), mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(),
                    12, mob.getBbWidth() * 0.4, mob.getBbHeight() * 0.4, mob.getBbWidth() * 0.4, 0.01);
        }
        mob.discard();
    }
}
