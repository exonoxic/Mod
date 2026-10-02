package com.exonoxic.palimpsest.recipe;

import com.exonoxic.palimpsest.registry.ModRecipes;
import com.google.gson.JsonObject;
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

/**
 * Scriptorium Desk recipe: a surface (usually vellum), an ink, and a subject, worked for a
 * number of ticks. Slots: 0 surface, 1 ink, 2 subject.
 *
 * <pre>
 * { "type": "palimpsest:transcription", "surface": {...}, "ink": {...}, "subject": {...},
 *   "result": {"item": "..."}, "time": 200, "keep_nbt": false }
 * </pre>
 */
public class TranscriptionRecipe implements Recipe<Container> {
    public static final int SURFACE = 0;
    public static final int INK = 1;
    public static final int SUBJECT = 2;

    private final ResourceLocation id;
    private final Ingredient surface;
    private final Ingredient ink;
    private final Ingredient subject;
    private final ItemStack result;
    private final int time;
    private final boolean keepNbt;

    public TranscriptionRecipe(ResourceLocation id, Ingredient surface, Ingredient ink, Ingredient subject, ItemStack result, int time, boolean keepNbt) {
        this.id = id;
        this.surface = surface;
        this.ink = ink;
        this.subject = subject;
        this.result = result;
        this.time = time;
        this.keepNbt = keepNbt;
    }

    @Override
    public boolean matches(Container c, Level level) {
        return test(surface, c.getItem(SURFACE)) && test(ink, c.getItem(INK)) && test(subject, c.getItem(SUBJECT));
    }

    private static boolean test(Ingredient ingredient, ItemStack stack) {
        return ingredient.isEmpty() ? stack.isEmpty() : ingredient.test(stack);
    }

    @Override
    public ItemStack assemble(Container c, RegistryAccess access) {
        ItemStack out = result.copy();
        ItemStack subj = c.getItem(SUBJECT);
        if (keepNbt && subj.hasTag()) {
            out.setTag(subj.getTag().copy());
            if (out.isDamageableItem()) out.setDamageValue(0);
        }
        return out;
    }

    public boolean usesSurface() {
        return !surface.isEmpty();
    }

    public boolean usesInk() {
        return !ink.isEmpty();
    }

    public boolean usesSubject() {
        return !subject.isEmpty();
    }

    public int getTime() {
        return time;
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
        return ModRecipes.TRANSCRIPTION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.TRANSCRIPTION_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<TranscriptionRecipe> {
        @Override
        public TranscriptionRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient surface = json.has("surface") ? Ingredient.fromJson(json.get("surface")) : Ingredient.EMPTY;
            Ingredient ink = json.has("ink") ? Ingredient.fromJson(json.get("ink")) : Ingredient.EMPTY;
            Ingredient subject = json.has("subject") ? Ingredient.fromJson(json.get("subject")) : Ingredient.EMPTY;
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int time = GsonHelper.getAsInt(json, "time", 200);
            boolean keep = GsonHelper.getAsBoolean(json, "keep_nbt", false);
            return new TranscriptionRecipe(id, surface, ink, subject, result, time, keep);
        }

        @Override
        public TranscriptionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient surface = Ingredient.fromNetwork(buf);
            Ingredient ink = Ingredient.fromNetwork(buf);
            Ingredient subject = Ingredient.fromNetwork(buf);
            ItemStack result = buf.readItem();
            int time = buf.readVarInt();
            boolean keep = buf.readBoolean();
            return new TranscriptionRecipe(id, surface, ink, subject, result, time, keep);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, TranscriptionRecipe r) {
            r.surface.toNetwork(buf);
            r.ink.toNetwork(buf);
            r.subject.toNetwork(buf);
            buf.writeItem(r.result);
            buf.writeVarInt(r.time);
            buf.writeBoolean(r.keepNbt);
        }
    }
}
