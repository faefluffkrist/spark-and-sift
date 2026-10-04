package com.faefluffkrist.siftinggravel.item;

import com.faefluffkrist.siftinggravel.SiftingGravel;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public final class ModItems {
    public static final ResourceKey<Item> FIRE_DRILL_KEY = key("fire_drill");
    public static final ResourceKey<Item> FLINT_FIRE_STARTER_KEY = key("flint_fire_starter");

    public static final Item FIRE_DRILL = register(FIRE_DRILL_KEY,
            p -> new PrimitiveFireStarterItem(PrimitiveFireStarterItem.Tier.FIRE_DRILL, p),
            new Item.Properties().durability(1));

    public static final Item FLINT_FIRE_STARTER = register(FLINT_FIRE_STARTER_KEY,
            p -> new PrimitiveFireStarterItem(PrimitiveFireStarterItem.Tier.FLINT_STARTER, p),
            new Item.Properties().durability(3));

    private static ResourceKey<Item> key(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SiftingGravel.MOD_ID, path));
    }

    private static Item register(ResourceKey<Item> key, Function<Item.Properties, Item> factory, Item.Properties properties) {
        Item item = factory.apply(properties.setId(key));
        Registry.register(BuiltInRegistries.ITEM, key, item);
        return item;
    }

    public static void initialize() {
    }

    private ModItems() {}
}
