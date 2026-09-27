package com.exonoxic.palimpsest.recipe;

import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.block.TearBlock;
import com.exonoxic.palimpsest.block.entity.BlankBlockEntity;
import com.exonoxic.palimpsest.entity.*;
import com.exonoxic.palimpsest.entity.boss.RasureEntity;
import com.exonoxic.palimpsest.horror.Spots;
import com.exonoxic.palimpsest.horror.WorldAlterations;
import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModEffects;
import com.exonoxic.palimpsest.registry.ModEntities;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.util.Advancements;
import com.exonoxic.palimpsest.world.ModStructures;
import com.exonoxic.palimpsest.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Conditions, effects and backlashes for rites. */
public final class RitualLogic {

    /** @return null if the rite may begin, otherwise a hint at what is wrong. */
    @Nullable
    public static Component checkConditions(RitualRecipe r, ServerLevel level, BlockPos pos, ServerPlayer player) {
        boolean undertext = level.dimension() == ModDimensions.UNDERTEXT;
        long dayTime = level.getDayTime() % 24000L;
        boolean night = undertext || (dayTime > 12800L && dayTime < 23200L);
        switch (r.time()) {
            case "night" -> { if (!night) return Component.translatable("message.palimpsest.ritual.need_night"); }
            case "day" -> { if (night) return Component.translatable("message.palimpsest.ritual.need_day"); }
            default -> {}
        }
        if (r.moonPhase() >= 0 && !undertext && level.getMoonPhase() != r.moonPhase()) {
            return Component.translatable("message.palimpsest.ritual.need_moon_" + r.moonPhase());
        }
        switch (r.weather()) {
            case "clear" -> { if (level.isRaining()) return Component.translatable("message.palimpsest.ritual.need_clear"); }
            case "rain" -> { if (!level.isRaining()) return Component.translatable("message.palimpsest.ritual.need_rain"); }
            case "thunder" -> { if (!level.isThundering()) return Component.translatable("message.palimpsest.ritual.need_thunder"); }
            default -> {}
        }
        if (!"any".equals(r.dimension()) && !level.dimension().location().toString().equals(r.dimension())) {
            return Component.translatable(undertext ? "message.palimpsest.ritual.need_overworld" : "message.palimpsest.ritual.need_undertext");
        }
        if (BleedManager.stage(player).index < r.minStage()) {
            return Component.translatable("message.palimpsest.ritual.not_ready");
        }
        if ("summon_rasure".equals(r.effect())) {
            var structure = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).get(ModStructures.LAST_FOLIO);
            boolean inArena = structure != null && level.structureManager().getStructureWithPieceAt(pos, structure).isValid();
            if (!inArena) return Component.translatable("message.palimpsest.ritual.need_last_folio");
            if (!level.getEntitiesOfClass(RasureEntity.class, new AABB(pos).inflate(64)).isEmpty()) {
                return Component.translatable("message.palimpsest.ritual.busy");
            }
        }
        return null;
    }

    public static void applyEffect(String effect, ServerLevel level, BlockPos pos, @Nullable ServerPlayer player, ItemStack catalyst) {
        switch (effect) {
            case "purify" -> purify(level, pos, 12);
            case "quiet" -> {
                if (player != null) BleedManager.add(player, -120F);
            }
            case "summon_rasure" -> {
                RasureEntity.summon(level, pos.above(2), player);
                if (player != null) Advancements.grant(player, "the_last_reading");
            }
            case "summon_rubricator" -> {
                RubricatorEntity r = ModEntities.RUBRICATOR.get().create(level);
                if (r != null) {
                    BlockPos at = Spots.localFloor(level, pos.getX() + 2, pos.getY(), pos.getZ() + 2, 4);
                    if (at == null) at = pos.above();
                    r.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360F, 0);
                    r.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
                    r.setVisitor(24000);
                    level.addFreshEntity(r);
                }
            }
            case "copy_catalyst" -> {
                if (!catalyst.isEmpty()) {
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, catalyst.copy()));
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, catalyst.copy()));
                }
            }
            default -> {}
        }
    }

    /** Undoes the Undertext's marks around a point: blanks, tears, faded ground and dead torches. */
    public static int purify(ServerLevel level, BlockPos center, int radius) {
        int n = 0;
        List<BlockPos> marks = WorldAlterations.scan(level, center, radius, radius / 2 + 2, s -> s.is(ModBlocks.FADED_GRASS_BLOCK.get())
                || s.is(ModBlocks.BLANK.get()) || s.is(ModBlocks.TEAR.get()) || s.is(ModBlocks.ERASED_GRASS.get()));
        for (BlockPos p : marks) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.FADED_GRASS_BLOCK.get())) level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            else if (s.is(ModBlocks.ERASED_GRASS.get()) && level.dimension() != ModDimensions.UNDERTEXT) level.setBlock(p, Blocks.GRASS.defaultBlockState(), 3);
            else if (s.is(ModBlocks.TEAR.get())) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                level.playSound(null, p, ModSounds.TEAR_SEAL.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            } else if (s.is(ModBlocks.BLANK.get()) && level.getBlockEntity(p) instanceof BlankBlockEntity blank) {
                level.setBlock(p, blank.getOriginal(), 3);
            }
            n++;
        }
        n += WorldAlterations.relight(level, center, radius);
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(center).inflate(radius))) p.removeEffect(ModEffects.ERASURE.get());
        return n;
    }

    public static void backlash(String type, ServerLevel level, BlockPos pos, ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.palimpsest.ritual.backlash." + type)
                .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.ITALIC), true);
        switch (type) {
            case "knocker" -> {
                BlockPos at = Spots.ground(level, player, 10, 16, 180, 90, 3, 16, p -> true);
                if (at != null) KnockerEntity.comeThrough(level, at, player);
            }
            case "blotlings" -> {
                for (int i = 0; i < 3; i++) {
                    BlotlingEntity b = ModEntities.BLOTLING.get().create(level);
                    if (b == null) continue;
                    b.setSize(2, true);
                    b.moveTo(pos.getX() + 0.5 + level.random.nextInt(5) - 2, pos.getY() + 1, pos.getZ() + 0.5 + level.random.nextInt(5) - 2, 0, 0);
                    level.addFreshEntity(b);
                }
            }
            case "longhand" -> {
                BlockPos at = Spots.ground(level, player, 18, 26, 180, 60, 4, 16, p -> true);
                if (at != null) LonghandEntity.spawnHunter(level, at, player);
            }
            case "longhand_watch" -> {
                BlockPos at = Spots.ground(level, player, 30, 45, 0, 90, 4, 16, p -> true);
                if (at != null) LonghandEntity.spawnWatcher(level, at, player);
            }
            case "fair_copy" -> {
                BlockPos at = Spots.ground(level, player, 12, 20, 180, 60, 2, 16, p -> true);
                if (at != null) FairCopyEntity.spawnFor(level, at, player);
            }
            case "bleed" -> BleedManager.add(player, 50F);
            case "lights_out" -> WorldAlterations.snuffTorches(level, player, 16, 16, true);
            case "tear" -> {
                if (level.dimension() != ModDimensions.UNDERTEXT) {
                    BlockPos at = Spots.ground(level, player, 8, 14, 0, 180, 2, 12, p -> true);
                    if (at != null) TearBlock.open(level, at);
                }
            }
            default -> {}
        }
    }

    private RitualLogic() {}
}
