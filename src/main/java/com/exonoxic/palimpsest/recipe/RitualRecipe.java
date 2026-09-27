package com.exonoxic.palimpsest.recipe;

import com.exonoxic.palimpsest.registry.ModRecipes;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * A rite performed at a Rubric Altar. Offerings sit on Reading Stands around the altar; the
 * catalyst is held by whoever begins it. Conditions (time, moon, weather, dimension) must hold
 * when the rite begins. Every rite has a price in Bleed, and some carry a risk of backlash.
 */
public class RitualRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final NonNullList<Ingredient> offerings;
    private final Ingredient catalyst;
    private final ItemStack result;
    private final String effect;
    private final boolean consumeCatalyst;
    private final String time;
    private final int moonPhase;
    private final String weather;
    private final String dimension;
    private final int duration;
    private final float bleed;
    private final float backlashChance;
    private final String backlash;
    private final int minStage;

    public RitualRecipe(ResourceLocation id, NonNullList<Ingredient> offerings, Ingredient catalyst, ItemStack result, String effect,
                        boolean consumeCatalyst, String time, int moonPhase, String weather, String dimension, int duration,
                        float bleed, float backlashChance, String backlash, int minStage) {
        this.id = id;
        this.offerings = offerings;
        this.catalyst = catalyst;
        this.result = result;
        this.effect = effect;
        this.consumeCatalyst = consumeCatalyst;
        this.time = time;
        this.moonPhase = moonPhase;
        this.weather = weather;
        this.dimension = dimension;
        this.duration = duration;
        this.bleed = bleed;
        this.backlashChance = backlashChance;
        this.backlash = backlash;
        this.minStage = minStage;
    }

    /**
     * Offerings match when every ingredient can be paired with a different stand item and no
     * stand item is left over. Order does not matter.
     */
    public boolean matchesOfferings(List<ItemStack> stands) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack s : stands) if (!s.isEmpty()) items.add(s);
        if (items.size() != offerings.size()) return false;
        return assign(0, items, new boolean[items.size()]);
    }

    private boolean assign(int index, List<ItemStack> items, boolean[] used) {
        if (index == offerings.size()) return true;
        Ingredient ing = offerings.get(index);
        for (int i = 0; i < items.size(); i++) {
            if (used[i] || !ing.test(items.get(i))) continue;
            used[i] = true;
            if (assign(index + 1, items, used)) return true;
            used[i] = false;
        }
        return false;
    }

    public boolean matchesCatalyst(ItemStack held) {
        return catalyst.test(held);
    }

    @Override
    public boolean matches(Container c, Level level) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < c.getContainerSize(); i++) items.add(c.getItem(i));
        return matchesOfferings(items);
    }

    @Override
    public ItemStack assemble(Container c, RegistryAccess access) {
        return result.copy();
    }

    public ItemStack result() { return result; }
    public String effect() { return effect; }
    public boolean consumeCatalyst() { return consumeCatalyst; }
    public String time() { return time; }
    public int moonPhase() { return moonPhase; }
    public String weather() { return weather; }
    public String dimension() { return dimension; }
    public int duration() { return duration; }
    public float bleed() { return bleed; }
    public float backlashChance() { return backlashChance; }
    public String backlash() { return backlash; }
    public int minStage() { return minStage; }
    public NonNullList<Ingredient> offerings() { return offerings; }
    public Ingredient catalyst() { return catalyst; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return offerings;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return result;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.RITUAL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.RITUAL_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<RitualRecipe> {
        @Override
        public RitualRecipe fromJson(ResourceLocation id, JsonObject json) {
            JsonArray arr = GsonHelper.getAsJsonArray(json, "offerings");
            NonNullList<Ingredient> offerings = NonNullList.create();
            for (int i = 0; i < arr.size(); i++) offerings.add(Ingredient.fromJson(arr.get(i)));
            if (offerings.size() > 8) throw new IllegalArgumentException("A rite takes at most 8 offerings: " + id);
            Ingredient catalyst = Ingredient.fromJson(json.get("catalyst"));
            ItemStack result = json.has("result") ? ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result")) : ItemStack.EMPTY;
            return new RitualRecipe(id, offerings, catalyst, result,
                    GsonHelper.getAsString(json, "effect", "none"),
                    GsonHelper.getAsBoolean(json, "consume_catalyst", true),
                    GsonHelper.getAsString(json, "time", "any"),
                    GsonHelper.getAsInt(json, "moon_phase", -1),
                    GsonHelper.getAsString(json, "weather", "any"),
                    GsonHelper.getAsString(json, "dimension", "any"),
                    GsonHelper.getAsInt(json, "duration", 100),
                    GsonHelper.getAsFloat(json, "bleed", 5F),
                    GsonHelper.getAsFloat(json, "backlash_chance", 0F),
                    GsonHelper.getAsString(json, "backlash", "none"),
                    GsonHelper.getAsInt(json, "min_stage", 0));
        }

        @Override
        public RitualRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            NonNullList<Ingredient> offerings = NonNullList.create();
            for (int i = 0; i < n; i++) offerings.add(Ingredient.fromNetwork(buf));
            Ingredient catalyst = Ingredient.fromNetwork(buf);
            ItemStack result = buf.readItem();
            return new RitualRecipe(id, offerings, catalyst, result, buf.readUtf(), buf.readBoolean(), buf.readUtf(), buf.readVarInt(),
                    buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readUtf(), buf.readVarInt());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, RitualRecipe r) {
            buf.writeVarInt(r.offerings.size());
            for (Ingredient i : r.offerings) i.toNetwork(buf);
            r.catalyst.toNetwork(buf);
            buf.writeItem(r.result);
            buf.writeUtf(r.effect);
            buf.writeBoolean(r.consumeCatalyst);
            buf.writeUtf(r.time);
            buf.writeVarInt(r.moonPhase);
            buf.writeUtf(r.weather);
            buf.writeUtf(r.dimension);
            buf.writeVarInt(r.duration);
            buf.writeFloat(r.bleed);
            buf.writeFloat(r.backlashChance);
            buf.writeUtf(r.backlash);
            buf.writeVarInt(r.minStage);
        }
    }
}
