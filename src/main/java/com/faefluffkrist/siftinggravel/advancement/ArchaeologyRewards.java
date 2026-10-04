package com.faefluffkrist.siftinggravel.advancement;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ArchaeologyRewards {
    public static void completed(ServerPlayer player, ItemStack tool) {
        if(tool.is(Items.BOWL)) ModCriteria.BOWL_ARCHAEOLOGY.trigger(player);
        else if(tool.is(Items.BRUSH)) ModCriteria.BRUSH_ARCHAEOLOGY.trigger(player);
    }
    public static void found(ServerPlayer player, ItemStack tool, ItemStack loot) {
        boolean bowl=tool.is(Items.BOWL), brush=tool.is(Items.BRUSH);
        if ((!bowl && !brush) || loot.isEmpty()) return;
        String path=BuiltInRegistries.ITEM.getKey(loot.getItem()).getPath();
        if(loot.is(ItemTags.DECORATED_POT_SHERDS)) (bowl?ModCriteria.BOWL_SHERD:ModCriteria.BRUSH_SHERD).trigger(player);
        if(path.endsWith("_armor_trim_smithing_template")) (bowl?ModCriteria.BOWL_TRIM:ModCriteria.BRUSH_TRIM).trigger(player);
        if(loot.is(Items.MUSIC_DISC_RELIC)) (bowl?ModCriteria.BOWL_RELIC:ModCriteria.BRUSH_RELIC).trigger(player);
    }
    private ArchaeologyRewards() {}
}
