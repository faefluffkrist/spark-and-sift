package com.faefluffkrist.siftinggravel.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import com.faefluffkrist.siftinggravel.item.PrimitiveFireStarterItem;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla bowls have no use duration, so startUsingItem would immediately end.
 * When a bowl is explicitly put into a use state by ManualSifting, keep that
 * state alive long enough for the server-side sifting timer to run.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow private int useItemRemaining;

    @Inject(method="swing(Lnet/minecraft/world/InteractionHand;Z)V", at=@At("HEAD"), cancellable=true)
    private void siftinggravel$noSwingDuringIgnition(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (self.isUsingItem() && self.getUsedItemHand() == hand
                && self.getUseItem().getItem() instanceof PrimitiveFireStarterItem) ci.cancel();
    }


    @Inject(method = "startUsingItem", at = @At("TAIL"))
    private void siftinggravel$allowHeldBowlUse(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getUseItem().is(Items.BOWL)) {
            this.useItemRemaining = 72000;
        }
    }
}
