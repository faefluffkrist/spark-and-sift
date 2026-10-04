package com.faefluffkrist.siftinggravel;

import com.faefluffkrist.siftinggravel.config.SiftingGravelConfig;
import com.faefluffkrist.siftinggravel.advancement.ModCriteria;
import com.faefluffkrist.siftinggravel.recipe.CoarseDirtSiftingRecipe;
import com.faefluffkrist.siftinggravel.recipe.GravelSiftingRecipe;
import com.faefluffkrist.siftinggravel.recipe.RootedDirtSiftingRecipe;
import com.faefluffkrist.siftinggravel.recipe.UnpackSiftedFlintRecipe;
import com.faefluffkrist.siftinggravel.recipe.FireDrillRecipe;
import com.faefluffkrist.siftinggravel.recipe.FlintFireStarterRecipe;
import com.faefluffkrist.siftinggravel.sifting.ManualSifting;
import com.faefluffkrist.siftinggravel.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SiftingGravel implements ModInitializer {
    public static final String MOD_ID = "siftinggravel";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        SiftingGravelConfig.load();
        ModItems.initialize();
        ModCriteria.initialize();
        com.faefluffkrist.siftinggravel.network.IgnitionConfigSync.initialize();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> SiftingGravelConfig.load());
        Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(MOD_ID, "gravel_sifting"),
                GravelSiftingRecipe.SERIALIZER
        );
        Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(MOD_ID, "coarse_dirt_sifting"),
                CoarseDirtSiftingRecipe.SERIALIZER
        );
        Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(MOD_ID, "rooted_dirt_sifting"),
                RootedDirtSiftingRecipe.SERIALIZER
        );
        Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(MOD_ID, "unpack_sifted_flint"),
                UnpackSiftedFlintRecipe.SERIALIZER
        );
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Identifier.fromNamespaceAndPath(MOD_ID, "fire_drill"), FireDrillRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Identifier.fromNamespaceAndPath(MOD_ID, "flint_fire_starter"), FlintFireStarterRecipe.SERIALIZER);

        ManualSifting.register();

        LOGGER.info("Sifting Gravel initialized with reusable-bowl recipes and M5 material loot tables, Soul Soil, and slow bowl archaeology.");
    }
}
