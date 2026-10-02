package com.exonoxic.palimpsest.block.entity;

import com.exonoxic.palimpsest.bleed.BleedEvents;
import com.exonoxic.palimpsest.entity.BlotlingEntity;
import com.exonoxic.palimpsest.entity.InkhoundEntity;
import com.exonoxic.palimpsest.registry.ModBlockEntities;
import com.exonoxic.palimpsest.registry.ModEffects;
import com.exonoxic.palimpsest.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Leaks a creature every so often, and blinds anyone who stands too close. */
public class TearBlockEntity extends BlockEntity {
    private int timer = 200;

    public TearBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEAR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TearBlockEntity be) {
        if (!(level instanceof ServerLevel server)) return;
        if (level.getGameTime() % 20 == 0) {
            for (Player p : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(3))) {
                if (!p.isCreative() && !p.isSpectator()) p.addEffect(new MobEffectInstance(ModEffects.INKBLIND.get(), 60, 0, false, true, true));
            }
        }
        if (--be.timer > 0) return;
        be.timer = 400 + level.random.nextInt(600);
        be.setChanged();
        if (level.getDifficulty() == Difficulty.PEACEFUL) return;
        if (level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 48, false) == null) return;
        int nearby = level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(16), m -> m.getType().is(BleedEvents.INKBORN)).size();
        if (nearby >= 4) return;
        Mob leaked;
        if (level.random.nextInt(4) == 0) {
            InkhoundEntity hound = ModEntities.INKHOUND.get().create(server);
            leaked = hound;
        } else {
            BlotlingEntity blot = ModEntities.BLOTLING.get().create(server);
            if (blot != null) blot.setSize(1 + level.random.nextInt(2), true);
            leaked = blot;
        }
        if (leaked == null) return;
        leaked.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360F, 0);
        leaked.finalizeSpawn(server, server.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
        server.addFreshEntity(leaked);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Timer", timer);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        timer = tag.getInt("Timer");
    }
}
