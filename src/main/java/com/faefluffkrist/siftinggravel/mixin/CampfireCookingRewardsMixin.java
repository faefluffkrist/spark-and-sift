package com.faefluffkrist.siftinggravel.mixin;

import com.faefluffkrist.siftinggravel.advancement.ModCriteria;
import com.faefluffkrist.siftinggravel.advancement.CampfireCookTracking;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(CampfireBlockEntity.class)
public abstract class CampfireCookingRewardsMixin implements CampfireCookTracking {
    @Shadow @Final private NonNullList<ItemStack> items;
    @Unique private final UUID[] siftinggravel$cooks=new UUID[4];
    @Unique private final boolean[] siftinggravel$occupied=new boolean[4];
    @Unique private final List<ItemStack> siftinggravel$results=new ArrayList<>();
    @Unique private int siftinggravel$placingSlot=-1;

    @Inject(method="placeFood",at=@At("HEAD"))
    private void siftinggravel$beforePlacement(ServerLevel level, LivingEntity source, ItemStack food,
            CallbackInfoReturnable<Boolean> cir) {
        siftinggravel$placingSlot=-1;
        for(int i=0;i<items.size();i++) if(items.get(i).isEmpty()) {siftinggravel$placingSlot=i;break;}
    }
    @Inject(method="placeFood",at=@At("RETURN"))
    private void siftinggravel$recordCook(ServerLevel level, LivingEntity source, ItemStack food,
            CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue() && siftinggravel$placingSlot>=0)
            siftinggravel$cooks[siftinggravel$placingSlot]=source instanceof ServerPlayer player?player.getUUID():null;
        siftinggravel$placingSlot=-1;
    }
    @Inject(method="cookTick",at=@At("HEAD"))
    private static void siftinggravel$beforeCooking(ServerLevel level, BlockPos pos, BlockState state,
            CampfireBlockEntity entity, RecipeManager.CachedCheck<SingleRecipeInput,CampfireCookingRecipe> cache,
            CallbackInfo ci) {
        ((CampfireCookTracking)entity).siftinggravel$beginCook();
    }
    @Redirect(method="cookTick",at=@At(value="INVOKE",target="Lnet/minecraft/world/Containers;dropItemStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"))
    private static void siftinggravel$completedResult(Level dropLevel,double x,double y,double z,ItemStack result,
            ServerLevel level,BlockPos pos,BlockState state,CampfireBlockEntity entity,
            RecipeManager.CachedCheck<SingleRecipeInput,CampfireCookingRecipe> cache) {
        ((CampfireCookTracking)entity).siftinggravel$cookedResult(result.copy());
        Containers.dropItemStack(dropLevel,x,y,z,result);
    }
    @Inject(method="cookTick",at=@At("TAIL"))
    private static void siftinggravel$afterCooking(ServerLevel level,BlockPos pos,BlockState state,
            CampfireBlockEntity entity,RecipeManager.CachedCheck<SingleRecipeInput,CampfireCookingRecipe> cache,
            CallbackInfo ci) {
        ((CampfireCookTracking)entity).siftinggravel$finishCook(level,state);
    }
    @Override public void siftinggravel$beginCook() {
        siftinggravel$results.clear();
        for(int i=0;i<items.size();i++) siftinggravel$occupied[i]=!items.get(i).isEmpty();
    }
    @Override public void siftinggravel$cookedResult(ItemStack result) {siftinggravel$results.add(result);}
    @Override public void siftinggravel$finishCook(ServerLevel level,BlockState state) {
        int resultIndex=0;
        for(int i=0;i<items.size();i++) {
            if(!siftinggravel$occupied[i] || !items.get(i).isEmpty()) continue;
            ItemStack result=resultIndex<siftinggravel$results.size()?siftinggravel$results.get(resultIndex++):ItemStack.EMPTY;
            UUID cook=siftinggravel$cooks[i];siftinggravel$cooks[i]=null;
            if(cook==null || !result.has(DataComponents.FOOD)) continue;
            ServerPlayer player=level.getServer().getPlayerList().getPlayer(cook);
            if(player==null) continue;
            if(state.is(Blocks.SOUL_CAMPFIRE)) {
                if(siftinggravel$done(player,"soul_fire")) ModCriteria.SOUL_CAMPFIRE_COOK.trigger(player);
            } else if(state.is(Blocks.CAMPFIRE) && siftinggravel$done(player,"finally_a_flame")) {
                ModCriteria.CAMPFIRE_COOK.trigger(player);
            }
        }
        siftinggravel$results.clear();
    }
    @Unique private static boolean siftinggravel$done(ServerPlayer player,String path) {
        var advancement=player.level().getServer().getAdvancements().get(Identifier.fromNamespaceAndPath("siftinggravel",path));
        return advancement!=null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }
    @Inject(method="saveAdditional",at=@At("TAIL"))
    private void siftinggravel$saveCooks(ValueOutput output,CallbackInfo ci) {
        List<String> saved=new ArrayList<>();
        for(UUID cook:siftinggravel$cooks) saved.add(cook==null?"":cook.toString());
        output.store("siftinggravel_cooks",Codec.STRING.listOf(),saved);
    }
    @Inject(method="loadAdditional",at=@At("TAIL"))
    private void siftinggravel$loadCooks(ValueInput input,CallbackInfo ci) {
        List<String> saved=input.read("siftinggravel_cooks",Codec.STRING.listOf()).orElse(List.of());
        for(int i=0;i<4;i++) {
            siftinggravel$cooks[i]=null;
            if(i<saved.size() && !saved.get(i).isEmpty()) try{siftinggravel$cooks[i]=UUID.fromString(saved.get(i));}
            catch(IllegalArgumentException ignored){}
        }
    }
}
