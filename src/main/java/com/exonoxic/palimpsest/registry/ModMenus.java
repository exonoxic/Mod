package com.exonoxic.palimpsest.registry;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.menu.ScriptoriumDeskMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Palimpsest.MODID);

    public static final RegistryObject<MenuType<ScriptoriumDeskMenu>> SCRIPTORIUM_DESK = MENUS.register("scriptorium_desk",
            () -> IForgeMenuType.create(ScriptoriumDeskMenu::new));

    private ModMenus() {}
}
