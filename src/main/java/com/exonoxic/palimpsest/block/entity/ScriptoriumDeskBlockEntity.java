package com.exonoxic.palimpsest.block.entity;

import com.exonoxic.palimpsest.menu.ScriptoriumDeskMenu;
import com.exonoxic.palimpsest.recipe.TranscriptionRecipe;
import com.exonoxic.palimpsest.registry.ModBlockEntities;
import com.exonoxic.palimpsest.registry.ModRecipes;
import com.exonoxic.palimpsest.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * The Scriptorium Desk copies things out properly: a surface, an ink and a subject become
 * something new over a few seconds of careful work. Hopper-friendly: inputs from the top and
 * sides, output from below.
 */
public class ScriptoriumDeskBlockEntity extends BlockEntity implements MenuProvider {
    public static final int OUTPUT = 3;

    private final ItemStackHandler items = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot != OUTPUT;
        }
    };
    private LazyOptional<IItemHandler> inputs = LazyOptional.of(() -> new RangedWrapper(items, 0, 3));
    private LazyOptional<IItemHandler> output = LazyOptional.of(() -> new RangedWrapper(items, OUTPUT, OUTPUT + 1));
    private int progress;
    private int maxProgress = 200;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? progress : maxProgress;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
            else maxProgress = value;
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public ScriptoriumDeskBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCRIPTORIUM_DESK.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ScriptoriumDeskBlockEntity be) {
        Optional<TranscriptionRecipe> match = level.getRecipeManager()
                .getRecipeFor(ModRecipes.TRANSCRIPTION_TYPE.get(), new RecipeWrapper(be.items), level);
        if (match.isEmpty()) {
            if (be.progress != 0) {
                be.progress = 0;
                be.setChanged();
            }
            return;
        }
        TranscriptionRecipe recipe = match.get();
        ItemStack result = recipe.assemble(new RecipeWrapper(be.items), level.registryAccess());
        if (!be.items.insertItem(OUTPUT, result, true).isEmpty()) return;
        be.maxProgress = Math.max(1, recipe.getTime());
        be.progress++;
        if (be.progress % 40 == 1) {
            level.playSound(null, pos, ModSounds.QUILL_SCRATCH.get(), SoundSource.BLOCKS, 0.5F, 0.9F + level.random.nextFloat() * 0.2F);
        }
        if (be.progress >= be.maxProgress) {
            if (recipe.usesSurface()) be.items.extractItem(TranscriptionRecipe.SURFACE, 1, false);
            if (recipe.usesInk()) be.items.extractItem(TranscriptionRecipe.INK, 1, false);
            if (recipe.usesSubject()) be.items.extractItem(TranscriptionRecipe.SUBJECT, 1, false);
            be.items.insertItem(OUTPUT, result, false);
            be.progress = 0;
            level.playSound(null, pos, ModSounds.PAGE_TURN.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        be.setChanged();
    }

    public void dropContents() {
        if (level == null) return;
        SimpleContainer c = new SimpleContainer(items.getSlots());
        for (int i = 0; i < items.getSlots(); i++) c.setItem(i, items.getStackInSlot(i));
        Containers.dropContents(level, worldPosition, c);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.palimpsest.scriptorium_desk");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ScriptoriumDeskMenu(id, inventory, this, data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return side == Direction.DOWN ? output.cast() : inputs.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inputs.invalidate();
        output.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        inputs = LazyOptional.of(() -> new RangedWrapper(items, 0, 3));
        output = LazyOptional.of(() -> new RangedWrapper(items, OUTPUT, OUTPUT + 1));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Items"));
        progress = tag.getInt("Progress");
        maxProgress = Math.max(1, tag.getInt("MaxProgress"));
    }
}
