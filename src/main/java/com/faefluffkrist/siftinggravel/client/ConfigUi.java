package com.faefluffkrist.siftinggravel.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

final class ConfigUi {
    private ConfigUi() {}
    static Component toggle(boolean on){return Component.literal(on?"ON":"OFF").withStyle(on?ChatFormatting.GREEN:ChatFormatting.RED);}
    static List<String> lines(Font font,String text,int width) {
        var lines=new ArrayList<String>();String current="";
        for(String word:text.split("\\s+")){
            String next=current.isEmpty()?word:current+" "+word;
            if(!current.isEmpty()&&font.width(next)>width){lines.add(current);current=word;}else current=next;
        }
        if(!current.isEmpty())lines.add(current);return lines;
    }
    static void text(GuiGraphicsExtractor g,Font font,String text,int x,int y,int width,int maxLines,int color){
        // GUI text requires ARGB; defend against callers supplying an RGB color.
        color |= 0xFF000000;
        var lines=lines(font,text,width);
        for(int i=0;i<Math.min(lines.size(),maxLines);i++)g.text(font,Component.literal(lines.get(i)),x,y+i*10,color);
    }
}