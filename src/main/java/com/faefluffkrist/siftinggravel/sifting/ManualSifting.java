package com.faefluffkrist.siftinggravel.sifting;

import com.faefluffkrist.siftinggravel.SiftingGravel;
import com.faefluffkrist.siftinggravel.config.SiftingGravelConfig;
import com.faefluffkrist.siftinggravel.advancement.ModCriteria;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import com.faefluffkrist.siftinggravel.item.PrimitiveFireStarterItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manual bowl sifting.
 *
 * Hold use with a wooden bowl on a supported loose block until the sift
 * completes. The source block is consumed without its vanilla drop, then each
 * material rolls its own small set of finds. Rolls are independent, so a lucky
 * sift can plop out more than one item. It is also possible to find nothing.
 */
public final class ManualSifting {
    private static final int SIFT_TICKS = 35;
    // Vanilla brushable blocks accept one brush increment every 10 ticks.
    // The bowl only attempts one every 30 ticks, making archaeology about 3x slower.
    private static final int ARCHAEOLOGY_INTERVAL_TICKS = 30;
    private static final double REACH = 5.0D;
    private static final Map<UUID, SiftSession> SESSIONS = new HashMap<>();

    private ManualSifting() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            ItemStack held = player.getItemInHand(hand);
            BlockState clickedState = level.getBlockState(hitResult.getBlockPos());
            // A held use repeats after ignition; candles otherwise interpret it as extinguish.
            if (held.getItem() instanceof PrimitiveFireStarterItem
                    && (clickedState.getBlock() instanceof CandleBlock || clickedState.getBlock() instanceof CandleCakeBlock)
                    && clickedState.getValue(BlockStateProperties.LIT)) return InteractionResult.FAIL;
            if (held.is(Items.BRUSH) && blocksBrushFeedback(clickedState)) {
                return InteractionResult.FAIL;
            }
            if (!held.is(Items.BOWL) || !isBowlTarget(clickedState)) {
                return InteractionResult.PASS;
            }

            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                BlockState targetState = level.getBlockState(hitResult.getBlockPos());
                SESSIONS.put(serverPlayer.getUUID(), new SiftSession(hitResult.getBlockPos().immutable(), 0, hitResult.getDirection()));
                if (isSuspicious(targetState)) {
                    serverPlayer.sendSystemMessage(
                            Component.literal("I should use a brush instead... it would be quicker."),
                            true
                    );
                }
            }

            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickPlayer(player);
            }
        });
    }

    private static void tickPlayer(ServerPlayer player) {
        SiftSession session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }

        if (!player.isUsingItem() || !player.getUseItem().is(Items.BOWL)) {
            cancel(player);
            return;
        }

        HitResult ray = player.pick(REACH, 0.0F, false);
        if (!(ray instanceof BlockHitResult blockHit) || !blockHit.getBlockPos().equals(session.pos())) {
            cancel(player);
            return;
        }

        BlockState state = player.level().getBlockState(session.pos());
        if (isSuspicious(state)) {
            tickArchaeology(player, session, state);
            return;
        }
        if (!isSiftable(state)) {
            cancel(player);
            return;
        }

        int progress = session.ticks() + 1;
        emitSiftingFeedback(player, session.pos(), state, progress);
        if (progress < SIFT_TICKS) {
            SESSIONS.put(player.getUUID(), new SiftSession(session.pos(), progress, session.direction()));
            return;
        }

        // Consume the sifted block without its ordinary block loot.
        player.level().removeBlock(session.pos(), false);
        playCompletionFeedback(player, session.pos(), state);
        ModCriteria.MANUAL_SIFT.trigger(player);
        if (rollLoot(player, session.pos(), state)) {
            ModCriteria.MANUAL_FLINT.trigger(player);
        }

        player.stopUsingItem();
        SESSIONS.remove(player.getUUID());
    }


    private static void tickArchaeology(ServerPlayer player, SiftSession session, BlockState state) {
        int progress = session.ticks() + 1;
        if (progress % ARCHAEOLOGY_INTERVAL_TICKS == 0) {
            if (!(player.level() instanceof ServerLevel serverLevel)
                    || !(serverLevel.getBlockEntity(session.pos()) instanceof BrushableBlockEntity brushable)) {
                cancel(player);
                return;
            }

            // Delegate to vanilla archaeology. This preserves the suspicious block's
            // seeded structure/datapack loot instead of inventing a parallel table.
            boolean completed = brushable.brush(
                    serverLevel.getGameTime(),
                    serverLevel,
                    player,
                    session.direction(),
                    player.getUseItem()
            );
            if (completed) {
                player.stopUsingItem();
                SESSIONS.remove(player.getUUID());
                return;
            }
        }

        // If another system replaced the block while we were brushing, stop cleanly.
        if (!isSuspicious(player.level().getBlockState(session.pos()))) {
            cancel(player);
            return;
        }
        SESSIONS.put(player.getUUID(), new SiftSession(session.pos(), progress, session.direction()));
    }

    private static void emitSiftingFeedback(ServerPlayer player, BlockPos pos, BlockState state, int progress) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // A few crumbs while the bowl is moving through the material. Keep this
        // restrained so sifting reads clearly without becoming a particle fountain.
        if (progress % 5 == 0) {
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5D,
                    pos.getY() + 0.82D,
                    pos.getZ() + 0.5D,
                    3,
                    0.24D,
                    0.12D,
                    0.24D,
                    0.025D
            );
        }

        // Use the material's own hit sound rather than inventing a custom audio
        // language. Four quiet scrapes over a normal sift feels deliberate.
        if (progress % 10 == 0 && progress < SIFT_TICKS) {
            serverLevel.playSound(
                    null,
                    pos,
                    siftingSound(state),
                    SoundSource.BLOCKS,
                    0.35F,
                    0.92F + player.getRandom().nextFloat() * 0.16F
            );
        }
    }

    private static void playCompletionFeedback(ServerPlayer player, BlockPos pos, BlockState state) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.getX() + 0.5D,
                pos.getY() + 0.88D,
                pos.getZ() + 0.5D,
                8,
                0.30D,
                0.16D,
                0.30D,
                0.045D
        );
        serverLevel.playSound(null, pos, completionSound(state), SoundSource.BLOCKS, 0.55F, 1.0F);
    }

    private static SoundEvent siftingSound(BlockState state) {
        if (state.is(Blocks.GRAVEL)) return SoundEvents.GRAVEL_HIT;
        if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)) return SoundEvents.SAND_HIT;
        if (state.is(Blocks.SOUL_SAND)) return SoundEvents.SOUL_SAND_HIT;
        if (state.is(Blocks.SOUL_SOIL)) return SoundEvents.SOUL_SOIL_HIT;
        return SoundEvents.GRASS_HIT;
    }

    private static SoundEvent completionSound(BlockState state) {
        if (state.is(Blocks.GRAVEL)) return SoundEvents.GRAVEL_BREAK;
        if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)) return SoundEvents.SAND_BREAK;
        if (state.is(Blocks.SOUL_SAND)) return SoundEvents.SOUL_SAND_BREAK;
        if (state.is(Blocks.SOUL_SOIL)) return SoundEvents.SOUL_SOIL_BREAK;
        return SoundEvents.GRASS_BREAK;
    }

    private static boolean rollLoot(ServerPlayer player, BlockPos pos, BlockState state) {
        SiftingGravelConfig config = SiftingGravelConfig.get();
        SiftingGravelConfig.BlockConfig blockConfig = null;
        if (state.is(Blocks.GRAVEL)) blockConfig = config.gravel;
        else if (state.is(Blocks.COARSE_DIRT)) blockConfig = config.coarseDirt;
        else if (state.is(Blocks.ROOTED_DIRT)) blockConfig = config.rootedDirt;
        else if (state.is(Blocks.SAND)) blockConfig = config.sand;
        else if (state.is(Blocks.RED_SAND)) blockConfig = config.redSand;
        else if (state.is(Blocks.SOUL_SAND)) blockConfig = config.soulSand;
        else if (state.is(Blocks.SOUL_SOIL)) blockConfig = config.soulSoil;
        if (blockConfig == null) return false;

        RandomSource random = player.getRandom();
        boolean foundFlint = false;
        for (String entry : blockConfig.loot) {
            if (entry == null || entry.isBlank()) continue;
            int equals = entry.lastIndexOf('=');
            if (equals <= 0 || equals == entry.length() - 1) {
                SiftingGravel.LOGGER.warn("Ignoring invalid sifting loot entry '{}'. Expected mod:item=percent", entry);
                continue;
            }
            try {
                Identifier id = Identifier.parse(entry.substring(0, equals).trim());
                double percent = Double.parseDouble(entry.substring(equals + 1).trim());
                if (percent <= 0.0D) continue;
                if (!BuiltInRegistries.ITEM.containsKey(id)) {
                    SiftingGravel.LOGGER.warn("Ignoring unknown sifting loot item '{}'.", id);
                    continue;
                }
                Item item = BuiltInRegistries.ITEM.get(id).map(holder -> holder.value()).orElse(null);
                boolean dropped = roll(player, pos, random, item, Math.min(percent, 100.0D) / 100.0D);
                if (dropped && item == Items.FLINT) foundFlint = true;
                if (dropped) awardFind(player, state, item);
            } catch (Exception e) {
                SiftingGravel.LOGGER.warn("Ignoring invalid sifting loot entry '{}'. Expected mod:item=percent", entry);
            }
        }
        return foundFlint;
    }

    private static void awardFind(ServerPlayer player, BlockState source, Item item) {
        if (item == Items.WITHER_SKELETON_SKULL && (source.is(Blocks.SOUL_SAND) || source.is(Blocks.SOUL_SOIL)))
            ModCriteria.WITHER_SKULL_FIND.trigger(player);
        if (item == Items.GOLD_NUGGET && source.is(Blocks.RED_SAND)) ModCriteria.RED_SAND_GOLD.trigger(player);
        if (item == Items.NAUTILUS_SHELL && source.is(Blocks.SAND)) ModCriteria.SAND_SHELL.trigger(player);
        if ((item == Items.RED_MUSHROOM || item == Items.BROWN_MUSHROOM)
                && (source.is(Blocks.ROOTED_DIRT) || source.is(Blocks.COARSE_DIRT)))
            ModCriteria.DIRT_MUSHROOM.trigger(player);
    }

    private static boolean roll(ServerPlayer player, BlockPos pos, RandomSource random, Item item, double chance) {
        if (random.nextDouble() < chance) {
            return plop(player, pos, new ItemStack(item));
        }
        return false;
    }

    private static boolean plop(ServerPlayer player, BlockPos pos, ItemStack stack) {
        RandomSource random = player.getRandom();
        ItemEntity drop = new ItemEntity(
                player.level(),
                pos.getX() + 0.5D,
                pos.getY() + 1.15D,
                pos.getZ() + 0.5D,
                stack
        );

        // A restrained dispenser-like plop. Enough movement to make the find
        // visible without firing somebody's nautilus shell into another county.
        double x = (random.nextDouble() - 0.5D) * 0.12D;
        double z = (random.nextDouble() - 0.5D) * 0.12D;
        drop.setDeltaMovement(x, 0.18D, z);
        return player.level().addFreshEntity(drop);
    }

    private static void cancel(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
        if (player.isUsingItem() && player.getUseItem().is(Items.BOWL)) {
            player.stopUsingItem();
        }
    }

    public static boolean blocksBrushFeedback(BlockState state) {
        return state.is(Blocks.SAND) || state.is(Blocks.GRAVEL)
                || (isSiftable(state) && !isSuspicious(state));
    }

    private static boolean isBowlTarget(BlockState state) {
        return isSiftable(state) || isSuspicious(state);
    }

    private static boolean isSuspicious(BlockState state) {
        SiftingGravelConfig c = SiftingGravelConfig.get();
        return (c.suspiciousSandEnabled && state.is(Blocks.SUSPICIOUS_SAND))
                || (c.suspiciousGravelEnabled && state.is(Blocks.SUSPICIOUS_GRAVEL));
    }

    private static boolean isSiftable(BlockState state) {
        SiftingGravelConfig c = SiftingGravelConfig.get();
        return (c.gravel.enabled && state.is(Blocks.GRAVEL))
                || (c.coarseDirt.enabled && state.is(Blocks.COARSE_DIRT))
                || (c.rootedDirt.enabled && state.is(Blocks.ROOTED_DIRT))
                || (c.sand.enabled && state.is(Blocks.SAND))
                || (c.redSand.enabled && state.is(Blocks.RED_SAND))
                || (c.soulSand.enabled && state.is(Blocks.SOUL_SAND))
                || (c.soulSoil.enabled && state.is(Blocks.SOUL_SOIL));
    }

    private record SiftSession(BlockPos pos, int ticks, net.minecraft.core.Direction direction) {}
}
