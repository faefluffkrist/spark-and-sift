package com.faefluffkrist.siftinggravel.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class CoarseDirtSiftingRecipe extends AbstractBowlSiftingRecipe {
    public static final CoarseDirtSiftingRecipe INSTANCE = new CoarseDirtSiftingRecipe();
    public static final RecipeSerializer<CoarseDirtSiftingRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(CoarseDirtSiftingRecipe::new),
            StreamCodec.unit(INSTANCE)
    );

    public CoarseDirtSiftingRecipe() {
        super(Items.COARSE_DIRT);
    }

    @Override
    public RecipeSerializer<CoarseDirtSiftingRecipe> getSerializer() {
        return SERIALIZER;
    }
}
