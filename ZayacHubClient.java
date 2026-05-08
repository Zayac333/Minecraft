package com.zayachub.zayacmulti;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ZayacHubClient implements ClientModInitializer {
    public static KeyBinding swapConfigKey;

    @Override
    public void onInitializeClient() {
        // Реєстрація клавіші головного меню (Right Shift)
        swapConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.zayacmulti.swap_menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.zayacmulti.menu"
        ));

        // Ініціалізуємо модулі (вони самі зареєструють свої клавіші всередині)
        SwapModLogic.init();   
        EventsModLogic.init(); 
        ClanModLogic.init();   

        // Обробка повідомлень чату
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            EventsModLogic.handleEventMessage(message);
            if (!overlay) ClanModLogic.handleChatMessage(message.getString());
        });

        // Головний тік клієнта
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            
            // Викликаємо логіку модулів
            SwapModLogic.onTick(client);
            EventsModLogic.onTick(client);
            ClanModLogic.onTick(client);

            // Перевірка тільки тієї клавіші, що зареєстрована ТУТ
            while (swapConfigKey.wasPressed()) {
                client.setScreen(new SwapMenuScreen(client.currentScreen));
            }
        });

        // Відображення HUD (радар і стрілка івентів)
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            EventsModLogic.renderHud(drawContext);
            ClanModLogic.renderHud(drawContext);
        });
    }
}