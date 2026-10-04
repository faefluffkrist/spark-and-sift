package com.faefluffkrist.siftinggravel.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class RootedDirtSiftingRecipe extends AbstractBowlSiftingRecipe {
    public static final RootedDirtSiftingRecipe INSTANCE = new RootedDirtSiftingRecipe();
    public static final RecipeSerializer<RootedDirtSiftingRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(RootedDirtSiftingRecipe::new),
            StreamCodec.unit(INSTANCE)
    );

    public RootedDirtSiftingRecipe() { super(Items.ROOTED_DIRT); }

    @Override
    public RecipeSerializer<RootedDirtSiftingRecipe> getSerializer() { return SERIALIZER; }
}
