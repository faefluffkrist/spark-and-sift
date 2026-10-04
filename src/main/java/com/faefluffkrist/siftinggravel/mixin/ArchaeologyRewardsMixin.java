package com.faefluffkrist.siftinggravel.mixin;

import com.faefluffkrist.siftinggravel.advancement.ArchaeologyRewards;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrushableBlockEntity.class)
public abstract class ArchaeologyRewardsMixin {
    @Inject(method="brush",at=@At("RETURN"))
    private void siftinggravel$completed(long time, ServerLevel level, LivingEntity user, Direction direction,
            ItemStack tool, CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue() && user instanceof ServerPlayer player) ArchaeologyRewards.completed(player,tool);
    }
    @Redirect(method="dropContent",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean siftinggravel$actualLoot(ServerLevel level, Entity drop,
            ServerLevel methodLevel, LivingEntity user, ItemStack tool) {
        boolean spawned=level.addFreshEntity(drop);
        if(spawned && user instanceof ServerPlayer player && drop instanceof ItemEntity item)
            ArchaeologyRewards.found(player,tool,item.getItem());
        return spawned;
    }
}
