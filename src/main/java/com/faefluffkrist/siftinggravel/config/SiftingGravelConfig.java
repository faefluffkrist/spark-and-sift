package com.faefluffkrist.siftinggravel.config;

import com.faefluffkrist.siftinggravel.SiftingGravel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class SiftingGravelConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("siftandspark.json");
    private static SiftingGravelConfig INSTANCE = defaults();

    public boolean inventoryCraftingEnabled = true;
    public boolean allowGeneralIgnition = false;
    public BlockConfig gravel = new BlockConfig(true, List.of("minecraft:flint=70", "minecraft:clay_ball=8", "minecraft:sand=6", "minecraft:stone=5", "minecraft:quartz=0.5"));
    public BlockConfig coarseDirt = new BlockConfig(true, List.of("minecraft:flint=65", "minecraft:dirt=18", "minecraft:hanging_roots=5", "minecraft:brown_mushroom=1.5", "minecraft:red_mushroom=1"));
    public BlockConfig rootedDirt = new BlockConfig(true, List.of("minecraft:flint=60", "minecraft:dirt=18", "minecraft:hanging_roots=8", "minecraft:brown_mushroom=1.5", "minecraft:red_mushroom=1"));
    public BlockConfig sand = new BlockConfig(true, List.of("minecraft:stick=8", "minecraft:dead_bush=5", "minecraft:flint=1.5", "minecraft:clay_ball=1.5", "minecraft:nautilus_shell=0.1"));
    public BlockConfig redSand = new BlockConfig(true, List.of("minecraft:stick=8", "minecraft:dead_bush=5", "minecraft:bone=1.5", "minecraft:gold_nugget=0.75"));
    public BlockConfig soulSand = new BlockConfig(true, List.of("minecraft:bone=10", "minecraft:skeleton_skull=0.4", "minecraft:wither_skeleton_skull=0.04"));
    public BlockConfig soulSoil = new BlockConfig(true, List.of("minecraft:bone=15", "minecraft:skeleton_skull=0.4", "minecraft:wither_skeleton_skull=0.04"));
    public boolean suspiciousSandEnabled = true;
    public boolean suspiciousGravelEnabled = true;

    public FireStarterConfig fireDrill = new FireStarterConfig(true, 1,
            List.of("minecraft:stick", "minecraft:leaf_litter"),
            List.of(1,2,3,5,8,14,24,40,65,100),
            List.of("Come on, catch...", "Not enough heat.", "Almost...", "Just need an ember...", "Maybe a little faster...", "Still nothing.", "Seriously?"),
            List.of("Finally, a flame."));
    public FireStarterConfig flintFireStarter = new FireStarterConfig(true, 3,
            List.of("minecraft:stick", "minecraft:paper", "minecraft:leaf_litter", "#minecraft:leaves"),
            List.of(10,18,30,48,70,100),
            List.of("Sparks, but no flame.", "Almost caught...", "Come on...", "The tinder missed it.", "Plenty of sparks. Wrong spot.", "One more try..."),
            List.of("There we go."));
    public List<String> additionalFireStarters = new ArrayList<>();

    public static SiftingGravelConfig get() { return INSTANCE; }
    public static void load() {
        if (Files.exists(PATH)) try (Reader r = Files.newBufferedReader(PATH)) { SiftingGravelConfig c=GSON.fromJson(r,SiftingGravelConfig.class); if(c!=null) INSTANCE=c; }
        catch(Exception e){ SiftingGravel.LOGGER.error("Could not read config; using defaults.",e); INSTANCE=defaults(); }
        INSTANCE.repairNulls(); save();
    }
    public static void save() { save(INSTANCE); }
    public static void save(SiftingGravelConfig config) { try { Files.createDirectories(PATH.getParent()); try(Writer w=Files.newBufferedWriter(PATH)){GSON.toJson(config,w);} } catch(IOException e){SiftingGravel.LOGGER.error("Could not save config.",e);} }
    public static SiftingGravelConfig editableCopy() { return GSON.fromJson(GSON.toJson(INSTANCE), SiftingGravelConfig.class); }
    public static SiftingGravelConfig editableCopyFromDisk() {
        if (Files.exists(PATH)) try (Reader r = Files.newBufferedReader(PATH)) { SiftingGravelConfig c=GSON.fromJson(r,SiftingGravelConfig.class); if(c!=null){c.repairNulls();return c;} }
        catch(Exception e){SiftingGravel.LOGGER.error("Could not read saved config for config screen; using active config.",e);}
        return editableCopy();
    }
    public static SiftingGravelConfig defaults(){ return new SiftingGravelConfig(); }
    private void repairNulls(){
        SiftingGravelConfig d=defaults();
        if(gravel==null)gravel=d.gravel;if(coarseDirt==null)coarseDirt=d.coarseDirt;if(rootedDirt==null)rootedDirt=d.rootedDirt;if(sand==null)sand=d.sand;if(redSand==null)redSand=d.redSand;if(soulSand==null)soulSand=d.soulSand;if(soulSoil==null)soulSoil=d.soulSoil;
        gravel.repair();coarseDirt.repair();rootedDirt.repair();sand.repair();redSand.repair();soulSand.repair();soulSoil.repair();
        if(fireDrill==null)fireDrill=d.fireDrill;if(flintFireStarter==null)flintFireStarter=d.flintFireStarter;
        fireDrill.repair(d.fireDrill); flintFireStarter.repair(d.flintFireStarter);
        if(additionalFireStarters==null)additionalFireStarters=new ArrayList<>();
    }
    public static final class BlockConfig { public boolean enabled=true; public List<String> loot=new ArrayList<>(); public BlockConfig(){} public BlockConfig(boolean e,List<String> l){enabled=e;loot=new ArrayList<>(l);} private void repair(){if(loot==null)loot=new ArrayList<>();} }
    public static final class FireStarterConfig {
        public boolean enabled=true; public int successfulLights=1; public List<String> ingredients=new ArrayList<>(); public List<Integer> chances=new ArrayList<>(); public List<String> failureMessages=new ArrayList<>(); public List<String> successMessages=new ArrayList<>();
        public FireStarterConfig(){}
        public FireStarterConfig(boolean e,int uses,List<String> i,List<Integer> c,List<String> f,List<String> s){enabled=e;successfulLights=uses;ingredients=new ArrayList<>(i);chances=new ArrayList<>(c);failureMessages=new ArrayList<>(f);successMessages=new ArrayList<>(s);}
        private void repair(FireStarterConfig d){if(ingredients==null)ingredients=new ArrayList<>(d.ingredients);if(chances==null||chances.isEmpty())chances=new ArrayList<>(d.chances);if(failureMessages==null||failureMessages.isEmpty())failureMessages=new ArrayList<>(d.failureMessages);if(successMessages==null||successMessages.isEmpty())successMessages=new ArrayList<>(d.successMessages);if(successfulLights<1)successfulLights=d.successfulLights;}
    }
}
