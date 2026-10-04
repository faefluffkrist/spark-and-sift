package com.faefluffkrist.siftinggravel.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shown after leaving the config screen when at least one change was saved. */
final class RelogNoticeScreen extends Screen {
    private final Screen returnTo;

    RelogNoticeScreen(Screen returnTo) {
        super(Component.literal("Spark and Sift Settings Saved"));
        this.returnTo = returnTo;
    }

    @Override public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Screen.extractMenuBackgroundTexture(graphics,
                net.minecraft.resources.Identifier.withDefaultNamespace("textures/block/cobbled_deepslate.png"),
                0, 0, 0, 0, width, height);
        graphics.fill(0, 0, width, height, 0x99000000);
    }
    @Override
    protected void init() {
        // Use real widgets for the notice text. In 26.2 these are collected by the
        // screen's normal render-state path, avoiding the custom text extraction
        // issue that previously left only the Okay button visible.
        Component saved = Component.literal("Settings saved!")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);

        Component relog = Component.empty()
                .append(Component.literal("Leave and rejoin")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD, ChatFormatting.ITALIC))
                .append(Component.literal(" the world to apply your changes.")
                        .withStyle(ChatFormatting.ITALIC));

        int savedWidth = font.width(saved);
        addRenderableWidget(new StringWidget(
                (width - savedWidth) / 2, height / 2 - 40, savedWidth, 12, saved, font));
        var lines=ConfigUi.lines(font,"Leave and rejoin the world to apply your changes.",Math.max(80,width-48));
        for(int i=0;i<lines.size();i++) {
            Component line=Component.literal(lines.get(i)).withStyle(ChatFormatting.RED,ChatFormatting.ITALIC);
            int lineWidth=font.width(line);
            addRenderableWidget(new StringWidget((width-lineWidth)/2,height/2-16+i*12,lineWidth,12,line,font));
        }

        addRenderableWidget(Button.builder(Component.literal("Okay"), b -> onClose())
                .bounds(width / 2 - 60, height / 2 + 34, 120, 20)
                .build());
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(returnTo);
    }
}
