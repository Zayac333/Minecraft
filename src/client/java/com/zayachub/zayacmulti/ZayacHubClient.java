package com.zayachub.zayacmulti;

import com.zayachub.zayacmulti.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ZayacHubClient implements ClientModInitializer {
    // Клавіші керування
    public static KeyBinding swapConfigKey;
    public static KeyBinding eventScannerKey;

    @Override
    public void onInitializeClient() {
        // 1. Реєстрація клавіш у налаштуваннях гри
        swapConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.zayacmulti.swap_menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT, // Основне меню на правий Shift
            "category.zayacmulti.menu"
        ));

        eventScannerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.zayacmulti.scanner_menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K, // Меню сканера на клавішу K
            "category.zayacmulti.menu"
        ));

        // 2. Завантаження конфігурації та ініціалізація модулів
        ConfigManager.load();
        
        SwapModLogic.init();   
        EventsModLogic.init(); 
        ClanModLogic.init();   
        ChestStealerLogic.init();
        EventScannerLogic.webhookUrl = ConfigManager.config.discordWebhook;
        // Якщо в EventScannerLogic буде метод init(), розкоментуй нижче:
        // EventScannerLogic.init();

        // 3. Синхронізація даних з файлу ConfigManager у змінні логіки
        if (ConfigManager.config != null) {
            SwapModLogic.sourceItem = ConfigManager.config.sourceItem;
            SwapModLogic.targetItem = ConfigManager.config.targetItem;
            ClanModLogic.autoCoordsEnabled.set(ConfigManager.config.radarEnabled);
            
            // Завантажуємо список івентів
            EventsModLogic.enabledEvents.clear();
            if (ConfigManager.config.enabledEvents != null) {
                EventsModLogic.enabledEvents.addAll(ConfigManager.config.enabledEvents);
            }

            // Завантажуємо збережений вебхук, щоб не вводити щоразу
            if (ConfigManager.config.discordWebhook != null) {
                EventScannerLogic.webhookUrl = ConfigManager.config.discordWebhook;
            }
        }

// 4. Глобальний обробник повідомлень чату
ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
    // ОБОВ'ЯЗКОВО додай цей рядок:
    EventScannerLogic.handleScannerMessage(message.getString());
    
    // Твоя існуюча логіка
    EventsModLogic.handleEventMessage(message);
    if (!overlay) {
        ClanModLogic.handleChatMessage(message.getString());
    }
});

        // 5. Головний робочий цикл (20 тіків на секунду)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            
            // Робота логіки всіх підключених модулів
            SwapModLogic.onTick(client);
            EventsModLogic.onTick(client);
            ClanModLogic.onTick(client);
            ChestStealerLogic.onTick(client);
            EventScannerLogic.onTick(client);

            // Відкриття основного меню (Swap/Config)
            while (swapConfigKey.wasPressed()) {
                client.setScreen(new SwapMenuScreen(client.currentScreen));
            }

            // Відкриття меню налаштування сканера (Webhook/Modes)
            while (eventScannerKey.wasPressed()) {
                client.setScreen(new EventScannerMenu());
            }
        });

        // 6. Рендеринг елементів інтерфейсу (HUD)
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;

            // Малюємо стрілку та назву івенту
            EventsModLogic.renderHud(drawContext);
            
            // Малюємо радар кланових гравців
            ClanModLogic.renderHud(drawContext);
            
            // Малюємо статус Chest Stealer (справа знизу)
            ChestStealerLogic.renderHud(drawContext, mc);
        });
    }
}