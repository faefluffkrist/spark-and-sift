package com.faefluffkrist.siftinggravel.recipe;

import com.faefluffkrist.siftinggravel.config.SiftingGravelConfig;
import com.faefluffkrist.siftinggravel.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
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

public final class FireDrillRecipe extends CustomRecipe {
    public static final FireDrillRecipe INSTANCE=new FireDrillRecipe();
    public static final RecipeSerializer<FireDrillRecipe> SERIALIZER=new RecipeSerializer<>(MapCodec.unit(FireDrillRecipe::new), StreamCodec.unit(INSTANCE));
    @Override public boolean matches(CraftingInput input, Level level){
        SiftingGravelConfig.FireStarterConfig cfg=SiftingGravelConfig.get().fireDrill; if(!cfg.enabled)return false;
        int sticks=0, extras=0, occupied=0;
        for(ItemStack stack:input.items()){if(stack.isEmpty())continue;occupied++; if(stack.is(Items.STICK))sticks++; if(matchesAny(stack,cfg.ingredients))extras++; else if(!stack.is(Items.STICK))return false;}
        // Two occupied slots: one must satisfy the fixed Stick base, the other a configured companion.
        return occupied==2 && sticks>=1 && extras>=1;
    }
    @Override public ItemStack assemble(CraftingInput input){return new ItemStack(ModItems.FIRE_DRILL);}

    @Override public boolean isSpecial(){return false;}
    @Override public PlacementInfo placementInfo(){
        return PlacementInfo.create(List.of(Ingredient.of(Items.STICK), Ingredient.of(Items.STICK)));
    }
    @Override public List<RecipeDisplay> display(){
        return List.of(new ShapelessCraftingRecipeDisplay(
                List.of(new SlotDisplay.ItemSlotDisplay(Items.STICK), new SlotDisplay.ItemSlotDisplay(Items.STICK)),
                new SlotDisplay.ItemSlotDisplay(ModItems.FIRE_DRILL),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        ));
    }
    @Override public RecipeSerializer<FireDrillRecipe> getSerializer(){return SERIALIZER;}
    static boolean matchesAny(ItemStack stack, java.util.List<String> entries){
        for(String raw:entries){if(raw==null||raw.isBlank())continue;try{String v=raw.trim();if(v.startsWith("#")){Identifier id=Identifier.parse(v.substring(1));if(stack.is(TagKey.create(Registries.ITEM,id)))return true;}else{Identifier id=Identifier.parse(v);Item item=BuiltInRegistries.ITEM.get(id).map(h->h.value()).orElse(null);if(item!=null&&stack.is(item))return true;}}catch(Exception ignored){}}
        return false;
    }
}
