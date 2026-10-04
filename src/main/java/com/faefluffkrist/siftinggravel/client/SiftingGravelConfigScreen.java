package com.faefluffkrist.siftinggravel.client;

import com.faefluffkrist.siftinggravel.config.SiftingGravelConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.*;

/** Category list and scrollable configuration editors. */
public final class SiftingGravelConfigScreen extends ScrollingConfigScreen {
    private final Screen parent;
    private SiftingGravelConfig config = SiftingGravelConfig.editableCopyFromDisk();
    private String savedSnapshot = snapshot(config);
    private boolean changesSaved;
    private int category;
    private Page page;
    private final List<Row> rows = new ArrayList<>();
    private final List<Runnable> captures = new ArrayList<>();
    private String status = "";
    private enum Page {
        GENERAL("General"), GRAVEL("Gravel"), COARSE_DIRT("Coarse Dirt"), ROOTED_DIRT("Rooted Dirt"),
        SAND("Sand"), RED_SAND("Red Sand"), SOUL_SAND("Soul Sand"), SOUL_SOIL("Soul Soil"),
        FIRE_OPTIONS("Fire Options"), FIRE_DRILL("Fire Drill"), FLINT_STARTER("Flint Starter");
        final String label; Page(String label) { this.label=label; }
    }
    private record Row(String label, String description, Consumer<Integer> widget) {}
    private SiftingGravelConfigScreen(Screen parent) { super(Component.literal("Spark and Sift Configuration")); this.parent=parent; }
    public static Screen create(Screen parent) { return new SiftingGravelConfigScreen(parent); }
    private static String snapshot(SiftingGravelConfig c) { return new com.google.gson.Gson().toJson(c); }
    @Override protected void init() {
        captures.clear(); rows.clear();
        if(page==null) initOverview(); else initEditor();
    }
    private Page[] pages() { return category==0 ? new Page[]{Page.GENERAL,Page.GRAVEL,Page.COARSE_DIRT,Page.ROOTED_DIRT,Page.SAND,Page.RED_SAND,Page.SOUL_SAND,Page.SOUL_SOIL} : new Page[]{Page.FIRE_OPTIONS,Page.FIRE_DRILL,Page.FLINT_STARTER}; }
    private void initOverview() {
        int w=Math.min(460,width-48),x=(width-w)/2,tab=(w-4)/2;
        for(int i=0;i<2;i++) { final int selected=i;
            var b=addRenderableWidget(Button.builder(Component.literal(i==0?"Sifting":"Firemaking"),q->{category=selected;scrollRow=0;rebuildWidgets();}).bounds(x+i*(tab+4),34,tab,20).build()); b.active=category!=i;
        }
        for(Page p:pages()) rows.add(new Row(p.label, "", y->{
            if(p!=Page.GENERAL && p!=Page.FIRE_OPTIONS) {
                addRenderableWidget(Button.builder(ConfigUi.toggle(enabled(p)),b->{setEnabled(p,!enabled(p));save();rebuildWidgets();}).bounds(listX+listWidth-125,y+3,55,20).build());
            }
            addRenderableWidget(Button.builder(Component.literal("Edit"),b->{page=p;scrollRow=0;status="";rebuildWidgets();}).bounds(listX+listWidth-65,y+3,65,20).build());
        }));
        layoutRows(rows.size(),82,height-50,32,460); buildRows();
        addRenderableWidget(Button.builder(Component.literal("Done"),b->finish()).bounds(width/2-60,height-28,120,20).build());
    }
    private void initEditor() {
        if(page==Page.GENERAL) {
            toggle("Inventory Crafting","Allow quick-crafting with a bowl in the inventory or crafting table. Otherwise, sift manually.",()->config.inventoryCraftingEnabled,v->config.inventoryCraftingEnabled=v);
            toggle("Suspicious Sand","Allow slower bowl archaeology. Vanilla brush archaeology is unaffected.",()->config.suspiciousSandEnabled,v->config.suspiciousSandEnabled=v);
            toggle("Suspicious Gravel","Allow slower bowl archaeology. Vanilla brush archaeology is unaffected.",()->config.suspiciousGravelEnabled,v->config.suspiciousGravelEnabled=v);
        } else if(page==Page.FIRE_OPTIONS) {
            toggle("Ignite Other Targets","Allow candles, candle cakes, TNT, placeable fire and valid Nether portals. Attempt chances and durability still apply.",()->config.allowGeneralIgnition,v->config.allowGeneralIgnition=v);
        } else if(category==0) {
            var block=block(page);
            toggle("Enable Sifting","Enable bowl sifting for "+page.label+".",()->block.enabled,v->block.enabled=v);
            for(int i=0;i<block.loot.size();i++) entry(block.loot,i,"Loot "+(i+1),"Format: minecraft:flint=70. Each entry rolls independently.");
            action("Add Loot",()->block.loot.add("minecraft:item=1"));
        } else {
            var fire=fire(page);
            toggle("Enable Starter",page==Page.FIRE_DRILL?"A slow friction starter, consumed after one successful fire.":"A primitive spark starter, good for three successful fires.",()->fire.enabled,v->fire.enabled=v);
            field("Success Chance per Attempt","Comma-separated percentages, from 0 to 100. The final attempt is guaranteed.",joinInts(fire.chances),s->fire.chances=parseInts(s,fire.chances),200);
            for(int i=0;i<fire.ingredients.size();i++) entry(fire.ingredients,i,"Companion Ingredient "+(i+1),"Item ID or #tag, beside the fixed base: "+(page==Page.FIRE_DRILL?"1 stick.":"2 flint."));
            action("Add Ingredient",()->fire.ingredients.add("minecraft:item"));
            field("Failure Messages","Separate multiple inner-monologue messages with |.",String.join(" | ",fire.failureMessages),s->fire.failureMessages=splitMessages(s),500);
            field("Success Messages","Separate multiple inner-monologue messages with |.",String.join(" | ",fire.successMessages),s->fire.successMessages=splitMessages(s),300);
        }
        int textWidth=Math.max(40,Math.min(420,width-48)-14);
        int step=62;
        for(Row r:rows) step=Math.max(step,40+ConfigUi.lines(font,r.description,textWidth).size()*10);
        layoutRows(rows.size(),48,height-54,step,420);buildRows();
        addRenderableWidget(Button.builder(Component.literal("Back"),b->back()).bounds(listX,height-28,80,20).build());
        addRenderableWidget(Button.builder(Component.literal("Save & Back"),b->{capture();save();if(status.equals("Could not save settings"))return;page=null;scrollRow=0;rebuildWidgets();}).bounds(listX+listWidth-115,height-28,115,20).build());
    }
    private void buildRows() { for(int i=scrollRow;i<Math.min(rows.size(),scrollRow+visibleRows);i++) rows.get(i).widget.accept(listTop+(i-scrollRow)*rowHeight); }
    private void toggle(String label,String description,BooleanSupplier get,Consumer<Boolean> set) {
        rows.add(new Row(label,description,y->addRenderableWidget(Button.builder(ConfigUi.toggle(get.getAsBoolean()),b->{set.accept(!get.getAsBoolean());b.setMessage(ConfigUi.toggle(get.getAsBoolean()));}).bounds(listX+listWidth-65,y+3,65,20).build())));
    }
    private void field(String label,String description,String value,Consumer<String> setter,int max) {
        rows.add(new Row(label,description,y->{var box=addRenderableWidget(new EditBox(font,listX,y+20,listWidth,20,Component.literal(label)));box.setMaxLength(max);box.setValue(value); captures.add(()->setter.accept(box.getValue().trim()));}));
    }
    private void entry(List<String> list,int index,String label,String description) {
        rows.add(new Row(label,description,y->{var box=addRenderableWidget(new EditBox(font,listX,y+20,listWidth-76,20,Component.literal(label)));box.setMaxLength(180);box.setValue(list.get(index));captures.add(()->list.set(index,box.getValue().trim()));
            addRenderableWidget(Button.builder(Component.literal("Remove"),b->{capture();list.remove(index);rebuildWidgets();}).bounds(listX+listWidth-72,y+20,72,20).build());}));
    }
    private void action(String label,Runnable action) { rows.add(new Row("","",y->addRenderableWidget(Button.builder(Component.literal(label),b->{capture();action.run();scrollRow=Math.max(0,rows.size()-visibleRows+1);rebuildWidgets();}).bounds(listX,y+3,listWidth,20).build()))); }
    @Override public void resize(int width,int height) { capture();super.resize(width,height); }
    private void capture() { for(Runnable r:captures) r.run(); }
    @Override protected void beforeScroll() { capture(); }
    private void save() {
        for(Page p:new Page[]{Page.GRAVEL,Page.COARSE_DIRT,Page.ROOTED_DIRT,Page.SAND,Page.RED_SAND,Page.SOUL_SAND,Page.SOUL_SOIL}) block(p).loot.removeIf(String::isBlank);
        config.fireDrill.ingredients.removeIf(String::isBlank);config.flintFireStarter.ingredients.removeIf(String::isBlank);
        String next=snapshot(config); if(!next.equals(savedSnapshot)) { SiftingGravelConfig.save(config);
            if(next.equals(snapshot(SiftingGravelConfig.editableCopyFromDisk()))) { changesSaved=true;savedSnapshot=next;status="Settings saved"; }
            else status="Could not save settings";
        }
    }
    private void back() { config=SiftingGravelConfig.editableCopyFromDisk();page=null;scrollRow=0;status="";rebuildWidgets(); }
    private void finish() { minecraft.gui.setScreen(changesSaved?new RelogNoticeScreen(parent):parent); }
    @Override public void onClose() { if(page!=null) back();else finish(); }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        super.extractRenderState(g,mx,my,delta);
        Component heading=page==null?title:Component.literal(page.label).withStyle(ChatFormatting.GOLD,ChatFormatting.BOLD).append(Component.literal(" Settings").withStyle(ChatFormatting.WHITE,ChatFormatting.BOLD));
        g.centeredText(font,heading,width/2,12,0xFFFFFFFF);
        if(page!=null && page!=Page.GENERAL && page!=Page.FIRE_OPTIONS) {
            g.centeredText(font,Component.literal("Currently Configured to be ").withStyle(ChatFormatting.ITALIC)
                .append(Component.literal("[").withStyle(ChatFormatting.WHITE)).append(ConfigUi.toggle(enabled(page)))
                .append(Component.literal("]").withStyle(ChatFormatting.WHITE)),width/2,28,0xFFFFFFFF);
        }
        if(page==null) g.centeredText(font,Component.literal(category==0?"Bowl sifting, archaeology and loot pools":"Ignition options and fire starters"),width/2,62,0xFFB7C7CC);
        for(int i=scrollRow;i<Math.min(rows.size(),scrollRow+visibleRows);i++) {
            Row r=rows.get(i);int y=listTop+(i-scrollRow)*rowHeight;
            int labelWidth=page==null?listWidth-140:listWidth-80;
            g.fill(listX,y,listX+(page==null?listWidth-132:listWidth),y+rowHeight-5,0x40333C42);
            ConfigUi.text(g,font,r.label,listX+7,y+7,labelWidth,1,0xFFFFE0A1);
            if(page!=null) ConfigUi.text(g,font,r.description,listX+7,y+43,listWidth-14,(rowHeight-43)/10,0xFFA8B8BF);
        }
        drawScrollbar(g);
        g.centeredText(font,Component.literal(status.isEmpty()?"Changes require reopening the world":status),width/2,height-43,0xFFAAAAAA);
    }
    private boolean enabled(Page p) { return category==0?block(p).enabled:fire(p).enabled; }
    private void setEnabled(Page p,boolean v) { if(category==0)block(p).enabled=v;else fire(p).enabled=v; }
    private SiftingGravelConfig.FireStarterConfig fire(Page p) { return p==Page.FIRE_DRILL?config.fireDrill:config.flintFireStarter; }
    private SiftingGravelConfig.BlockConfig block(Page p) { return switch(p) {case GRAVEL->config.gravel;case COARSE_DIRT->config.coarseDirt;case ROOTED_DIRT->config.rootedDirt;case SAND->config.sand;case RED_SAND->config.redSand;case SOUL_SAND->config.soulSand;case SOUL_SOIL->config.soulSoil;default->throw new IllegalStateException();}; }
    private static String joinInts(List<Integer> values) { return String.join(", ",values.stream().map(String::valueOf).toList()); }
    private static List<Integer> parseInts(String s,List<Integer> fallback) { try { var result=new ArrayList<Integer>(); for(String v:s.split(",")) result.add(Math.max(0,Math.min(100,Integer.parseInt(v.trim()))));return result.isEmpty()?fallback:result; } catch(NumberFormatException e) { return fallback; } }
    private static List<String> splitMessages(String s) { var result=new ArrayList<String>();for(String v:s.split("\\|")) if(!v.trim().isEmpty())result.add(v.trim());return result; }
}
