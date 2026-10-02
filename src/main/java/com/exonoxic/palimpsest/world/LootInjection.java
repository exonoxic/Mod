package com.exonoxic.palimpsest.world;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.SetNbtFunction;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Threads the mod into a handful of vanilla loot tables, so that the first traces can be found
 * without leaving the ordinary world: galls on oak leaves, and a rare folio or fragment in
 * places a curious player already looks.
 */
@Mod.EventBusSubscriber(modid = Palimpsest.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LootInjection {
    private LootInjection() {}

    @SubscribeEvent
    public static void onLootLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        if (!"minecraft".equals(name.getNamespace())) return;
        switch (name.getPath()) {
            case "blocks/oak_leaves", "blocks/dark_oak_leaves" -> {
                LootPool pool = LootPool.lootPool().name("palimpsest:oak_gall")
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(ModItems.OAK_GALL.get()))
                        .when(LootItemRandomChanceCondition.randomChance(0.03F))
                        .when(ExplosionCondition.survivesExplosion())
                        .build();
                event.getTable().addPool(pool);
            }
            case "chests/village/village_cartographer", "chests/stronghold_library", "chests/shipwreck_map" ->
                    event.getTable().addPool(LootPool.lootPool().name("palimpsest:folios")
                            .setRolls(ConstantValue.exactly(1))
                            .add(folio(1, 3)).add(folio(2, 2))
                            .when(LootItemRandomChanceCondition.randomChance(0.35F))
                            .build());
            case "chests/simple_dungeon", "chests/abandoned_mineshaft", "chests/ancient_city" ->
                    event.getTable().addPool(LootPool.lootPool().name("palimpsest:fragments")
                            .setRolls(ConstantValue.exactly(1))
                            .add(item(ModItems.UNDERTEXT_FRAGMENT.get(), 3))
                            .add(item(ModItems.OAK_GALL.get(), 4))
                            .add(folio(3, 1))
                            .when(LootItemRandomChanceCondition.randomChance(0.25F))
                            .build());
            default -> {}
        }
    }

    private static LootPoolEntryContainer.Builder<?> item(Item item, int weight) {
        return LootItem.lootTableItem(item).setWeight(weight);
    }

    private static LootPoolEntryContainer.Builder<?> folio(int n, int weight) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Folio", n);
        return LootItem.lootTableItem(ModItems.TORN_FOLIO.get()).setWeight(weight).apply(SetNbtFunction.setTag(tag));
    }
}
