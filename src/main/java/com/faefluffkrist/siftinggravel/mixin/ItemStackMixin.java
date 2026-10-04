package com.faefluffkrist.siftinggravel.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemUseAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gives a bowl the vanilla brush pose whenever ManualSifting has put it into use. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void siftinggravel$brushAnimationForUsedBowl(CallbackInfoReturnable<ItemUseAnimation> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.is(Items.BOWL)) {
            cir.setReturnValue(ItemUseAnimation.BRUSH);
        }
    }
}
