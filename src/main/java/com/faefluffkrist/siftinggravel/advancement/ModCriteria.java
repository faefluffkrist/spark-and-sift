package com.faefluffkrist.siftinggravel.advancement;

import com.faefluffkrist.siftinggravel.SiftingGravel;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class ModCriteria {
    public static final SiftSparkCriterion MANUAL_SIFT = register("manual_sift", new SiftSparkCriterion());
    public static final SiftSparkCriterion MANUAL_FLINT = register("manual_flint", new SiftSparkCriterion());
    public static final SiftSparkCriterion QUICK_SIFT = register("quick_sift", new SiftSparkCriterion());
    public static final SiftSparkCriterion FIRE_DRILL_SUCCESS = register("fire_drill_success", new SiftSparkCriterion());
    public static final SiftSparkCriterion BOWL_ARCHAEOLOGY = register("bowl_archaeology", new SiftSparkCriterion());
    public static final SiftSparkCriterion SIFTED_CRAFTING = register("sifted_crafting", new SiftSparkCriterion());

    public static final SiftSparkCriterion WITHER_SKULL_FIND = register("wither_skull_find", new SiftSparkCriterion());
    public static final SiftSparkCriterion RED_SAND_GOLD = register("red_sand_gold", new SiftSparkCriterion());
    public static final SiftSparkCriterion SAND_SHELL = register("sand_shell", new SiftSparkCriterion());
    public static final SiftSparkCriterion DIRT_MUSHROOM = register("dirt_mushroom", new SiftSparkCriterion());

    public static final SiftSparkCriterion FLINT_STARTER_SUCCESS = register("flint_starter_success", new SiftSparkCriterion());
    public static final SiftSparkCriterion SOUL_FIRE_SUCCESS = register("soul_fire_success", new SiftSparkCriterion());
    public static final SiftSparkCriterion CAMPFIRE_COOK = register("campfire_cook", new SiftSparkCriterion());
    public static final SiftSparkCriterion SOUL_CAMPFIRE_COOK = register("soul_campfire_cook", new SiftSparkCriterion());
    public static final SiftSparkCriterion BRUSH_ARCHAEOLOGY = register("brush_archaeology", new SiftSparkCriterion());
    public static final SiftSparkCriterion BOWL_SHERD = register("bowl_sherd", new SiftSparkCriterion());
    public static final SiftSparkCriterion BOWL_TRIM = register("bowl_trim", new SiftSparkCriterion());
    public static final SiftSparkCriterion BOWL_RELIC = register("bowl_relic", new SiftSparkCriterion());
    public static final SiftSparkCriterion BRUSH_SHERD = register("brush_sherd", new SiftSparkCriterion());
    public static final SiftSparkCriterion BRUSH_TRIM = register("brush_trim", new SiftSparkCriterion());
    public static final SiftSparkCriterion BRUSH_RELIC = register("brush_relic", new SiftSparkCriterion());

    private static <T extends CriterionTrigger<?>> T register(String name, T trigger) {
        return Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                Identifier.fromNamespaceAndPath(SiftingGravel.MOD_ID, name), trigger);
    }

    public static void initialize() {}
    private ModCriteria() {}
}
