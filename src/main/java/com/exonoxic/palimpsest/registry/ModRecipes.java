package com.exonoxic.palimpsest.registry;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.recipe.RitualRecipe;
import com.exonoxic.palimpsest.recipe.TranscriptionRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Palimpsest.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Palimpsest.MODID);

    public static final RegistryObject<RecipeType<RitualRecipe>> RITUAL_TYPE = TYPES.register("ritual",
            () -> RecipeType.simple(Palimpsest.id("ritual")));
    public static final RegistryObject<RecipeType<TranscriptionRecipe>> TRANSCRIPTION_TYPE = TYPES.register("transcription",
            () -> RecipeType.simple(Palimpsest.id("transcription")));

    public static final RegistryObject<RecipeSerializer<RitualRecipe>> RITUAL_SERIALIZER = SERIALIZERS.register("ritual",
            RitualRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<TranscriptionRecipe>> TRANSCRIPTION_SERIALIZER = SERIALIZERS.register("transcription",
            TranscriptionRecipe.Serializer::new);

    private ModRecipes() {}
}
