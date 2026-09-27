package com.exonoxic.palimpsest;

import com.exonoxic.palimpsest.bleed.BleedCapability;
import com.exonoxic.palimpsest.config.ClientConfig;
import com.exonoxic.palimpsest.config.CommonConfig;
import com.exonoxic.palimpsest.network.PacketHandler;
import com.exonoxic.palimpsest.registry.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Palimpsest — "This world was written over another."
 *
 * <p>Entry point. Everything is registered through DeferredRegisters in the {@code registry}
 * package; gameplay systems subscribe themselves to the Forge event bus through
 * {@code @Mod.EventBusSubscriber} annotations.</p>
 */
@Mod(Palimpsest.MODID)
public final class Palimpsest {
    public static final String MODID = "palimpsest";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Palimpsest() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModSounds.SOUNDS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModRecipes.TYPES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModFeatures.FEATURES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModBiomeModifiers.SERIALIZERS.register(modBus);
        ModPoiTypes.POI_TYPES.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC, "palimpsest-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, "palimpsest-client.toml");

        modBus.addListener(this::commonSetup);
        modBus.addListener(BleedCapability::register);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            PacketHandler.init();
            ModBlocks.registerFlammability();
        });
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
