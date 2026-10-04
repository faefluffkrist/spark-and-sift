package com.faefluffkrist.siftinggravel.mixin.client;

import com.faefluffkrist.siftinggravel.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Raise the Drill only in vanilla's first-person hand-rendering path. */
@Mixin(ItemInHandRenderer.class)
public abstract class FireDrillVanillaPositionMixin {
    @Inject(method="applyItemArmTransform", at=@At("TAIL"))
    private void siftinggravel$raiseVanillaDrill(PoseStack poseStack, HumanoidArm arm,
            float inverseArmHeight, CallbackInfo ci) {
        var player=Minecraft.getInstance().player;
        if(player==null) return;
        InteractionHand hand=arm==player.getMainArm()?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
        if(player.getItemInHand(hand).is(ModItems.FIRE_DRILL)) poseStack.translate(0.0D,0.40D,0.0D);
    }
}
