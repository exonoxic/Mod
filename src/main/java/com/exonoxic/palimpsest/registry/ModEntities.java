package com.exonoxic.palimpsest.registry;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.entity.*;
import com.exonoxic.palimpsest.entity.boss.BookbinderEntity;
import com.exonoxic.palimpsest.entity.boss.RasureEntity;
import com.exonoxic.palimpsest.entity.projectile.BindingNeedleEntity;
import com.exonoxic.palimpsest.entity.projectile.InkBombEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Palimpsest.MODID);

    // Ordinary inhabitants
    public static final RegistryObject<EntityType<FoxingMothEntity>> FOXING_MOTH = ENTITIES.register("foxing_moth",
            () -> EntityType.Builder.of(FoxingMothEntity::new, MobCategory.AMBIENT).sized(0.45F, 0.35F).clientTrackingRange(5).build("foxing_moth"));
    public static final RegistryObject<EntityType<BlotlingEntity>> BLOTLING = ENTITIES.register("blotling",
            () -> EntityType.Builder.of(BlotlingEntity::new, MobCategory.MONSTER).sized(2.04F, 2.04F).clientTrackingRange(8).build("blotling"));
    public static final RegistryObject<EntityType<SmudgeEntity>> SMUDGE = ENTITIES.register("smudge",
            () -> EntityType.Builder.of(SmudgeEntity::new, MobCategory.CREATURE).sized(0.6F, 1.85F).clientTrackingRange(10).build("smudge"));
    public static final RegistryObject<EntityType<MarginCrawlerEntity>> MARGIN_CRAWLER = ENTITIES.register("margin_crawler",
            () -> EntityType.Builder.of(MarginCrawlerEntity::new, MobCategory.MONSTER).sized(0.6F, 0.5F).clientTrackingRange(8).build("margin_crawler"));
    public static final RegistryObject<EntityType<QuillcrowEntity>> QUILLCROW = ENTITIES.register("quillcrow",
            () -> EntityType.Builder.of(QuillcrowEntity::new, MobCategory.CREATURE).sized(0.5F, 0.6F).clientTrackingRange(10).build("quillcrow"));
    public static final RegistryObject<EntityType<PaleStagEntity>> PALE_STAG = ENTITIES.register("pale_stag",
            () -> EntityType.Builder.of(PaleStagEntity::new, MobCategory.CREATURE).sized(0.9F, 1.7F).clientTrackingRange(10).build("pale_stag"));
    public static final RegistryObject<EntityType<InkhoundEntity>> INKHOUND = ENTITIES.register("inkhound",
            () -> EntityType.Builder.of(InkhoundEntity::new, MobCategory.MONSTER).sized(0.8F, 0.9F).clientTrackingRange(10).build("inkhound"));
    public static final RegistryObject<EntityType<RubricatorEntity>> RUBRICATOR = ENTITIES.register("rubricator",
            () -> EntityType.Builder.of(RubricatorEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10).build("rubricator"));

    // Major threats
    public static final RegistryObject<EntityType<KnockerEntity>> KNOCKER = ENTITIES.register("knocker",
            () -> EntityType.Builder.of(KnockerEntity::new, MobCategory.MONSTER).sized(0.7F, 2.7F).clientTrackingRange(10).build("knocker"));
    public static final RegistryObject<EntityType<CopyistEntity>> COPYIST = ENTITIES.register("copyist",
            () -> EntityType.Builder.of(CopyistEntity::new, MobCategory.MONSTER).sized(0.9F, 1.4F).clientTrackingRange(10).build("copyist"));
    public static final RegistryObject<EntityType<LonghandEntity>> LONGHAND = ENTITIES.register("longhand",
            () -> EntityType.Builder.of(LonghandEntity::new, MobCategory.MONSTER).sized(0.8F, 3.6F).clientTrackingRange(12).build("longhand"));
    public static final RegistryObject<EntityType<RedactedEntity>> REDACTED = ENTITIES.register("redacted",
            () -> EntityType.Builder.of(RedactedEntity::new, MobCategory.MONSTER).sized(0.6F, 2.0F).clientTrackingRange(10).build("redacted"));

    // Very rare
    public static final RegistryObject<EntityType<FairCopyEntity>> FAIR_COPY = ENTITIES.register("fair_copy",
            () -> EntityType.Builder.of(FairCopyEntity::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(12).build("fair_copy"));
    public static final RegistryObject<EntityType<ErratumEntity>> ERRATUM = ENTITIES.register("erratum",
            () -> EntityType.Builder.of(ErratumEntity::new, MobCategory.MISC).sized(0.98F, 0.98F).clientTrackingRange(8).build("erratum"));
    public static final RegistryObject<EntityType<PalehandEntity>> PALEHAND = ENTITIES.register("palehand",
            () -> EntityType.Builder.of(PalehandEntity::new, MobCategory.MISC).sized(4.0F, 30.0F).fireImmune().clientTrackingRange(16).build("palehand"));

    // Bosses
    public static final RegistryObject<EntityType<BookbinderEntity>> BOOKBINDER = ENTITIES.register("bookbinder",
            () -> EntityType.Builder.of(BookbinderEntity::new, MobCategory.MONSTER).sized(2.0F, 2.4F).fireImmune().clientTrackingRange(10).build("bookbinder"));
    public static final RegistryObject<EntityType<RasureEntity>> RASURE = ENTITIES.register("rasure",
            () -> EntityType.Builder.of(RasureEntity::new, MobCategory.MONSTER).sized(1.4F, 4.0F).fireImmune().clientTrackingRange(12).build("rasure"));

    // Projectiles
    public static final RegistryObject<EntityType<InkBombEntity>> INK_BOMB = ENTITIES.register("ink_bomb",
            () -> EntityType.Builder.<InkBombEntity>of(InkBombEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build("ink_bomb"));
    public static final RegistryObject<EntityType<BindingNeedleEntity>> BINDING_NEEDLE = ENTITIES.register("binding_needle",
            () -> EntityType.Builder.<BindingNeedleEntity>of(BindingNeedleEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(5).build("binding_needle"));

    private ModEntities() {}
}
