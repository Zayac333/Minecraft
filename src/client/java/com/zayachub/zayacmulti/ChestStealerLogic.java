package com.zayachub.zayacmulti;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;

public class ChestStealerLogic {
    public static KeyBinding stealKey;
    public static boolean isActive = false;
    private static int tickDelay = 0;
    public static int notificationTimer = 0;

    // Список предметів для швидкого луту
    private static final List<String> TARGET_ITEMS = Arrays.asList(
        "minecraft:gray_dye",
        "minecraft:phantom_membrane",
        "minecraft:nautilus_shell",
        "minecraft:gunpowder"
    );

    public static void init() {
        stealKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.zayacmulti.chest_stealer",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "category.zayacmulti.functions"
        ));
    }

    public static void onTick(MinecraftClient client) {
        if (client.player == null) return;

        // Перемикання режиму на клавішу H
        if (InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_H)) {
            if (notificationTimer == 0) {
                isActive = !isActive;
                notificationTimer = 100; // Сповіщення на 5 секунд
            }
        }

        if (notificationTimer > 0) notificationTimer--;
        if (!isActive) return;

        // Перевірка, чи відкрита скриня
        if (client.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
            if (tickDelay > 0) {
                tickDelay--;
                return;
            }

            // Проходимо по слотах інвентарю скрині
            int containerSize = handler.getInventory().size();
            for (int i = 0; i < containerSize; i++) {
                Slot slot = handler.getSlot(i);
                
                if (slot.hasStack()) {
                    // Перевіряємо ID предмета
                    String itemId = Registries.ITEM.getId(slot.getStack().getItem()).toString();
                    
                    if (TARGET_ITEMS.contains(itemId)) {
                        // Забираємо предмет через Quick Move (Shift+Click)
                        client.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, client.player);
                        tickDelay = 1; // Затримка 1 тік, щоб не кікнув античит
                        return; 
                    }
                }
            }
        } else {
            isActive = false; // Вимикаємо, якщо скриню закрили
        }
    }

    public static void renderHud(DrawContext drawContext, MinecraftClient client) {
        if (notificationTimer > 0) {
            String text = isActive ? "§6Chest Stealer: §aУВІМКНЕНО" : "§6Chest Stealer: §cВИМКНЕНО";
            int width = client.textRenderer.getWidth(text);
            int x = client.getWindow().getScaledWidth() - width - 10;
            int y = client.getWindow().getScaledHeight() - 20;

            drawContext.drawText(client.textRenderer, text, x, y, 0xFFFFFF, true);
        }
    }
}