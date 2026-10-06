package com.exonoxic.palimpsest.menu;

import com.exonoxic.palimpsest.block.entity.ScriptoriumDeskBlockEntity;
import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModMenus;
import com.exonoxic.palimpsest.registry.ModTags;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class ScriptoriumDeskMenu extends AbstractContainerMenu {
    private static final int DESK_SLOTS = 4;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client side: the block entity is found from the position sent by the server. */
    public ScriptoriumDeskMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, resolve(inventory, buf), new SimpleContainerData(2));
    }

    public ScriptoriumDeskMenu(int id, Inventory inventory, ScriptoriumDeskBlockEntity desk, ContainerData data) {
        super(ModMenus.SCRIPTORIUM_DESK.get(), id);
        this.data = data;
        this.access = desk.getLevel() != null ? ContainerLevelAccess.create(desk.getLevel(), desk.getBlockPos()) : ContainerLevelAccess.NULL;
        IItemHandler items = desk.getItems();
        addSlot(new SlotItemHandler(items, 0, 38, 17));
        addSlot(new SlotItemHandler(items, 1, 38, 53));
        addSlot(new SlotItemHandler(items, 2, 62, 35));
        addSlot(new SlotItemHandler(items, 3, 124, 35) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        addDataSlots(data);
    }

    private static ScriptoriumDeskBlockEntity resolve(Inventory inventory, FriendlyByteBuf buf) {
        BlockEntity be = inventory.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof ScriptoriumDeskBlockEntity desk) return desk;
        // Should never happen; give the client an inert desk so the screen can still close cleanly.
        return new ScriptoriumDeskBlockEntity(inventory.player.blockPosition(), ModBlocks.SCRIPTORIUM_DESK.get().defaultBlockState()) {
            private final ItemStackHandler empty = new ItemStackHandler(DESK_SLOTS);

            @Override
            public ItemStackHandler getItems() {
                return empty;
            }
        };
    }

    public float progress() {
        int max = data.get(1);
        return max <= 0 ? 0F : Math.min(1F, data.get(0) / (float) max);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int playerStart = DESK_SLOTS;
        int playerEnd = slots.size();
        if (index < DESK_SLOTS) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);
        } else {
            int target = stack.is(ModTags.SURFACES) ? 0 : stack.is(ModTags.INKS) ? 1 : 2;
            if (!moveItemStackTo(stack, target, target + 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.SCRIPTORIUM_DESK.get());
    }
}
