package com.faefluffkrist.siftinggravel.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class GravelSiftingRecipe extends AbstractBowlSiftingRecipe {
    public static final GravelSiftingRecipe INSTANCE = new GravelSiftingRecipe();
    public static final RecipeSerializer<GravelSiftingRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(GravelSiftingRecipe::new),
            StreamCodec.unit(INSTANCE)
    );

    public GravelSiftingRecipe() {
        super(Items.GRAVEL);
    }

    @Override
    public RecipeSerializer<GravelSiftingRecipe> getSerializer() {
        return SERIALIZER;
    }
}
