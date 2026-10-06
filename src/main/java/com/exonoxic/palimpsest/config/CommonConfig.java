package com.exonoxic.palimpsest.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server-authoritative settings. Only values that a player or server owner would
 * reasonably want to change are exposed; internal tuning stays in code.
 */
public final class CommonConfig {
    public static final ForgeConfigSpec SPEC;

    // General pacing
    public static final ForgeConfigSpec.DoubleValue HORROR_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue EVENT_FREQUENCY;
    public static final ForgeConfigSpec.DoubleValue BLEED_GAIN_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FINAL_STAGE;
    public static final ForgeConfigSpec.BooleanValue BLEED_DECAYS_WHILE_RESTING;
    public static final ForgeConfigSpec.IntValue GRACE_PERIOD_DAYS;

    // World
    public static final ForgeConfigSpec.BooleanValue ALLOW_WORLD_ALTERATION;
    public static final ForgeConfigSpec.BooleanValue ALLOW_TEARS;
    public static final ForgeConfigSpec.IntValue MAX_TEARS_PER_PLAYER;
    public static final ForgeConfigSpec.BooleanValue ALLOW_SCRAPING;
    public static final ForgeConfigSpec.BooleanValue ALLOW_GATES;

    // World generation
    public static final ForgeConfigSpec.BooleanValue GENERATE_ORES;
    public static final ForgeConfigSpec.BooleanValue GENERATE_SCARS;
    public static final ForgeConfigSpec.BooleanValue GENERATE_PLANTS;

    // Creatures
    public static final ForgeConfigSpec.BooleanValue ENABLE_KNOCKER;
    public static final ForgeConfigSpec.BooleanValue KNOCKER_BREAKS_DOORS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_COPYIST;
    public static final ForgeConfigSpec.BooleanValue ENABLE_LONGHAND;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FAIR_COPY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ERRATUM;
    public static final ForgeConfigSpec.BooleanValue REDACTED_RENAMES_ITEMS;
    public static final ForgeConfigSpec.DoubleValue OVERWORLD_SPAWN_MULTIPLIER;

    // Multiplayer
    public static final ForgeConfigSpec.BooleanValue SHARED_APPARITIONS;
    public static final ForgeConfigSpec.BooleanValue FAKE_CHAT_MESSAGES;
    public static final ForgeConfigSpec.DoubleValue COOPERATIVE_RITUAL_BONUS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("How the horror paces itself.").push("pacing");
        HORROR_INTENSITY = b.comment("Scales how strong events are once they happen (more blocks changed, closer apparitions, louder sounds).",
                        "0 disables every event but keeps creatures and the dimension.")
                .defineInRange("horrorIntensity", 1.0D, 0.0D, 2.0D);
        EVENT_FREQUENCY = b.comment("Multiplier on how often the Director considers firing an event.")
                .defineInRange("eventFrequency", 1.0D, 0.1D, 4.0D);
        BLEED_GAIN_MULTIPLIER = b.comment("Multiplier on every source of Bleed. Lower values make the descent slower.")
                .defineInRange("bleedGainMultiplier", 1.0D, 0.0D, 5.0D);
        ENABLE_FINAL_STAGE = b.comment("Allow players to reach the final stage. When false, Bleed is capped just below it.")
                .define("enableFinalStage", true);
        BLEED_DECAYS_WHILE_RESTING = b.comment("If true, a full night's sleep near a Rubric Ward slowly lowers Bleed (recommended).")
                .define("bleedDecaysWhileResting", true);
        GRACE_PERIOD_DAYS = b.comment("In-game days after a player first joins during which nothing strange happens at all.")
                .defineInRange("gracePeriodDays", 1, 0, 30);
        b.pop();

        b.comment("Things the mod may do to the world around players.").push("world");
        ALLOW_WORLD_ALTERATION = b.comment("Allow events to snuff torches, open doors, alter signs (temporarily), shuffle chests and fade grass.")
                .define("allowWorldAlteration", true);
        ALLOW_TEARS = b.comment("Allow Tears (rifts that leak creatures) to open near players at high Bleed.")
                .define("allowTears", true);
        MAX_TEARS_PER_PLAYER = b.defineInRange("maxTearsPerPlayer", 3, 0, 16);
        ALLOW_SCRAPING = b.comment("Allow the final stage to temporarily scrape blocks near players. Scraped blocks always restore themselves.")
                .define("allowScraping", true);
        ALLOW_GATES = b.comment("Allow Folio Gates (the way into the Undertext) to be opened.")
                .define("allowGates", true);
        b.pop();

        b.comment("Overworld generation. Structures can be tuned with a datapack (see README).").push("worldgen");
        GENERATE_ORES = b.define("generateOres", true);
        GENERATE_SCARS = b.comment("Erasure scars: small, rare patches of scraped ground.").define("generateScars", true);
        GENERATE_PLANTS = b.define("generatePlants", true);
        b.pop();

        b.comment("Creatures.").push("creatures");
        ENABLE_KNOCKER = b.define("enableKnocker", true);
        KNOCKER_BREAKS_DOORS = b.comment("Whether a Knocker that finds no way in may slowly break a wooden door down while nobody is looking at it"
                + " (also needs the mobGriefing game rule). If not, it only ever hides and waits.").define("knockerBreaksDoors", true);
        ENABLE_COPYIST = b.define("enableCopyist", true);
        ENABLE_LONGHAND = b.define("enableLonghand", true);
        ENABLE_FAIR_COPY = b.define("enableFairCopy", true);
        ENABLE_ERRATUM = b.define("enableErratum", true);
        REDACTED_RENAMES_ITEMS = b.comment("Whether a Redacted's strike can black out the name of an item in your hotbar (cosmetic; rename it at an anvil).")
                .define("redactedRenamesItems", true);
        OVERWORLD_SPAWN_MULTIPLIER = b.comment("Multiplier on natural Overworld spawn chances of Palimpsest creatures.")
                .defineInRange("overworldSpawnMultiplier", 1.0D, 0.0D, 4.0D);
        b.pop();

        b.comment("Multiplayer behaviour.").push("multiplayer");
        SHARED_APPARITIONS = b.comment("If true, apparitions summoned for one player are visible to everyone nearby. If false they are sent only to the haunted player where possible.")
                .define("sharedApparitions", true);
        FAKE_CHAT_MESSAGES = b.comment("Allow the late stages to put words in chat that nobody typed.")
                .define("fakeChatMessages", true);
        COOPERATIVE_RITUAL_BONUS = b.comment("Backlash chance reduction per additional player standing near a ritual.")
                .defineInRange("cooperativeRitualBonus", 0.05D, 0.0D, 0.5D);
        b.pop();

        SPEC = b.build();
    }

    private CommonConfig() {}
}
