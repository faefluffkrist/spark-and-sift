package com.faefluffkrist.siftinggravel.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {
    private static final Identifier SIFTING_BOWL = id("sifting_bowl");

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("siftinggravel", path);
    }

    @ModifyVariable(method = "appendItemLayers", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private ItemStack siftinggravel$temporarySiftingBowlModel(
            ItemStack stack,
            ItemStackRenderState renderState,
            ItemStack originalStack,
            ItemDisplayContext displayContext,
            Level level,
            ItemOwner owner,
            int seed
    ) {
        if (!stack.is(Items.BOWL) || owner == null) return stack;

        LivingEntity living = owner.asLivingEntity();
        if (living == null || !living.isUsingItem() || !living.getUseItem().is(Items.BOWL)) {
            return stack;
        }

        Identifier model = SIFTING_BOWL;

        // The contents are chosen only for the local player's actively-used Bowl.
        // This leaves inventory/dropped Bowls and the Brush completely untouched.
        Minecraft client = Minecraft.getInstance();
        if (client.player == living && client.level != null && client.hitResult instanceof BlockHitResult hit) {
            BlockState state = client.level.getBlockState(hit.getBlockPos());
            model = siftinggravel$modelFor(state, living.tickCount);
        }

        ItemStack rendered = stack.copy();
        rendered.set(DataComponents.ITEM_MODEL, model);
        return rendered;
    }

    private static Identifier siftinggravel$modelFor(BlockState state, int tickCount) {
        String material = null;
        if (state.is(Blocks.GRAVEL)) material = "gravel";
        else if (state.is(Blocks.COARSE_DIRT)) material = "coarse_dirt";
        else if (state.is(Blocks.ROOTED_DIRT)) material = "rooted_dirt";
        else if (state.is(Blocks.SAND)) material = "sand";
        else if (state.is(Blocks.RED_SAND)) material = "red_sand";
        else if (state.is(Blocks.SOUL_SAND)) material = "soul_sand";
        else if (state.is(Blocks.SOUL_SOIL)) material = "soul_soil";
        else if (state.is(Blocks.SUSPICIOUS_SAND)) material = "suspicious_sand";
        else if (state.is(Blocks.SUSPICIOUS_GRAVEL)) material = "suspicious_gravel";
        if (material == null) return SIFTING_BOWL;

        // Do not rely on minecraft:use_duration for Bowls. Bowls have no native use
        // duration, so the item-model property can remain at its fallback forever.
        // Pick an explicit rendered model every five client ticks instead.
        int frame = Math.floorMod(tickCount / 5, 4);
        return id("sifting_bowl_" + material + "_frame_" + frame);
    }
}
