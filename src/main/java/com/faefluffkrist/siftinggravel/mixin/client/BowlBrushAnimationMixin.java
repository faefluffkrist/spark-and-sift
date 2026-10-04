package com.faefluffkrist.siftinggravel.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives only Spark and Sift's actively-used Bowl a slower, more centered version
 * of vanilla's first-person brush transform. Real Brushes still use vanilla code.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class BowlBrushAnimationMixin {
    @Inject(method = "applyBrushTransform", at = @At("HEAD"), cancellable = true)
    private void siftinggravel$slowerBowlBrush(
            PoseStack poseStack,
            float partialTick,
            HumanoidArm arm,
            Player player,
            CallbackInfo ci
    ) {
        if (!player.getUseItem().is(Items.BOWL)) {
            return; // Critical: vanilla Brush animation is completely untouched.
        }

        // Vanilla uses a 10-tick cycle. A 20-tick cycle is exactly 50% speed.
        float phaseTicks = player.getUseItemRemainingTicks() % 20;
        float interpolated = phaseTicks - partialTick + 1.0F;
        float phase = 1.0F - interpolated / 20.0F;

        // Bowl-only cropped brush-style sweep: preserve the middle of the motion,
        // but remove the troublesome extremes. The cycle still runs at half speed.
        float sweepWave = Mth.cos(phase * 2.0F * (float)Math.PI);
        float sweepAngle = -15.0F + 37.5F * sweepWave;

        // The Bowl is much wider than a Brush, so one fixed orientation does not
        // look correct at both ends of the sweep. Give each end its own subtle
        // compensation and smoothly blend between them.
        //
        // sweepWave +1 = one end, -1 = the opposite end.
        float leftEnd = (sweepWave + 1.0F) * 0.5F;
        float rightEnd = 1.0F - leftEnd;

        if (arm != HumanoidArm.RIGHT) {
            poseStack.translate(0.30D, 0.83D, 0.35D);
            poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(sweepAngle));
            poseStack.translate(-0.30D, 0.22D, 0.35D);
        } else {
            // Position the sweep toward screen-left and raise the right endpoint
            // so the wide bowl stays visible throughout the motion.
            double x = -0.66D - (0.105D * leftEnd) - (0.085D * rightEnd);
            double y =  0.22D + (0.085D * rightEnd);
            // Positive Z brings the bowl toward the camera. Pull the left endpoint
            // closer and push the right endpoint away to balance the sweep's depth.
            double z = 0.34D + (0.145D * leftEnd) - (0.085D * rightEnd);

            poseStack.translate(x, y, z);
            poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));

            // This is deliberately Y-axis compensation, not another diagonal
            // screen-space tilt. Each end faces the camera a little differently.
            float endpointYaw = (6.0F * leftEnd) - (7.0F * rightEnd);
            poseStack.mulPose(Axis.YP.rotationDegrees(endpointYaw));

            poseStack.mulPose(Axis.XP.rotationDegrees(sweepAngle));
        }

        ci.cancel();
    }
}
