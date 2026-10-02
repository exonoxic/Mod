package com.exonoxic.palimpsest.registry;

import com.exonoxic.palimpsest.Palimpsest;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.function.Supplier;

public enum ModArmorMaterials implements ArmorMaterial {
    RUBRIC("rubric", 14, new int[]{2, 5, 4, 1}, 18, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
            () -> Ingredient.of(ModItems.BLANK_VELLUM.get())),
    FIRST_DRAFT("first_draft", 30, new int[]{3, 0, 0, 0}, 25, SoundEvents.ARMOR_EQUIP_GOLD, 1.0F, 0.0F,
            () -> Ingredient.of(ModItems.ILLUMINE_LEAF.get()));

    private static final EnumMap<ArmorItem.Type, Integer> BASE_DURABILITY = new EnumMap<>(ArmorItem.Type.class);

    static {
        BASE_DURABILITY.put(ArmorItem.Type.HELMET, 11);
        BASE_DURABILITY.put(ArmorItem.Type.CHESTPLATE, 16);
        BASE_DURABILITY.put(ArmorItem.Type.LEGGINGS, 15);
        BASE_DURABILITY.put(ArmorItem.Type.BOOTS, 13);
    }

    private final String name;
    private final int durabilityMultiplier;
    /** helmet, chestplate, leggings, boots */
    private final int[] defense;
    private final int enchantability;
    private final SoundEvent sound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repair;

    ModArmorMaterials(String name, int durabilityMultiplier, int[] defense, int enchantability, SoundEvent sound,
                      float toughness, float knockbackResistance, Supplier<Ingredient> repair) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.defense = defense;
        this.enchantability = enchantability;
        this.sound = sound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repair = repair;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return BASE_DURABILITY.get(type) * durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> defense[0];
            case CHESTPLATE -> defense[1];
            case LEGGINGS -> defense[2];
            case BOOTS -> defense[3];
        };
    }

    @Override
    public int getEnchantmentValue() {
        return enchantability;
    }

    @Override
    public SoundEvent getEquipSound() {
        return sound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repair.get();
    }

    @Override
    public String getName() {
        return Palimpsest.MODID + ":" + name;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return knockbackResistance;
    }
}
