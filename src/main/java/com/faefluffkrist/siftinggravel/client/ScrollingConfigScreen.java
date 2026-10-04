package com.faefluffkrist.siftinggravel.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Shared height-aware row viewport with wheel scrolling and a draggable scrollbar. */
abstract class ScrollingConfigScreen extends Screen {
    protected int scrollRow, visibleRows, listTop, listBottom, listX, listWidth, rowHeight;
    private int totalRows;
    private boolean dragging;
    ScrollingConfigScreen(Component title) { super(title); }
    protected void layoutRows(int total, int top, int bottom, int step, int maximumWidth) {
        totalRows=total;listTop=top;rowHeight=step;
        listWidth=Math.min(maximumWidth,width-48);listX=width/2-listWidth/2;
        visibleRows=Math.max(1,(bottom-top)/step);
        listBottom=listTop+visibleRows*step;
        scrollRow=Math.max(0,Math.min(scrollRow,Math.max(0,totalRows-visibleRows)));
    }
    protected void beforeScroll() {}
    protected void drawScrollbar(GuiGraphicsExtractor g) {
        if(totalRows<=visibleRows)return;
        int x=listX+listWidth+7,track=listBottom-listTop;
        int thumb=Math.max(16,track*visibleRows/totalRows),travel=track-thumb;
        int top=listTop+travel*scrollRow/(totalRows-visibleRows);
        g.fill(x,listTop,x+5,listBottom,0x663D454B);
        g.fill(x,top,x+5,top+thumb,dragging?0xFFFFCF77:0xFFB7C7CC);
    }
    private void setScroll(double mouseY) {
        int max=Math.max(0,totalRows-visibleRows);if(max==0)return;
        int track=listBottom-listTop,thumb=Math.max(16,track*visibleRows/totalRows);
        int next=(int)Math.round((mouseY-listTop-thumb/2.0)/Math.max(1,track-thumb)*max);
        next=Math.max(0,Math.min(max,next));
        if(next!=scrollRow){beforeScroll();scrollRow=next;rebuildWidgets();}
    }
    @Override public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Screen.extractMenuBackgroundTexture(graphics,
                net.minecraft.resources.Identifier.withDefaultNamespace("textures/block/cobbled_deepslate.png"),
                0, 0, 0, 0, width, height);
        graphics.fill(0, 0, width, height, 0x99000000);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(my>=listTop&&my<=listBottom&&vertical!=0&&totalRows>visibleRows){
            beforeScroll();int old=scrollRow;
            scrollRow=Math.max(0,Math.min(totalRows-visibleRows,scrollRow+(vertical>0?-1:1)));
            if(old!=scrollRow)rebuildWidgets();return true;
        }
        return super.mouseScrolled(mx,my,horizontal,vertical);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick) {
        int x=listX+listWidth+7;
        if(e.button()==0&&totalRows>visibleRows&&e.x()>=x-3&&e.x()<=x+8&&e.y()>=listTop&&e.y()<=listBottom){dragging=true;setScroll(e.y());return true;}
        return super.mouseClicked(e,doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(dragging){setScroll(e.y());return true;}return super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e){if(dragging){dragging=false;return true;}return super.mouseReleased(e);}
}
