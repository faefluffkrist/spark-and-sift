package com.faefluffkrist.siftinggravel.item;

import net.minecraft.ChatFormatting;
import com.faefluffkrist.siftinggravel.network.IgnitionConfigSync;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import com.faefluffkrist.siftinggravel.config.SiftingGravelConfig;
import com.faefluffkrist.siftinggravel.advancement.ModCriteria;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PrimitiveFireStarterItem extends Item {
    public enum Tier { FIRE_DRILL, FLINT_STARTER }

    private record Attempt(BlockPos pos, Tier tier, int count, long gameTime) {}
    private static final Map<UUID, Attempt> ATTEMPTS = new HashMap<>();
    private static final Map<UUID, IgnitionTarget> PENDING_DRILL = new HashMap<>();
    private static final Map<UUID, IgnitionTarget> PENDING_FLINT = new HashMap<>();
    private static final int FIRE_DRILL_USE_TICKS = 8;
    private static final int FLINT_STARTER_USE_TICKS = 14;

    public enum IgnitionKind { LIGHT_BLOCK, PLACE_FIRE, TNT }
    public record IgnitionTarget(BlockPos clicked, BlockPos pos, Direction face,
                                 Direction horizontal, IgnitionKind kind, BlockState original) {}

    /** Resolve the same legal vanilla fire targets on client and server. */
    public IgnitionTarget ignitionTarget(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !config().enabled) return null;
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockState state = level.getBlockState(clicked);
        boolean broad = IgnitionConfigSync.allowed(level);
        IgnitionKind kind;
        BlockPos pos = clicked;
        if (CampfireBlock.canLight(state)
                || (broad && (CandleBlock.canLight(state) || CandleCakeBlock.canLight(state)))) {
            kind = IgnitionKind.LIGHT_BLOCK;
        } else if (broad && state.is(Blocks.TNT)) {
            kind = IgnitionKind.TNT;
        } else if (broad) {
            pos = clicked.relative(context.getClickedFace());
            if (!BaseFireBlock.canBePlacedAt(level, pos, context.getHorizontalDirection())) return null;
            kind = IgnitionKind.PLACE_FIRE;
        } else return null;
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) return null;
        return new IgnitionTarget(clicked.immutable(), pos.immutable(), context.getClickedFace(),
                context.getHorizontalDirection(), kind, level.getBlockState(pos));
    }

    private boolean validTarget(Level level, IgnitionTarget target) {
        BlockState state = level.getBlockState(target.pos());
        if (!state.equals(target.original()) || !config().enabled) return false;
        return switch (target.kind()) {
            case LIGHT_BLOCK -> CampfireBlock.canLight(state) || (IgnitionConfigSync.allowed(level)
                    && (CandleBlock.canLight(state) || CandleCakeBlock.canLight(state)));
            case PLACE_FIRE -> IgnitionConfigSync.allowed(level)
                    && BaseFireBlock.canBePlacedAt(level, target.pos(), target.horizontal());
            case TNT -> IgnitionConfigSync.allowed(level) && state.is(Blocks.TNT);
        };
    }

    private final Tier tier;

    public PrimitiveFireStarterItem(Tier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        IgnitionTarget target = ignitionTarget(context);
        if (player == null || target == null) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            if (tier == Tier.FIRE_DRILL) PENDING_DRILL.put(player.getUUID(), target);
            else PENDING_FLINT.put(player.getUUID(), target);
        }
        player.startUsingItem(context.getHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return tier == Tier.FIRE_DRILL ? FIRE_DRILL_USE_TICKS : FLINT_STARTER_USE_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        // NONE keeps the hand in our authored Blockbench pose instead of applying
        // bow/eat/block transforms. The model itself supplies the drill motion.
        return ItemUseAnimation.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseTicks) {
        if (level.isClientSide() || !(entity instanceof Player player)) return;

        int duration = getUseDuration(stack, entity);
        int elapsed = duration - remainingUseTicks;
        IgnitionTarget target = tier == Tier.FIRE_DRILL
                ? PENDING_DRILL.get(player.getUUID())
                : PENDING_FLINT.get(player.getUUID());
        if (target == null || !(level instanceof ServerLevel server)) return;
        BlockPos pos = target.pos();

        if (tier == Tier.FIRE_DRILL) {
            if (elapsed == 2 || elapsed == 5) {
                server.playSound(null, pos, SoundEvents.WOOD_STEP, SoundSource.PLAYERS,
                        elapsed == 2 ? 0.22F : 0.26F,
                        elapsed == 2 ? 1.72F : 1.82F);

                // Approximate the drill's visible spindle/contact pivot over the campfire.
                // Server particles live in world-space, so this intentionally marks the
                // action point rather than pretending we can bind them to model-space bones.
                server.sendParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 0.72, pos.getZ() + 0.5,
                        2, 0.035, 0.018, 0.035, 0.0015);
                server.sendParticles(ParticleTypes.POOF,
                        pos.getX() + 0.5, pos.getY() + 0.70, pos.getZ() + 0.5,
                        1, 0.025, 0.012, 0.025, 0.002);
            }
        } else {
            // Flint starter: audiovisual strike beats only. Gameplay resolves exactly once
            // when the 14-tick use completes, keeping visuals and attempt timing deterministic.
            if (elapsed == 8) {
                server.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS,
                        0.36F, 1.48F);
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        pos.getX() + 0.5, pos.getY() + 0.78, pos.getZ() + 0.5,
                        7, 0.11, 0.050, 0.11, 0.038);
                server.sendParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5,
                        2, 0.060, 0.025, 0.060, 0.003);
            } else if (elapsed == 9 || elapsed == 10) {
                // Lingering contact-point particles, deliberately much lighter
                // than the initial strike burst.
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        pos.getX() + 0.5, pos.getY() + 0.77, pos.getZ() + 0.5,
                        2, 0.075, 0.035, 0.075, 0.020);
                server.sendParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5,
                        1, 0.040, 0.020, 0.040, 0.002);
            }

        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player && !level.isClientSide()) {
            IgnitionTarget target = tier == Tier.FIRE_DRILL
                    ? PENDING_DRILL.remove(player.getUUID())
                    : PENDING_FLINT.remove(player.getUUID());
            if (target != null && validTarget(level, target)
                    && level.mayInteract(player, target.pos())
                    && player.mayUseItemAt(target.pos(), target.face(), stack)) {
                resolveAttempt((ServerLevel) level, target, player, stack);
            }
        }
        return stack;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingUseTicks) {
        if (!level.isClientSide()) {
            if (tier == Tier.FIRE_DRILL) PENDING_DRILL.remove(entity.getUUID());
            else PENDING_FLINT.remove(entity.getUUID());
        }
        return false;
    }

    private void resolveAttempt(ServerLevel server, IgnitionTarget target, Player player, ItemStack stack) {
        Level level = server;
        BlockPos pos = target.pos();
        if (!validTarget(level, target)) return;
        // Honour vanilla's TNT switch before any success roll or durability loss.
        if (target.kind() == IgnitionKind.TNT && !server.getGameRules().get(GameRules.TNT_EXPLODES)) return;

        SiftingGravelConfig.FireStarterConfig cfg = config();
        long now = level.getGameTime();
        Attempt previous = ATTEMPTS.get(player.getUUID());
        int attempt = 1;
        if (previous != null && previous.tier == tier && previous.pos.equals(pos) && now - previous.gameTime <= 100) {
            attempt = previous.count + 1;
        }
        ATTEMPTS.put(player.getUUID(), new Attempt(pos.immutable(), tier, attempt, now));

        boolean success = level.getRandom().nextFloat() < successChance(attempt) || attempt >= guaranteedAttempt();
        if (!success) {
            playFailure(server, pos, attempt);
            java.util.List<String> thoughts = cfg.failureMessages;
            if (!thoughts.isEmpty()) player.sendOverlayMessage(Component.literal(thoughts.get((attempt - 1) % thoughts.size()))
                    .withStyle(ChatFormatting.WHITE));
            return;
        }

        if (!ignite(server, target, player)) return;
        ATTEMPTS.remove(player.getUUID());
        server.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS,
                tier == Tier.FIRE_DRILL ? 0.62F : 0.62F,
                tier == Tier.FIRE_DRILL ? 0.82F : 1.08F);
        server.sendParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                5, 0.18, 0.08, 0.18, 0.015);

        if (!cfg.successMessages.isEmpty()) player.sendOverlayMessage(Component.literal(
                cfg.successMessages.get(level.getRandom().nextInt(cfg.successMessages.size())))
                .withStyle(ChatFormatting.WHITE));
        if (player instanceof ServerPlayer serverPlayer) {
            if (tier == Tier.FIRE_DRILL) ModCriteria.FIRE_DRILL_SUCCESS.trigger(serverPlayer);
            else ModCriteria.FLINT_STARTER_SUCCESS.trigger(serverPlayer);
            if (target.original().is(Blocks.SOUL_CAMPFIRE)) ModCriteria.SOUL_FIRE_SUCCESS.trigger(serverPlayer);
        }
        damageOnSuccessfulLight(stack, player, server, pos);
    }

    private boolean ignite(ServerLevel level, IgnitionTarget target, Player player) {
        BlockPos pos = target.pos();
        if (!validTarget(level, target)) return false;
        switch (target.kind()) {
            case LIGHT_BLOCK -> {
                if (!level.setBlock(pos, target.original().setValue(BlockStateProperties.LIT, true), 11)) return false;
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            case PLACE_FIRE -> {
                if (!level.setBlock(pos, BaseFireBlock.getState(level, pos), 11)) return false;
                level.gameEvent(player, GameEvent.BLOCK_PLACE, pos);
            }
            case TNT -> {
                if (!level.getGameRules().get(GameRules.TNT_EXPLODES)) return false;
                PrimedTnt tnt = new PrimedTnt(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player);
                if (!level.addFreshEntity(tnt)) return false;
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
                level.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.TNT_PRIMED,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(player, GameEvent.PRIME_FUSE, pos);
            }
        }
        return true;
    }

    private SiftingGravelConfig.FireStarterConfig config() {
        SiftingGravelConfig c = SiftingGravelConfig.get();
        return tier == Tier.FIRE_DRILL ? c.fireDrill : c.flintFireStarter;
    }

    private float successChance(int attempt) {
        java.util.List<Integer> chances = config().chances;
        if (chances.isEmpty()) return 1.0F;
        int index = Math.min(Math.max(attempt - 1, 0), chances.size() - 1);
        return Math.max(0, Math.min(100, chances.get(index))) / 100.0F;
    }

    private int guaranteedAttempt() {
        return Math.max(1, config().chances.size());
    }

    private void playFailure(ServerLevel level, BlockPos pos, int attempt) {
        if (tier == Tier.FIRE_DRILL) {
            level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS,
                    0.24F, 1.42F + (attempt % 3) * 0.04F);
        } else {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
                    0.12F, 1.65F);
        }
        int smokeCount = tier == Tier.FIRE_DRILL ? (attempt >= 5 ? 3 : 2) : (attempt >= 2 ? 2 : 1);
        level.sendParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5,
                smokeCount, 0.13, 0.055, 0.13, 0.008);
    }

    private void damageOnSuccessfulLight(ItemStack stack, Player player, ServerLevel level, BlockPos pos) {
        if (player.getAbilities().instabuild) return;
        int next = stack.getDamageValue() + 1;
        if (next >= stack.getMaxDamage()) {
            level.playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.85F, 1.0F);
            stack.shrink(1);
        } else {
            stack.setDamageValue(next);
        }
    }
}
