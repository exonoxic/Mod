package com.exonoxic.palimpsest.bleed;

import com.exonoxic.palimpsest.Palimpsest;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BleedCapability {
    public static final Capability<BleedData> BLEED = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation KEY = Palimpsest.id("bleed");

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(BleedData.class);
    }

    /** Never null for a live player; returns a throwaway instance if the capability is missing. */
    public static BleedData get(Player player) {
        return player.getCapability(BLEED).orElseGet(BleedData::new);
    }

    @Nullable
    public static BleedData getOrNull(Player player) {
        return player.getCapability(BLEED).resolve().orElse(null);
    }

    public static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final BleedData data = new BleedData();
        private final LazyOptional<BleedData> optional = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return BLEED.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.save();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.load(nbt);
        }

        public void invalidate() {
            optional.invalidate();
        }
    }

    private BleedCapability() {}
}
