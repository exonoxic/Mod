package com.exonoxic.palimpsest.entity;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.entity.ai.Pursuit;
import com.exonoxic.palimpsest.entity.boss.BookbinderEntity;
import com.exonoxic.palimpsest.entity.boss.RasureEntity;
import com.exonoxic.palimpsest.registry.ModEntities;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Attributes and natural-spawn rules. Which biomes spawn what lives in the biome modifiers. */
@Mod.EventBusSubscriber(modid = Palimpsest.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntityEvents {

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.FOXING_MOTH.get(), FoxingMothEntity.createAttributes().build());
        event.put(ModEntities.BLOTLING.get(), Monster.createMonsterAttributes().build());
        event.put(ModEntities.SMUDGE.get(), SmudgeEntity.createAttributes().build());
        event.put(ModEntities.MARGIN_CRAWLER.get(), Pursuit.attributes(MarginCrawlerEntity.createAttributes()).build());
        event.put(ModEntities.QUILLCROW.get(), QuillcrowEntity.createAttributes().build());
        event.put(ModEntities.PALE_STAG.get(), PaleStagEntity.createAttributes().build());
        event.put(ModEntities.INKHOUND.get(), Pursuit.attributes(InkhoundEntity.createAttributes()).build());
        event.put(ModEntities.RUBRICATOR.get(), RubricatorEntity.createAttributes().build());
        event.put(ModEntities.KNOCKER.get(), Pursuit.attributes(KnockerEntity.createAttributes()).build());
        event.put(ModEntities.COPYIST.get(), Pursuit.attributes(CopyistEntity.createAttributes()).build());
        event.put(ModEntities.LONGHAND.get(), Pursuit.attributes(LonghandEntity.createAttributes()).build());
        event.put(ModEntities.REDACTED.get(), Pursuit.attributes(RedactedEntity.createAttributes()).build());
        event.put(ModEntities.FAIR_COPY.get(), FairCopyEntity.createAttributes().build());
        event.put(ModEntities.ERRATUM.get(), ErratumEntity.createAttributes().build());
        event.put(ModEntities.PALEHAND.get(), PalehandEntity.createAttributes().build());
        event.put(ModEntities.BOOKBINDER.get(), Pursuit.attributes(BookbinderEntity.createAttributes()).build());
        event.put(ModEntities.RASURE.get(), Pursuit.attributes(RasureEntity.createAttributes()).build());
    }

    @SubscribeEvent
    public static void spawnPlacements(SpawnPlacementRegisterEvent event) {
        var op = SpawnPlacementRegisterEvent.Operation.REPLACE;
        var ground = SpawnPlacements.Type.ON_GROUND;
        var heightmap = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        event.register(ModEntities.FOXING_MOTH.get(), SpawnPlacements.Type.NO_RESTRICTIONS, heightmap, FoxingMothEntity::checkSpawn, op);
        event.register(ModEntities.BLOTLING.get(), ground, heightmap, BlotlingEntity::checkSpawn, op);
        event.register(ModEntities.SMUDGE.get(), ground, heightmap, Mob::checkMobSpawnRules, op);
        event.register(ModEntities.MARGIN_CRAWLER.get(), ground, heightmap, MarginCrawlerEntity::checkSpawn, op);
        event.register(ModEntities.QUILLCROW.get(), ground, heightmap, Mob::checkMobSpawnRules, op);
        event.register(ModEntities.PALE_STAG.get(), ground, heightmap, Mob::checkMobSpawnRules, op);
        event.register(ModEntities.INKHOUND.get(), ground, heightmap, InkhoundEntity::checkSpawn, op);
        event.register(ModEntities.RUBRICATOR.get(), ground, heightmap, Mob::checkMobSpawnRules, op);
        event.register(ModEntities.COPYIST.get(), ground, heightmap, CopyistEntity::checkSpawn, op);
        event.register(ModEntities.REDACTED.get(), ground, heightmap, RedactedEntity::checkSpawn, op);
    }

    private ModEntityEvents() {}
}
