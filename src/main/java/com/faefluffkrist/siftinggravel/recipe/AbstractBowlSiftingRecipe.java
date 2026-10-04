package com.faefluffkrist.siftinggravel.recipe;

import com.faefluffkrist.siftinggravel.config.SiftingGravelConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.ItemStackTemplate;
import java.util.List;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.level.Level;

abstract class AbstractBowlSiftingRecipe extends CustomRecipe {
    private final Item siftedItem;

    protected AbstractBowlSiftingRecipe(Item siftedItem) {
        this.siftedItem = siftedItem;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        SiftingGravelConfig config = SiftingGravelConfig.get();
        if (!config.inventoryCraftingEnabled) return false;
        if (siftedItem == Items.GRAVEL && !config.gravel.enabled) return false;
        if (siftedItem == Items.COARSE_DIRT && !config.coarseDirt.enabled) return false;
        if (siftedItem == Items.ROOTED_DIRT && !config.rootedDirt.enabled) return false;
        int bowlSlots = 0;
        int materialSlots = 0;
        int occupiedSlots = 0;

        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }

            occupiedSlots++;
            if (stack.getItem() == Items.BOWL) {
                // One reusable bowl slot is required, but the stack may contain
                // any number of bowls. Quick Sifting never consumes the bowl stack.
                bowlSlots++;
            } else if (stack.getItem() == siftedItem) {
                materialSlots++;
            } else {
                return false;
            }
        }

        // Vanilla crafting recipes count ingredient slots, not multiple items in
        // one slot. Require one bowl plus three separate stacks of siftable material.
        // This fills a 2x2 inventory crafting grid exactly, so no crafting table is needed.
        return occupiedSlots == 4 && bowlSlots == 1 && materialSlots == 3;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack result = new ItemStack(Items.FLINT);
        String materialName = siftedItem == Items.COARSE_DIRT ? "Coarse Dirt"
                : siftedItem == Items.ROOTED_DIRT ? "Rooted Dirt" : "Gravel";
        result.set(DataComponents.CUSTOM_NAME,
                Component.literal("Sifted " + materialName).withStyle(ChatFormatting.WHITE));
        result.set(DataComponents.ITEM_MODEL,
                Identifier.fromNamespaceAndPath("siftinggravel", "quick_sift"));
        return result;
    }

    private boolean siftinggravel$isEnabledForBook() {
        SiftingGravelConfig config = SiftingGravelConfig.get();
        if (!config.inventoryCraftingEnabled) return false;
        if (siftedItem == Items.GRAVEL) return config.gravel.enabled;
        if (siftedItem == Items.COARSE_DIRT) return config.coarseDirt.enabled;
        if (siftedItem == Items.ROOTED_DIRT) return config.rootedDirt.enabled;
        return false;
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public PlacementInfo placementInfo() {
        if (!siftinggravel$isEnabledForBook()) return PlacementInfo.NOT_PLACEABLE;
        return PlacementInfo.create(List.of(
                Ingredient.of(Items.BOWL),
                Ingredient.of(siftedItem),
                Ingredient.of(siftedItem),
                Ingredient.of(siftedItem)
        ));
    }

    private ItemStack siftinggravel$displayResult() {
        ItemStack result = new ItemStack(Items.FLINT);
        String materialName = siftedItem == Items.COARSE_DIRT ? "Coarse Dirt"
                : siftedItem == Items.ROOTED_DIRT ? "Rooted Dirt" : "Gravel";
        result.set(DataComponents.CUSTOM_NAME,
                Component.literal("Sifted " + materialName).withStyle(ChatFormatting.WHITE));
        result.set(DataComponents.ITEM_MODEL,
                Identifier.fromNamespaceAndPath("siftinggravel", "quick_sift"));
        return result;
    }

    @Override
    public List<RecipeDisplay> display() {
        if (!siftinggravel$isEnabledForBook()) return List.of();
        return List.of(new ShapelessCraftingRecipeDisplay(
                List.of(
                        new SlotDisplay.ItemSlotDisplay(Items.BOWL),
                        new SlotDisplay.ItemSlotDisplay(siftedItem),
                        new SlotDisplay.ItemSlotDisplay(siftedItem),
                        new SlotDisplay.ItemSlotDisplay(siftedItem)
                ),
                new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(siftinggravel$displayResult())),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        ));
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remainder = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == Items.BOWL) {
                // Vanilla consumes one ingredient from the slot per craft.
                // Return exactly one bowl so the reusable bowl stack has zero net change.
                remainder.set(slot, new ItemStack(Items.BOWL));
            }
        }

        return remainder;
    }
}
