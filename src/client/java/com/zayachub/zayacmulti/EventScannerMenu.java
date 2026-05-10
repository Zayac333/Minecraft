package com.zayachub.zayacmulti;

import com.zayachub.zayacmulti.config.ConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class EventScannerMenu extends Screen {
    private TextFieldWidget webhookField;

    public EventScannerMenu() {
        super(Text.literal("Event Scanner Settings"));
    }

    @Override
    protected void init() {
        // 1. Поле для введення Webhook URL (висота 50)
        this.webhookField = new TextFieldWidget(textRenderer, width / 2 - 100, 50, 200, 20, Text.literal("Webhook URL"));
        this.webhookField.setMaxLength(512); 
        
        if (EventScannerLogic.webhookUrl != null) {
            this.webhookField.setText(EventScannerLogic.webhookUrl);
        }
        this.addDrawableChild(webhookField);

        // 2. Кнопка SOLO (висота 80)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("§eSOLO §f(101-114)"), (btn) -> {
            startScanner(101, 114);
        }).dimensions(width / 2 - 100, 80, 200, 20).build());

        // 3. Кнопка DUO (висота 110)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("§bDUO §f(201-236)"), (btn) -> {
            startScanner(201, 236);
        }).dimensions(width / 2 - 100, 110, 200, 20).build());

        // 4. Кнопка TRIO (висота 140)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("§6TRIO §f(301-323)"), (btn) -> {
            startScanner(301, 323);
        }).dimensions(width / 2 - 100, 140, 200, 20).build());

        // --- НОВА КНОПКА SAVE (висота 165) ---
        this.addDrawableChild(ButtonWidget.builder(Text.literal("§aSave Web-Hook"), (btn) -> {
            String url = this.webhookField.getText();
            ConfigManager.config.discordWebhook = url;
            EventScannerLogic.webhookUrl = url;
            ConfigManager.save();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§6[Scanner] §aWebhook успішно збережено!"), false);
            }
        }).dimensions(width / 2 - 100, 165, 200, 20).build());

        // 5. Кнопка закриття (висота 190)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("§cЗакрити"), (btn) -> {
            this.close();
        }).dimensions(width / 2 - 100, 190, 200, 20).build());
    }

    private void startScanner(int start, int end) {
        String url = this.webhookField.getText();
        if (url.isEmpty() || !url.startsWith("http")) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§cПомилка: Введіть коректний Webhook URL!"), false);
            }
            return;
        }
        EventScannerLogic.webhookUrl = url;
        EventScannerLogic.startScan(start, end, url);
        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, "§6§lEvent Scanner Configuration", width / 2, 20, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Discord Webhook URL:", width / 2 - 100, 38, 0xAAAAAA);
        
        if (EventScannerLogic.isScanning) {
            String status = "§aСканування анархії " + EventScannerLogic.currentAnarchy + "...";
            context.drawCenteredTextWithShadow(textRenderer, status, width / 2, height - 25, 0xFFFFFF);
        }
        super.render(context, mouseX, mouseY, delta);
    }
}