package com.faefluffkrist.siftinggravel.compat;

import com.faefluffkrist.siftinggravel.item.PrimitiveFireStarterItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import com.faefluffkrist.siftinggravel.sifting.ManualSifting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="punchy.client.state.UseActionTracker", remap=false)
public abstract class PunchyUseTriggerMixin {
    @Inject(method={
            "recordUse(Lnet/minecraft/world/InteractionHand;Lpunchy/client/state/UseActionTracker$TriggerType;Lnet/minecraft/world/phys/BlockHitResult;)V",
            "recordFallbackUse"}, at=@At("HEAD"), cancellable=true, require=0, remap=false)
    private static void siftinggravel$onlyActualIgnition(InteractionHand hand, @Coerce Object type,
            BlockHitResult hit, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player == null || hand == null || "DROP_ITEM".equals(String.valueOf(type))) return;
        if (player.getItemInHand(hand).is(Items.BRUSH) && hit != null
                && ManualSifting.blocksBrushFeedback(player.level().getBlockState(hit.getBlockPos()))) {
            ci.cancel();
            return;
        }
        if (!(player.getItemInHand(hand).getItem() instanceof PrimitiveFireStarterItem)) return;
        if (!player.isUsingItem() || player.getUsedItemHand() != hand
                || !(player.getUseItem().getItem() instanceof PrimitiveFireStarterItem)) ci.cancel();
    }
}
