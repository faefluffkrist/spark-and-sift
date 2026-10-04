package com.faefluffkrist.siftinggravel.compat;

import com.faefluffkrist.siftinggravel.item.PrimitiveFireStarterItem;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.huaxiaosheng.smoothplayeranimations.state.ActionController", remap=false)
public abstract class SmoothStarterUseMixin {
    @Shadow private boolean miningAnimActive;
    @Shadow private void clearUpperPlayback(AbstractClientPlayer player, int fade) { throw new AssertionError(); }

    @Inject(method="handleToolSwing", at=@At("HEAD"), cancellable=true, require=0, remap=false)
    private void siftinggravel$heldUseIsNotPunching(AbstractClientPlayer player, @Coerce Object snapshot,
            CallbackInfoReturnable<Object> cir) {
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof PrimitiveFireStarterItem) {
            if (miningAnimActive) clearUpperPlayback(player, 2);
            // Continue the controller's normal NONE use/idle path. Keep model animation active.
            cir.setReturnValue(null);
        }
    }
}
