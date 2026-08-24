package dev.eriksonn.aeronautics.index;

import dev.eriksonn.aeronautics.Aeronautics;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Minecraft 1.20.1 represents armor materials as direct interface instances,
 * rather than entries in the 1.21 armor-material registry.
 */
public enum AeroArmorMaterials implements ArmorMaterial {
    AVIATORS_GOGGLES;

    @Override
    public int getDurabilityForType(final ArmorItem.Type type) {
        // The pinned 1.21 item declares no durability component.
        return 0;
    }

    @Override
    public int getDefenseForType(final ArmorItem.Type type) {
        return type == ArmorItem.Type.HELMET ? 1 : 0;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.LEATHER);
    }

    @Override
    public String getName() {
        return Aeronautics.MOD_ID + ":aviators_goggles";
    }

    @Override
    public float getToughness() {
        return 0.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }

    public static void init() {
    }
}
