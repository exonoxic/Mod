package com.exonoxic.palimpsest.block.entity;

import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.block.RubricAltarBlock;
import com.exonoxic.palimpsest.config.CommonConfig;
import com.exonoxic.palimpsest.recipe.RitualLogic;
import com.exonoxic.palimpsest.recipe.RitualRecipe;
import com.exonoxic.palimpsest.registry.ModBlockEntities;
import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModRecipes;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.util.Advancements;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runs a rite: checks the offerings on nearby Reading Stands against every ritual recipe,
 * checks the conditions, takes the offerings, then reads for {@code duration} ticks before
 * producing the result, applying the effect, charging Bleed and rolling for backlash.
 */
public class RubricAltarBlockEntity extends BlockEntity {
    public static final int STAND_RADIUS = 3;

    @Nullable
    private ResourceLocation active;
    private int timer;
    @Nullable
    private UUID activator;
    private ItemStack catalyst = ItemStack.EMPTY;
    private final List<BlockPos> stands = new ArrayList<>();

    public RubricAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUBRIC_ALTAR.get(), pos, state);
    }

    public List<ReadingStandBlockEntity> findStands() {
        List<ReadingStandBlockEntity> out = new ArrayList<>();
        if (level == null) return out;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-STAND_RADIUS, -1, -STAND_RADIUS), worldPosition.offset(STAND_RADIUS, 1, STAND_RADIUS))) {
            if (level.getBlockEntity(p) instanceof ReadingStandBlockEntity stand) out.add(stand);
        }
        return out;
    }

    public InteractionResult tryBegin(ServerPlayer player, InteractionHand hand) {
        ServerLevel level = (ServerLevel) this.level;
        if (level == null) return InteractionResult.PASS;
        if (active != null) {
            tell(player, "message.palimpsest.ritual.busy");
            return InteractionResult.CONSUME;
        }
        List<ReadingStandBlockEntity> standEntities = findStands();
        List<ItemStack> offerings = new ArrayList<>();
        for (ReadingStandBlockEntity s : standEntities) offerings.add(s.getItem());
        ItemStack held = player.getItemInHand(hand);
        boolean anyOffering = offerings.stream().anyMatch(s -> !s.isEmpty());

        for (RitualRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.RITUAL_TYPE.get())) {
            if (!recipe.matchesOfferings(offerings) || !recipe.matchesCatalyst(held)) continue;
            Component problem = RitualLogic.checkConditions(recipe, level, worldPosition, player);
            if (problem != null) {
                player.displayClientMessage(problem.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                level.playSound(null, worldPosition, ModSounds.RITUAL_FAIL.get(), SoundSource.BLOCKS, 0.6F, 1.2F);
                return InteractionResult.CONSUME;
            }
            begin(level, recipe, player, held, standEntities);
            return InteractionResult.CONSUME;
        }
        if (anyOffering || !held.isEmpty()) {
            tell(player, "message.palimpsest.ritual.no_sentence");
            level.playSound(null, worldPosition, ModSounds.RITUAL_FAIL.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
        }
        return InteractionResult.CONSUME;
    }

    private void begin(ServerLevel level, RitualRecipe recipe, ServerPlayer player, ItemStack held, List<ReadingStandBlockEntity> standEntities) {
        stands.clear();
        for (ReadingStandBlockEntity s : standEntities) {
            if (s.getItem().isEmpty()) continue;
            stands.add(s.getBlockPos());
            s.takeItem();
        }
        catalyst = held.copyWithCount(1);
        if (recipe.consumeCatalyst() && !player.getAbilities().instabuild) held.shrink(1);
        active = recipe.getId();
        timer = recipe.duration();
        activator = player.getUUID();
        level.setBlock(worldPosition, getBlockState().setValue(RubricAltarBlock.ACTIVE, true), 3);
        level.playSound(null, worldPosition, ModSounds.RITUAL_BEGIN.get(), SoundSource.BLOCKS, 1.2F, 1.0F);
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RubricAltarBlockEntity be) {
        if (be.active == null || !(level instanceof ServerLevel server)) return;
        be.timer--;
        if (be.timer % 4 == 0) {
            for (BlockPos s : be.stands) {
                double t = level.random.nextDouble();
                server.sendParticles(ModParticles.GLYPH.get(), s.getX() + 0.5 + (pos.getX() - s.getX()) * t, s.getY() + 1.1 + t * 0.3,
                        s.getZ() + 0.5 + (pos.getZ() - s.getZ()) * t, 1, 0.02, 0.02, 0.02, 0.0);
            }
            server.sendParticles(ModParticles.RUBRIC_SPARK.get(), pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.02);
        }
        if (be.timer <= 0) be.complete(server);
        be.setChanged();
    }

    private void complete(ServerLevel level) {
        ResourceLocation id = active;
        active = null;
        level.setBlock(worldPosition, getBlockState().setValue(RubricAltarBlock.ACTIVE, false), 3);
        if (id == null) return;
        Recipe<?> r = level.getRecipeManager().byKey(id).orElse(null);
        if (!(r instanceof RitualRecipe recipe)) return;

        ServerPlayer player = activator == null ? null : level.getServer().getPlayerList().getPlayer(activator);
        if (player == null) {
            Player nearest = level.getNearestPlayer(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5, 16, false);
            if (nearest instanceof ServerPlayer sp) player = sp;
        }

        if (!recipe.result().isEmpty()) {
            ItemEntity out = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, recipe.result().copy());
            out.setDeltaMovement(0, 0.15, 0);
            level.addFreshEntity(out);
        }
        RitualLogic.applyEffect(recipe.effect(), level, worldPosition, player, catalyst);
        level.playSound(null, worldPosition, ModSounds.RITUAL_COMPLETE.get(), SoundSource.BLOCKS, 1.2F, 1.0F);
        level.sendParticles(ModParticles.RUBRIC_SPARK.get(), worldPosition.getX() + 0.5, worldPosition.getY() + 1.3, worldPosition.getZ() + 0.5, 40, 0.5, 0.5, 0.5, 0.1);

        if (player != null) {
            BleedManager.add(player, recipe.bleed());
            BleedManager.unlock(player, "rites");
            Advancements.grant(player, "rubrication");
            int others = level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(8), p -> p.isAlive() && !p.isSpectator()).size() - 1;
            if (others > 0) Advancements.grant(player, "many_hands");
            double chance = recipe.backlashChance() - Math.max(0, others) * CommonConfig.COOPERATIVE_RITUAL_BONUS.get();
            if (!"none".equals(recipe.backlash()) && level.random.nextDouble() < chance) {
                RitualLogic.backlash(recipe.backlash(), level, worldPosition, player);
            }
        }
        catalyst = ItemStack.EMPTY;
        stands.clear();
        activator = null;
        setChanged();
    }

    private static void tell(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (active != null) {
            tag.putString("Active", active.toString());
            tag.putInt("Timer", timer);
            if (activator != null) tag.putUUID("Activator", activator);
            tag.put("Catalyst", catalyst.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        active = tag.contains("Active") ? ResourceLocation.tryParse(tag.getString("Active")) : null;
        timer = tag.getInt("Timer");
        activator = tag.hasUUID("Activator") ? tag.getUUID("Activator") : null;
        catalyst = tag.contains("Catalyst") ? ItemStack.of(tag.getCompound("Catalyst")) : ItemStack.EMPTY;
    }
}
