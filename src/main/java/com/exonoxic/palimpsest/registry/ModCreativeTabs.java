package com.exonoxic.palimpsest.registry;

import com.exonoxic.palimpsest.Palimpsest;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Palimpsest.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.palimpsest"))
            .icon(() -> new ItemStack(ModItems.FOLIO_OF_DESCENT.get()))
            .displayItems((params, output) -> ModItems.ITEMS.getEntries().forEach(ro -> output.accept(ro.get())))
            .build());

    private ModCreativeTabs() {}
}
