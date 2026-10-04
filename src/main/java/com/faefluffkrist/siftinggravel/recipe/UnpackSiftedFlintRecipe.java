package com.faefluffkrist.siftinggravel.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class UnpackSiftedFlintRecipe extends CustomRecipe {
    private static final Identifier QUICK_SIFT_MODEL =
            Identifier.fromNamespaceAndPath("siftinggravel", "quick_sift");

    public static final UnpackSiftedFlintRecipe INSTANCE = new UnpackSiftedFlintRecipe();
    public static final RecipeSerializer<UnpackSiftedFlintRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(UnpackSiftedFlintRecipe::new),
            StreamCodec.unit(INSTANCE)
    );

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int occupied = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            occupied++;
            if (!isSiftedFlint(stack)) return false;
        }
        return occupied == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return new ItemStack(Items.FLINT);
    }

    private static boolean isSiftedFlint(ItemStack stack) {
        if (!stack.is(Items.FLINT)) return false;
        Identifier model = stack.get(DataComponents.ITEM_MODEL);
        return QUICK_SIFT_MODEL.equals(model);
    }

    @Override
    public RecipeSerializer<UnpackSiftedFlintRecipe> getSerializer() {
        return SERIALIZER;
    }
}
