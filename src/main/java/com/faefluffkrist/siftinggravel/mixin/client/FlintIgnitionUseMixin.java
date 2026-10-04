package com.faefluffkrist.siftinggravel.mixin.client;

import com.faefluffkrist.siftinggravel.client.FlintIgnitionAnimationStop;
import com.faefluffkrist.siftinggravel.item.ModItems;
import com.faefluffkrist.siftinggravel.item.PrimitiveFireStarterItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PrimitiveFireStarterItem.class)
public abstract class FlintIgnitionUseMixin {
    @Inject(method = "useOn", at = @At("RETURN"))
    private void siftinggravel$trackFlintIgnition(UseOnContext context,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel().isClientSide() && cir.getReturnValue() == InteractionResult.CONSUME
                && context.getItemInHand().is(ModItems.FLINT_FIRE_STARTER)) {
            FlintIgnitionAnimationStop.begin(((PrimitiveFireStarterItem)(Object)this).ignitionTarget(context),
                    context.getHand(), context.getItemInHand());
        }
    }
}
