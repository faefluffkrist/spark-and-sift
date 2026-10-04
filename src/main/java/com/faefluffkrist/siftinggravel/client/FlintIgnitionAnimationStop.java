package com.faefluffkrist.siftinggravel.client;

import com.faefluffkrist.siftinggravel.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import com.faefluffkrist.siftinggravel.item.PrimitiveFireStarterItem;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;

/** Cancels only our Flint pose after authoritative block and inventory updates. */
public final class FlintIgnitionAnimationStop {
    private record Pending(PrimitiveFireStarterItem.IgnitionTarget target, InteractionHand hand, int damage, int count,
                           boolean creative, int entityId) {}
    private static Pending pending;
    private static int ticksLeft;
    private static Object trackedLevel;
    private static boolean unavailable;

    private FlintIgnitionAnimationStop() {}

    public static void begin(PrimitiveFireStarterItem.IgnitionTarget target, InteractionHand hand, ItemStack stack) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || target == null) return;
        pending = new Pending(target, hand, stack.getDamageValue(), stack.getCount(),
                client.player.getAbilities().instabuild, client.player.getId());
        trackedLevel = client.level;
        ticksLeft = 40;
    }

    public static void tick(Minecraft client) {
        if (pending == null) return;
        if (client.player == null || client.level == null || client.level != trackedLevel
                || client.player.getId() != pending.entityId() || --ticksLeft <= 0) {
            pending = null;
            trackedLevel = null;
            return;
        }
        var state = client.level.getBlockState(pending.target().pos());
        boolean ignited = switch(pending.target().kind()) {
            case LIGHT_BLOCK -> state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
            case PLACE_FIRE -> state.getBlock() instanceof BaseFireBlock || state.getBlock() instanceof NetherPortalBlock;
            case TNT -> state.isAir();
        };
        if (!ignited) return;
        ItemStack held = client.player.getItemInHand(pending.hand());
        // Block updates and inventory updates can arrive separately. Wait for both.
        boolean paidDurability = held.is(ModItems.FLINT_FIRE_STARTER)
                && (held.getDamageValue() > pending.damage() || held.getCount() < pending.count());
        boolean broke = held.isEmpty() && pending.damage() + 1 >= ModItems.FLINT_FIRE_STARTER
                .getDefaultInstance().getMaxDamage();
        if (!pending.creative() && !paidDurability && !broke) return;
        if (client.player.isUsingItem() && client.player.getUsedItemHand() == pending.hand()
                && client.player.getUseItem().is(ModItems.FLINT_FIRE_STARTER)) {
            client.player.stopUsingItem();
        }
        pending = null;
        trackedLevel = null;
        stopPunchyFlintPose();
    }

    private static void stopPunchyFlintPose() {
        if (unavailable) return;
        try {
            // Optional bridge: no Punchy dependency or server-side Punchy class loading.
            Class<?> manager = Class.forName("punchy.client.animation.PunchyAnimationManager");
            stopMatchingPose(manager.getField("POSE_HANDLER").get(null));
            Class<?> items = Class.forName("punchy.client.animation.ItemAnimationManager");
            Field handlers = items.getDeclaredField("HANDLERS");
            handlers.setAccessible(true);
            for (Object handler : ((Map<?, ?>) handlers.get(null)).values()) stopMatchingPose(handler);
        } catch (ClassNotFoundException absent) {
            unavailable = true;
        } catch (ReflectiveOperationException | RuntimeException incompatible) {
            unavailable = true;
            LoggerFactory.getLogger("siftinggravel").warn(
                    "Punchy Flint ignition animation stop is unavailable for this Punchy version", incompatible);
        }
    }

    private static void stopMatchingPose(Object handler) throws ReflectiveOperationException {
        if (handler == null) return;
        Class<?> type = handler.getClass();
        Object clip = type.getMethod("getCurrentClip").invoke(handler);
        if (clip == null) return;
        String name = (String) clip.getClass().getMethod("getName").invoke(clip);
        if (!"siftandspark_flint_starter_use".equals(name)
                && !"animation.siftandspark_flint_starter_use".equals(name)) return;
        type.getMethod("setLooping", boolean.class).invoke(handler, false);
        // Freeze the strike and use Punchy's existing smooth return-to-idle path.
        type.getMethod("stopImmediateWithBlendOut").invoke(handler);
    }
}
