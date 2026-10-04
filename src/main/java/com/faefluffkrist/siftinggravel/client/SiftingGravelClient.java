package com.faefluffkrist.siftinggravel.client;

import com.faefluffkrist.siftinggravel.config.SiftingGravelConfig;
import com.faefluffkrist.siftinggravel.item.ModItems;
import net.fabricmc.api.ClientModInitializer;
import com.faefluffkrist.siftinggravel.network.IgnitionConfigSync;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class SiftingGravelClient implements ClientModInitializer {
    private static final Identifier QUICK_SIFT_MODEL =
            Identifier.fromNamespaceAndPath("siftinggravel", "quick_sift");
    private static Component quickSiftWarning = null;
    private static int warningTicks = 0;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(IgnitionConfigSync.Payload.TYPE,
                (payload, context) -> context.client().execute(() -> IgnitionConfigSync.setClientSetting(payload.enabled())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> IgnitionConfigSync.setClientSetting(null));
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (stack.is(Items.BOWL)) {
                lines.add(Component.literal("  Can sift various dirts and minerals.  ")
                        .withStyle(ChatFormatting.GRAY));
                return;
            }
            if (stack.is(Items.BRUSH)) {
                lines.add(Component.literal("  Excavates suspicious blocks faster than a Bowl.  ")
                        .withStyle(ChatFormatting.GRAY));
                return;
            }
            if (stack.is(ModItems.FIRE_DRILL)) {
                SiftingGravelConfig.FireStarterConfig f = SiftingGravelConfig.get().fireDrill;
                lines.add(Component.literal(IgnitionConfigSync.allowed(Minecraft.getInstance().level)
                        ? "  Used to light fires and other ignition targets.  " : "  Used to light unlit Campfires.  ").withStyle(ChatFormatting.GRAY));
                lines.add(Component.literal("  Base: 1 Stick  ").withStyle(ChatFormatting.YELLOW));
                lines.add(Component.literal("  Pair with: " + friendlyIngredients(f.ingredients) + "  ").withStyle(ChatFormatting.DARK_GRAY));
                lines.add(Component.literal("  Crude and unreliable • 1 successful fire  ").withStyle(ChatFormatting.DARK_GRAY));
                return;
            }
            if (stack.is(ModItems.FLINT_FIRE_STARTER)) {
                SiftingGravelConfig.FireStarterConfig f = SiftingGravelConfig.get().flintFireStarter;
                lines.add(Component.literal(IgnitionConfigSync.allowed(Minecraft.getInstance().level)
                        ? "  Used to light fires and other ignition targets.  " : "  Used to light unlit Campfires.  ").withStyle(ChatFormatting.GRAY));
                lines.add(Component.literal("  Base: 2 Flint  ").withStyle(ChatFormatting.YELLOW));
                lines.add(Component.literal("  Pair with: " + friendlyIngredients(f.ingredients) + "  ").withStyle(ChatFormatting.DARK_GRAY));
                lines.add(Component.literal("  Unreliable • 3 successful fires  ").withStyle(ChatFormatting.DARK_GRAY));
                return;
            }
            if (!isQuickSift(stack)) return;

            List<Component> extra = new ArrayList<>();
            extra.add(Component.literal("Functions as Flint in crafting recipes.")
                    .withStyle(ChatFormatting.AQUA));
            extra.add(Component.literal("Can also be crafted 1:1 into regular Flint.")
                    .withStyle(ChatFormatting.AQUA));
            extra.add(Component.empty());
            extra.add(Component.literal("Quick Sifting is faster, but forfeits")
                    .withStyle(ChatFormatting.GRAY));
            extra.add(Component.literal("the chance for additional items...")
                    .withStyle(ChatFormatting.GRAY));
            extra.add(Component.literal("In your haste, you incautiously discard")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
            extra.add(Component.literal("the finer remnants.")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
            extra.add(Component.empty());

            SiftingGravelConfig c = SiftingGravelConfig.get();
            List<String> manualOnly = new ArrayList<>();
            if (c.sand.enabled) manualOnly.add("Sand");
            if (c.redSand.enabled) manualOnly.add("Red Sand");
            if (c.suspiciousSandEnabled && c.suspiciousGravelEnabled) manualOnly.add("Suspicious Sand/Gravel");
            else if (c.suspiciousSandEnabled) manualOnly.add("Suspicious Sand");
            else if (c.suspiciousGravelEnabled) manualOnly.add("Suspicious Gravel");
            if (c.soulSand.enabled) manualOnly.add("Soul Sand");
            if (c.soulSoil.enabled) manualOnly.add("Soul Soil");

            if (!manualOnly.isEmpty()) {
                extra.add(Component.literal("I may not be able to Quick Sift them,")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
                extra.add(Component.literal("but I should be able to manually sift...")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
                for (int i = 0; i < manualOnly.size(); i += 3) {
                    extra.add(Component.literal(String.join(", ",
                                    manualOnly.subList(i, Math.min(i + 3, manualOnly.size()))))
                            .withStyle(ChatFormatting.GRAY));
                }
            } else {
                List<String> better = new ArrayList<>();
                if (c.gravel.enabled) better.add("Gravel");
                if (c.coarseDirt.enabled) better.add("Coarse Dirt");
                if (c.rootedDirt.enabled) better.add("Rooted Dirt");
                if (!better.isEmpty()) {
                    String joined = better.size() == 1 ? better.getFirst()
                            : better.size() == 2 ? String.join(" and ", better)
                            : String.join(", ", better.subList(0, better.size() - 1)) + ", and " + better.getLast();
                    extra.add(Component.literal("Manually sifting " + joined + " may yield better results.")
                            .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
                }
            }

            lines.addAll(Math.min(1, lines.size()), extra);
        });

        ClientTickEvents.END_CLIENT_TICK.register(SiftingGravelClient::tickDisabledQuickSiftWarning);
        ClientTickEvents.END_CLIENT_TICK.register(FlintIgnitionAnimationStop::tick);

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("siftinggravel", "quick_sift_warning"),
                (graphics, deltaTracker) -> {
                    if (quickSiftWarning == null || warningTicks <= 0) return;
                    Minecraft client = Minecraft.getInstance();
                    int x = graphics.guiWidth() / 2;
                    int y = graphics.guiHeight() - 68;
                    int alpha = warningTicks > 40 ? 255 : Math.max(0, warningTicks * 255 / 40);
                    int color = (alpha << 24) | 0x00FF7777;
                    graphics.centeredText(client.font, quickSiftWarning, x, y, color);
                }
        );
    }

    private static boolean isQuickSift(ItemStack stack) {
        if (!stack.is(Items.FLINT)) return false;
        Identifier model = stack.get(DataComponents.ITEM_MODEL);
        return QUICK_SIFT_MODEL.equals(model);
    }

    private static void tickDisabledQuickSiftWarning(Minecraft client) {
        if (warningTicks > 0) {
            warningTicks--;
            if (warningTicks == 0) quickSiftWarning = null;
        }
        if (client.player == null) return;

        int first;
        int last;
        if (client.player.containerMenu instanceof InventoryMenu) {
            first = 1;
            last = 4;
        } else if (client.player.containerMenu instanceof CraftingMenu) {
            first = 1;
            last = 9;
        } else {
            return;
        }

        int bowls = 0;
        int gravelSlots = 0;
        int coarseSlots = 0;
        int rootedSlots = 0;
        int occupied = 0;

        for (int i = first; i <= last && i < client.player.containerMenu.slots.size(); i++) {
            ItemStack stack = client.player.containerMenu.getSlot(i).getItem();
            if (stack.isEmpty()) continue;
            occupied++;
            if (stack.is(Items.BOWL)) bowls++;
            else if (stack.is(Items.GRAVEL)) gravelSlots++;
            else if (stack.is(Items.COARSE_DIRT)) coarseSlots++;
            else if (stack.is(Items.ROOTED_DIRT)) rootedSlots++;
            else return;
        }

        if (occupied != 4 || bowls != 1) return;

        SiftingGravelConfig c = SiftingGravelConfig.get();
        String material = null;
        if (gravelSlots == 3 && !c.gravel.enabled) material = "Gravel";
        else if (coarseSlots == 3 && !c.coarseDirt.enabled) material = "Coarse Dirt";
        else if (rootedSlots == 3 && !c.rootedDirt.enabled) material = "Rooted Dirt";
        if (material == null) return;

        quickSiftWarning = Component.literal("I don't recall being able to Quick Sift " + material + ".");
        warningTicks = 100;
    }
    private static String friendlyIngredients(List<String> ids) {
        List<String> names = new ArrayList<>();
        for (String raw : ids) {
            if (raw == null || raw.isBlank()) continue;
            String v = raw.trim();
            if (v.equals("#minecraft:leaves")) { names.add("Leaves"); continue; }
            int colon = v.indexOf(':'); if (colon >= 0) v = v.substring(colon + 1);
            if (v.startsWith("#")) v = v.substring(1);
            String[] words = v.split("_"); StringBuilder b = new StringBuilder();
            for (String w : words) { if (!w.isEmpty()) { if (b.length() > 0) b.append(' '); b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)); } }
            names.add(b.toString());
        }
        return names.isEmpty() ? "Nothing configured" : String.join(", ", names);
    }

}
