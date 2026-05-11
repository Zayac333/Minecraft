package com.zayachub.zayacmulti;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class SwapModLogic {
    // Налаштування предметів
    public static String sourceItem = "TALISMAN";
    public static String targetItem = "SPHERE";
    
    // Клавіші
    public static KeyBinding swapKey;
    public static KeyBinding debugLogKey;

    public static boolean debugEnabled = false;
    
    // Внутрішні змінні логіки
    private static int freezeTicks = 0;
    private static boolean wasForwardPressed = false;
    private static boolean pendingSwap = false;

    // Статичний метод ініціалізації (викликається в ZayacHubClient)
    public static void init() {
        swapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.zayacmulti.swap_action",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.zayacmulti.functions"
        ));

        debugLogKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.zayacmulti.swap_debug",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            "category.zayacmulti.functions"
        ));
    }

    // Статичний метод обробки тіків (викликається в ZayacHubClient)
    public static void onTick(MinecraftClient client) {
        if (client.player == null) return;

        // Логіка заморозки та свапу
        if (freezeTicks > 0) {
            freezeTicks--;
            
            // Примусова зупинка руху
            client.options.forwardKey.setPressed(false);
            client.options.backKey.setPressed(false);
            client.options.leftKey.setPressed(false);
            client.options.rightKey.setPressed(false);
            client.options.jumpKey.setPressed(false);

            // ВИКОНАННЯ СВАПУ: на 1-му тіку після початку (як ти просив)
            if (pendingSwap && freezeTicks == 1) {
                performSmartSwap(client);
                pendingSwap = false;
            }

            // Відновлення руху після закінчення фрізу
            if (freezeTicks == 0 && wasForwardPressed) {
                client.options.forwardKey.setPressed(true);
            }
        }

        // Обробка натискання клавіші свапу
        while (swapKey.wasPressed()) {
            wasForwardPressed = client.options.forwardKey.isPressed();
            freezeTicks = 3; // Загальна заморозка 3 тіки
            pendingSwap = true;
        }

        // Обробка дебаг-логу
        while (debugLogKey.wasPressed()) {
            debugEnabled = !debugEnabled;
            String status = debugEnabled ? "§aувімкнено" : "§cвимкнено";
            client.player.sendMessage(Text.literal("§7[ZayacSwap] §fЛог: " + status), false);
        }
    }

    private static void performSmartSwap(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;

        PlayerInventory inv = client.player.getInventory();
        int sourceIndex = -1;
        int targetIndex = -1;

        // Пошук предметів в інвентарі
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            String name = stack.getName().getString().toLowerCase();

            if (sourceIndex == -1 && matchesItem(stack, sourceItem, name)) sourceIndex = i;
            if (targetIndex == -1 && matchesItem(stack, targetItem, name)) targetIndex = i;
        }

        if (sourceIndex == -1 || targetIndex == -1) {
            if (debugEnabled) sendMessage(client, "§cПомилка: Предмети не знайдено!", 0xFF5555);
            return;
        }

        // Конвертація індексу для пакету (слоти 0-8 хотбару в мережі мають інші ID)
        int slotSource = sourceIndex < 9 ? sourceIndex + 36 : sourceIndex;

        try {
            // Сама дія свапу
            client.interactionManager.clickSlot(
                client.player.currentScreenHandler.syncId,
                slotSource,
                targetIndex, 
                SlotActionType.SWAP,
                client.player
            );

            if (debugEnabled) sendMessage(client, "§aСвап успішно: " + sourceItem + " ↔ " + targetItem, 0x55FF55);

            // Міняємо місцями для наступного натискання
            String tmp = sourceItem;
            sourceItem = targetItem;
            targetItem = tmp;

        } catch (Exception e) {
            if (debugEnabled) sendMessage(client, "§cПомилка: " + e.getMessage(), 0xFF5555);
        }
    }

    private static boolean matchesItem(ItemStack stack, String key, String name) {
        switch (key) {
            case "TALISMAN": return stack.getItem() == Items.TOTEM_OF_UNDYING && name.contains("талисман");
            case "SPHERE":   return stack.getItem() == Items.PLAYER_HEAD && name.contains("сфера");
            case "TOTEM":    return stack.getItem() == Items.TOTEM_OF_UNDYING && name.contains("тотем");
            case "APPLE":    return stack.getItem() == Items.GOLDEN_APPLE;
            case "SHIELD":   return stack.getItem() == Items.SHIELD;
            default: return false;
        }
    }

    private static void sendMessage(MinecraftClient client, String message, int color) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message).styled(s -> s.withColor(color)), false);
        }
    }
}