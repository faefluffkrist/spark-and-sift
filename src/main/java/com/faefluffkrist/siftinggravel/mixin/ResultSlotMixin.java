package com.faefluffkrist.siftinggravel.mixin;

import net.minecraft.core.component.DataComponents;
import com.faefluffkrist.siftinggravel.advancement.ModCriteria;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Plays material feedback when Flint is taken from either bowl-sifting recipe. */
@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
    @Shadow private CraftingContainer craftSlots;
    @Shadow private Player player;

    @Unique private boolean siftinggravel$quickMoveSound;
    @Unique private boolean siftinggravel$unpackSoundPending;
    @Unique private long siftinggravel$lastBulkSoundTick = Long.MIN_VALUE;

    /*
     * Shift-click follows Slot#onQuickCraft before ResultSlot#onTake. Catch it here
     * while the crafting grid still identifies which sifting recipe is active.
     */
    @Inject(method = "onQuickCraft(Lnet/minecraft/world/item/ItemStack;I)V", at = @At("HEAD"))
    private void siftinggravel$playQuickCraftSound(ItemStack stack, int amount, CallbackInfo ci) {
        long now = player.level().getGameTime();
        if (amount > 0 && now == siftinggravel$lastBulkSoundTick) {
            siftinggravel$quickMoveSound = true;
            return;
        }
        if (amount > 0) siftinggravel$lastBulkSoundTick = now;
        if (amount > 0 && siftinggravel$isUnpackInput()) {
            siftinggravel$quickMoveSound = true;
            siftinggravel$playUnpack(true);
            return;
        }
        SoundEvent sound = siftinggravel$recipeSound(stack);
        if (sound == null || amount <= 0) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) ModCriteria.QUICK_SIFT.trigger(serverPlayer);
        siftinggravel$quickMoveSound = true;
        siftinggravel$play(sound, true);
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void siftinggravel$playSingleCraftSound(Player player, ItemStack stack, CallbackInfo ci) {
        // At HEAD the crafting input is still intact. Capture conversion identity now,
        // before vanilla consumes/mutates the input slots.
        boolean unpackNow = siftinggravel$isUnpackInput();
        if (!unpackNow && siftinggravel$hasSiftedIngredient() && player instanceof ServerPlayer serverPlayer) {
            ModCriteria.SIFTED_CRAFTING.trigger(serverPlayer);
        }

        // quickMoveStack calls onQuickCraft and then onTake. Do not triple-play it.
        if (siftinggravel$quickMoveSound) {
            siftinggravel$quickMoveSound = false;
            return;
        }
        if (unpackNow) {
            siftinggravel$playUnpack(false);
            return;
        }
        SoundEvent sound = siftinggravel$recipeSound(stack);
        if (sound != null) {
            if (player instanceof ServerPlayer serverPlayer) ModCriteria.QUICK_SIFT.trigger(serverPlayer);
            siftinggravel$play(sound, false);
        }
    }



    @Unique
    private boolean siftinggravel$hasSiftedIngredient() {
        Identifier quickSift = Identifier.fromNamespaceAndPath("siftinggravel", "quick_sift");
        for (int slot = 0; slot < craftSlots.getContainerSize(); slot++) {
            ItemStack ingredient = craftSlots.getItem(slot);
            if (ingredient.isEmpty() || !ingredient.is(Items.FLINT)) continue;
            if (quickSift.equals(ingredient.get(DataComponents.ITEM_MODEL))) return true;
        }
        return false;
    }

    @Unique
    private boolean siftinggravel$isUnpackInput() {
        int occupied = 0;
        for (int slot = 0; slot < craftSlots.getContainerSize(); slot++) {
            ItemStack ingredient = craftSlots.getItem(slot);
            if (ingredient.isEmpty()) continue;
            occupied++;
            if (!ingredient.is(Items.FLINT)) return false;
            Identifier model = ingredient.get(DataComponents.ITEM_MODEL);
            if (!Identifier.fromNamespaceAndPath("siftinggravel", "quick_sift").equals(model)) return false;
        }
        return occupied == 1;
    }

    @Unique
    private void siftinggravel$playUnpack(boolean batch) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS,
                SoundSource.PLAYERS, 0.18F, 1.00F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.FLINTANDSTEEL_USE,
                SoundSource.PLAYERS, 0.14F, 1.30F);
    }

    @Unique
    private SoundEvent siftinggravel$recipeSound(ItemStack result) {
        if (!result.is(Items.FLINT)) return null;

        Component customName = result.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            String name = customName.getString();
            if ("Sifted Gravel".equals(name)) return SoundEvents.GRAVEL_BREAK;
            if ("Sifted Coarse Dirt".equals(name)) return SoundEvents.GRASS_BREAK;
            if ("Sifted Rooted Dirt".equals(name)) return SoundEvents.GRASS_BREAK;
        }

        int bowls = 0, gravel = 0, coarseDirt = 0, rootedDirt = 0, occupied = 0;
        for (int slot = 0; slot < craftSlots.getContainerSize(); slot++) {
            ItemStack ingredient = craftSlots.getItem(slot);
            if (ingredient.isEmpty()) continue;
            occupied++;
            if (ingredient.is(Items.BOWL) && ingredient.getCount() == 1) bowls++;
            else if (ingredient.is(Items.GRAVEL)) gravel++;
            else if (ingredient.is(Items.COARSE_DIRT)) coarseDirt++;
            else if (ingredient.is(Items.ROOTED_DIRT)) rootedDirt++;
        }

        if (occupied == 4 && bowls == 1 && gravel == 3 && coarseDirt == 0 && rootedDirt == 0) return SoundEvents.GRAVEL_BREAK;
        if (occupied == 4 && bowls == 1 && coarseDirt == 3 && gravel == 0 && rootedDirt == 0) return SoundEvents.GRASS_BREAK;
        if (occupied == 4 && bowls == 1 && rootedDirt == 3 && gravel == 0 && coarseDirt == 0) return SoundEvents.GRASS_BREAK;
        return null;
    }

    @Unique
    private void siftinggravel$play(SoundEvent sound, boolean batch) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.52F, 1.0F);
    }
}
