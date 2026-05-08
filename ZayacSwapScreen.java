package com.zayachub.swapmod;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class SwapConfigScreen extends Screen {
    private final Screen parent;

    private final List<ButtonWidget> sourceButtons = new ArrayList<>();
    private final List<ButtonWidget> targetButtons = new ArrayList<>();

    // заголовки секцій
    private ButtonWidget sourceTitle;
    private ButtonWidget targetTitle;

    public SwapConfigScreen(Screen parent) {
        super(Text.literal("AutoSwap"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 80;

        // Заголовок секції "Перший предмет"
        sourceTitle = ButtonWidget.builder(Text.literal("Перший: " + getLabel(SwapModClient.sourceItem)), b -> {})
            .dimensions(centerX - 220, startY, 200, 20).build();
        this.addDrawableChild(sourceTitle);

        // Кнопки для вибору першого предмета
        addSourceButton("TALISMAN", "Талисман", centerX - 220, startY + 30);
        addSourceButton("SPHERE",   "Сфера",    centerX - 220, startY + 60);
        addSourceButton("TOTEM",    "Тотем",    centerX - 220, startY + 90);
        addSourceButton("APPLE",    "Золотое яблоко", centerX - 220, startY + 120);
        addSourceButton("SHIELD",   "Щит",      centerX - 220, startY + 150);

        // Заголовок секції "Другий предмет"
        targetTitle = ButtonWidget.builder(Text.literal("Другий: " + getLabel(SwapModClient.targetItem)), b -> {})
            .dimensions(centerX + 20, startY, 200, 20).build();
        this.addDrawableChild(targetTitle);

        // Кнопки для вибору другого предмета
        addTargetButton("TALISMAN", "Талисман", centerX + 20, startY + 30);
        addTargetButton("SPHERE",   "Сфера",    centerX + 20, startY + 60);
        addTargetButton("TOTEM",    "Тотем",    centerX + 20, startY + 90);
        addTargetButton("APPLE",    "Золотое яблоко", centerX + 20, startY + 120);
        addTargetButton("SHIELD",   "Щит",      centerX + 20, startY + 150);

        // Кнопка "Зберегти"
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Зберегти"), button -> {
            this.client.setScreen(parent);
        }).dimensions(centerX - 100, this.height - 40, 200, 20).build());
    }

    private void addSourceButton(String key, String label, int x, int y) {
        ButtonWidget btn = ButtonWidget.builder(Text.literal(label), button -> {
            SwapModClient.sourceItem = key;
            for (ButtonWidget b : sourceButtons) {
                b.setMessage(Text.literal(b.getMessage().getString().replace("✔ ", "")));
            }
            button.setMessage(Text.literal("✔ " + label));
            sourceTitle.setMessage(Text.literal("Перший: ✔ " + label));
        }).dimensions(x, y, 200, 20).build();

        sourceButtons.add(btn);
        this.addDrawableChild(btn);

        if (SwapModClient.sourceItem.equals(key)) {
            btn.setMessage(Text.literal("✔ " + label));
            sourceTitle.setMessage(Text.literal("Перший: ✔ " + label));
        }
    }

    private void addTargetButton(String key, String label, int x, int y) {
        ButtonWidget btn = ButtonWidget.builder(Text.literal(label), button -> {
            SwapModClient.targetItem = key;
            for (ButtonWidget b : targetButtons) {
                b.setMessage(Text.literal(b.getMessage().getString().replace("✔ ", "")));
            }
            button.setMessage(Text.literal("✔ " + label));
            targetTitle.setMessage(Text.literal("Другий: ✔ " + label));
        }).dimensions(x, y, 200, 20).build();

        targetButtons.add(btn);
        this.addDrawableChild(btn);

        if (SwapModClient.targetItem.equals(key)) {
            btn.setMessage(Text.literal("✔ " + label));
            targetTitle.setMessage(Text.literal("Другий: ✔ " + label));
        }
    }

    private String getLabel(String key) {
        switch (key) {
            case "TALISMAN": return "Талисман";
            case "SPHERE":   return "Сфера";
            case "TOTEM":    return "Тотем";
            case "APPLE":    return "Золотое яблоко";
            case "SHIELD":   return "Щит";
            default: return key;
        }
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
