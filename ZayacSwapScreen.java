package com.zayachub.swapmod;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class SwapConfigScreen extends Screen {
    private final Screen parent;

    public SwapConfigScreen(Screen parent) {
        super(Text.literal("SwapMod Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Кнопка вибору джерела
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Джерело: " + SwapModClient.sourceItem), button -> {
            if (SwapModClient.sourceItem.equals("TALISMAN")) {
                SwapModClient.sourceItem = "SPHERE";
            } else if (SwapModClient.sourceItem.equals("SPHERE")) {
                SwapModClient.sourceItem = "TOTEM";
            } else {
                SwapModClient.sourceItem = "TALISMAN";
            }
            button.setMessage(Text.literal("Джерело: " + SwapModClient.sourceItem));
        })
        .dimensions(this.width / 2 - 100, this.height / 2 - 60, 200, 20)
        .build());

        // Кнопка вибору цілі
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Ціль: " + SwapModClient.targetItem), button -> {
            if (SwapModClient.targetItem.equals("TALISMAN")) {
                SwapModClient.targetItem = "SPHERE";
            } else if (SwapModClient.targetItem.equals("SPHERE")) {
                SwapModClient.targetItem = "TOTEM";
            } else {
                SwapModClient.targetItem = "TALISMAN";
            }
            button.setMessage(Text.literal("Ціль: " + SwapModClient.targetItem));
        })
        .dimensions(this.width / 2 - 100, this.height / 2 - 30, 200, 20)
        .build());

        // Кнопка "Зберегти"
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Зберегти"), button -> {
            this.client.setScreen(parent); // повертаємось до попереднього екрану
        })
        .dimensions(this.width / 2 - 100, this.height / 2, 200, 20)
        .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
