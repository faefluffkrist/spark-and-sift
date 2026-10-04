package com.faefluffkrist.siftinggravel.advancement;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;

public interface CampfireCookTracking {
    void siftinggravel$beginCook();
    void siftinggravel$cookedResult(ItemStack result);
    void siftinggravel$finishCook(ServerLevel level, BlockState state);
}
