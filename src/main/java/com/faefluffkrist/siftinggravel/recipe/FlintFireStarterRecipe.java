package com.faefluffkrist.siftinggravel.recipe;

import com.faefluffkrist.siftinggravel.config.SiftingGravelConfig;
import com.faefluffkrist.siftinggravel.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import java.util.List;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class FlintFireStarterRecipe extends CustomRecipe {
    public static final FlintFireStarterRecipe INSTANCE=new FlintFireStarterRecipe();
    public static final RecipeSerializer<FlintFireStarterRecipe> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(FlintFireStarterRecipe::new), StreamCodec.unit(INSTANCE));
    @Override public boolean matches(CraftingInput input, Level level){
        SiftingGravelConfig.FireStarterConfig cfg=SiftingGravelConfig.get().flintFireStarter; if(!cfg.enabled)return false;
        int flint=0, extras=0, occupied=0;
        for(ItemStack stack:input.items()){if(stack.isEmpty())continue;occupied++;if(stack.is(Items.FLINT))flint++;else if(FireDrillRecipe.matchesAny(stack,cfg.ingredients))extras++;else return false;}
        return occupied==3 && flint==2 && extras==1;
    }
    @Override public ItemStack assemble(CraftingInput input){return new ItemStack(ModItems.FLINT_FIRE_STARTER);}

    @Override public boolean isSpecial(){return false;}
    @Override public PlacementInfo placementInfo(){
        return PlacementInfo.create(List.of(Ingredient.of(Items.FLINT), Ingredient.of(Items.FLINT), Ingredient.of(Items.STICK)));
    }
    @Override public List<RecipeDisplay> display(){
        return List.of(new ShapelessCraftingRecipeDisplay(
                List.of(new SlotDisplay.ItemSlotDisplay(Items.FLINT), new SlotDisplay.ItemSlotDisplay(Items.FLINT), new SlotDisplay.ItemSlotDisplay(Items.STICK)),
                new SlotDisplay.ItemSlotDisplay(ModItems.FLINT_FIRE_STARTER),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        ));
    }
    @Override public RecipeSerializer<FlintFireStarterRecipe> getSerializer(){return SERIALIZER;}
}
