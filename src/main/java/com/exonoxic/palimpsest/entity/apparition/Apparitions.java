package com.exonoxic.palimpsest.entity.apparition;

import com.exonoxic.palimpsest.config.CommonConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class Apparitions {
    /** Creates, positions (facing the player) and adds a mob; returns null if creation failed. */
    @Nullable
    public static <T extends Mob> T spawn(EntityType<T> type, ServerLevel level, BlockPos pos, ServerPlayer facing, Consumer<T> setup) {
        T mob = type.create(level);
        if (mob == null) return null;
        Vec3 to = facing.position().subtract(Vec3.atBottomCenterOf(pos));
        float yaw = (float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90.0D);
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0F);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
        setup.accept(mob);
        if (!level.addFreshEntity(mob)) return null;
        return mob;
    }

    /** The viewer to record in synced data: the haunted player if apparitions are private. */
    public static Optional<UUID> viewerFor(ServerPlayer player) {
        return CommonConfig.SHARED_APPARITIONS.get() ? Optional.empty() : Optional.of(player.getUUID());
    }

    private Apparitions() {}
}
