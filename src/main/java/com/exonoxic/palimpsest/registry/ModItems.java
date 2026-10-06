package com.exonoxic.palimpsest.registry;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.item.*;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Palimpsest.MODID);

    // ------------------------------------------------------------------ materials
    public static final RegistryObject<Item> VELLUM_SCRAP = lore("vellum_scrap", new Item.Properties());
    public static final RegistryObject<Item> BLANK_VELLUM = lore("blank_vellum", new Item.Properties());
    public static final RegistryObject<Item> OAK_GALL = lore("oak_gall", new Item.Properties());
    public static final RegistryObject<Item> IRON_GALL_INK = lore("iron_gall_ink", new Item.Properties().stacksTo(16));
    public static final RegistryObject<Item> CINNABAR = lore("cinnabar", new Item.Properties());
    public static final RegistryObject<Item> VERMILION = lore("vermilion", new Item.Properties());
    public static final RegistryObject<Item> UNDERTEXT_FRAGMENT = lore("undertext_fragment", new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final RegistryObject<Item> LAMPBLACK = lore("lampblack", new Item.Properties());
    public static final RegistryObject<Item> RAW_ILLUMINE = lore("raw_illumine", new Item.Properties());
    public static final RegistryObject<Item> ILLUMINE_LEAF = lore("illumine_leaf", new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final RegistryObject<Item> GALL_STEEPED_IRON = lore("gall_steeped_iron", new Item.Properties());
    public static final RegistryObject<Item> GALL_IRON_INGOT = lore("gall_iron_ingot", new Item.Properties());
    public static final RegistryObject<Item> FOXED_DUST = lore("foxed_dust", new Item.Properties());
    public static final RegistryObject<Item> BLACK_QUILL = lore("black_quill", new Item.Properties());
    public static final RegistryObject<Item> PALE_ANTLER = lore("pale_antler", new Item.Properties());
    public static final RegistryObject<Item> BLOT_RESIDUE = lore("blot_residue", new Item.Properties());
    public static final RegistryObject<Item> REDACTION_STRIP = lore("redaction_strip", new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final RegistryObject<Item> BOOKBINDER_NEEDLE = lore("bookbinder_needle", new Item.Properties().rarity(Rarity.RARE));
    public static final RegistryObject<Item> RASURE_SHARD = lore("rasure_shard", new Item.Properties().rarity(Rarity.EPIC).fireResistant());

    // ------------------------------------------------------------------ keys & lore
    public static final RegistryObject<Item> SPINE_KEY = lore("spine_key", new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
    public static final RegistryObject<Item> TORN_FOLIO = ITEMS.register("torn_folio", () -> new TornFolioItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FADED_PAGE = ITEMS.register("faded_page", () -> new FadedPageItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> LAST_FOLIO = ITEMS.register("last_folio", () -> new LastFolioItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> COMMONPLACE_BOOK = ITEMS.register("commonplace_book", () -> new CommonplaceBookItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MISPRINT = ITEMS.register("misprint", () -> new MisprintItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // ------------------------------------------------------------------ tools
    public static final RegistryObject<Item> IRON_RASORIUM = ITEMS.register("iron_rasorium",
            () -> new RasoriumItem(Tiers.IRON, 1, -1.6F, 4.0F, new Item.Properties()));
    public static final RegistryObject<Item> RASURE_EDGE = ITEMS.register("rasure_edge",
            () -> new RasureEdgeItem(ModTiers.RASURE, 4, -2.2F, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> READING_LENS = ITEMS.register("reading_lens",
            () -> new ReadingLensItem(new Item.Properties().durability(96)));
    public static final RegistryObject<Item> MARGIN_COMPASS = ITEMS.register("margin_compass",
            () -> new MarginCompassItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DOWSING_QUILL = ITEMS.register("dowsing_quill",
            () -> new DowsingQuillItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BOOKMARK = ITEMS.register("bookmark",
            () -> new BookmarkItem(new Item.Properties().stacksTo(8).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> FOLIO_OF_DESCENT = ITEMS.register("folio_of_descent",
            () -> new FolioOfDescentItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> SEAL_OF_CLOSING = ITEMS.register("seal_of_closing",
            () -> new SealOfClosingItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> INK_BOMB = ITEMS.register("ink_bomb",
            () -> new InkBombItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ILLUMINED_CENSER = ITEMS.register("illumined_censer",
            () -> new IllumineCenserItem(new Item.Properties().durability(240).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> SCRIVENERS_QUILL = ITEMS.register("scriveners_quill",
            () -> new ScrivenersQuillItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> RUBRIC_CHALK = ITEMS.register("rubric_chalk",
            () -> new RubricChalkItem(ModBlocks.RUBRIC_CHALK.get(), new Item.Properties().durability(64)));

    public static final RegistryObject<Item> GALL_IRON_SWORD = ITEMS.register("gall_iron_sword",
            () -> new SwordItem(ModTiers.GALL_IRON, 3, -2.4F, new Item.Properties()));
    public static final RegistryObject<Item> GALL_IRON_PICKAXE = ITEMS.register("gall_iron_pickaxe",
            () -> new PickaxeItem(ModTiers.GALL_IRON, 1, -2.8F, new Item.Properties()));
    public static final RegistryObject<Item> GALL_IRON_AXE = ITEMS.register("gall_iron_axe",
            () -> new AxeItem(ModTiers.GALL_IRON, 6.0F, -3.1F, new Item.Properties()));

    // ------------------------------------------------------------------ armour
    public static final RegistryObject<Item> RUBRIC_HOOD = ITEMS.register("rubric_hood",
            () -> new RubricArmorItem(ModArmorMaterials.RUBRIC, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> RUBRIC_ROBE = ITEMS.register("rubric_robe",
            () -> new RubricArmorItem(ModArmorMaterials.RUBRIC, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> RUBRIC_LEGGINGS = ITEMS.register("rubric_leggings",
            () -> new RubricArmorItem(ModArmorMaterials.RUBRIC, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> RUBRIC_BOOTS = ITEMS.register("rubric_boots",
            () -> new RubricArmorItem(ModArmorMaterials.RUBRIC, ArmorItem.Type.BOOTS, new Item.Properties()));
    public static final RegistryObject<Item> CIRCLET = ITEMS.register("circlet_of_the_first_draft",
            () -> new CircletItem(ModArmorMaterials.FIRST_DRAFT, ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    // ------------------------------------------------------------------ food & remedies
    public static final RegistryObject<Item> BLOTBERRIES = ITEMS.register("blotberries",
            () -> new BlotberriesItem(ModBlocks.BLOTBERRY_BUSH.get(), new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(2).saturationMod(0.1F).fast().build())));
    public static final RegistryObject<Item> BLOTBERRY_TART = lore("blotberry_tart", new Item.Properties().food(new FoodProperties.Builder()
            .nutrition(8).saturationMod(0.6F).build()));
    public static final RegistryObject<Item> CANDLEWORT_TEA = ITEMS.register("candlewort_tea",
            () -> new RemedyItem(new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE), RemedyItem.Kind.TEA));
    public static final RegistryObject<Item> SEALING_WAX = ITEMS.register("sealing_wax",
            () -> new RemedyItem(new Item.Properties().stacksTo(16), RemedyItem.Kind.WAX));
    public static final RegistryObject<Item> PALE_VENISON = lore("pale_venison", new Item.Properties().food(new FoodProperties.Builder()
            .nutrition(3).saturationMod(0.3F).meat()
            .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 200, 0), 0.4F).build()));
    public static final RegistryObject<Item> COOKED_PALE_VENISON = lore("cooked_pale_venison", new Item.Properties().food(new FoodProperties.Builder()
            .nutrition(7).saturationMod(0.8F).meat().build()));
    public static final RegistryObject<Item> SCRIBES_RATION = lore("scribes_ration", new Item.Properties().food(new FoodProperties.Builder()
            .nutrition(9).saturationMod(0.9F).build()));

    public static final RegistryObject<Item> MUSIC_DISC_LOWER_WRITING = ITEMS.register("music_disc_lower_writing",
            () -> new RecordItem(13, ModSounds.MUSIC_DISC_LOWER_WRITING, new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 2280));

    // ------------------------------------------------------------------ special block items
    public static final RegistryObject<Item> SPENT_TORCH = ITEMS.register("spent_torch",
            () -> new StandingAndWallBlockItem(ModBlocks.SPENT_TORCH.get(), ModBlocks.SPENT_WALL_TORCH.get(), new Item.Properties(), Direction.DOWN));
    public static final RegistryObject<Item> BLOTWOOD_DOOR = ITEMS.register("blotwood_door",
            () -> new DoubleHighBlockItem(ModBlocks.BLOTWOOD_DOOR.get(), new Item.Properties()));

    // ------------------------------------------------------------------ spawn eggs
    public static final RegistryObject<Item> FOXING_MOTH_EGG = egg("foxing_moth", ModEntities.FOXING_MOTH, 0xC9B89A, 0x6B4A2E);
    public static final RegistryObject<Item> BLOTLING_EGG = egg("blotling", ModEntities.BLOTLING, 0x15131A, 0x3B3550);
    public static final RegistryObject<Item> SMUDGE_EGG = egg("smudge", ModEntities.SMUDGE, 0xA9A294, 0x4D4A45);
    public static final RegistryObject<Item> MARGIN_CRAWLER_EGG = egg("margin_crawler", ModEntities.MARGIN_CRAWLER, 0xD8CBA8, 0x8C1C13);
    public static final RegistryObject<Item> QUILLCROW_EGG = egg("quillcrow", ModEntities.QUILLCROW, 0x1B1A22, 0x6F7887);
    public static final RegistryObject<Item> PALE_STAG_EGG = egg("pale_stag", ModEntities.PALE_STAG, 0xE4E0D6, 0x25222A);
    public static final RegistryObject<Item> INKHOUND_EGG = egg("inkhound", ModEntities.INKHOUND, 0x0E0D12, 0x5A1010);
    public static final RegistryObject<Item> RUBRICATOR_EGG = egg("rubricator", ModEntities.RUBRICATOR, 0x6E1A14, 0xD8CBA8);
    public static final RegistryObject<Item> KNOCKER_EGG = egg("knocker", ModEntities.KNOCKER, 0x3A3530, 0xCFC6B4);
    public static final RegistryObject<Item> COPYIST_EGG = egg("copyist", ModEntities.COPYIST, 0x8A6F5C, 0xE0A8A0);
    public static final RegistryObject<Item> LONGHAND_EGG = egg("longhand", ModEntities.LONGHAND, 0x0B0B0F, 0xF0EDE4);
    public static final RegistryObject<Item> REDACTED_EGG = egg("redacted", ModEntities.REDACTED, 0xBDB6A6, 0x050505);
    public static final RegistryObject<Item> FAIR_COPY_EGG = egg("fair_copy", ModEntities.FAIR_COPY, 0x3AAFA9, 0xF9FFFE);
    public static final RegistryObject<Item> ERRATUM_EGG = egg("erratum", ModEntities.ERRATUM, 0x7F7F7F, 0xFF00FF);

    private static RegistryObject<Item> lore(String name, Item.Properties props) {
        return ITEMS.register(name, () -> new LoreItem(props));
    }

    private static RegistryObject<Item> egg(String name, Supplier<? extends EntityType<? extends Mob>> type, int bg, int fg) {
        return ITEMS.register(name + "_spawn_egg", () -> new ForgeSpawnEggItem(type, bg, fg, new Item.Properties()));
    }

    private ModItems() {}
}
