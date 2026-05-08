package com.zayachub.swapmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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

public class SwapModClient implements ClientModInitializer {
    public static String sourceItem = "TALISMAN";
    public static String targetItem = "SPHERE";
    public static KeyBinding swapKey;
    public static KeyBinding configMenuKey;
    public static KeyBinding debugLogKey;

    public static boolean debugEnabled = false;
    private int freezeTicks = 0;
    private boolean wasForwardPressed = false;
    private boolean pendingSwap = false;

    @Override
    public void onInitializeClient() {
        swapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.zayachub.swap",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.zayachub.tools"
        ));

        configMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.zayachub.config",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.zayachub.tools"
        ));

        debugLogKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.zayachub.debug",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            "category.zayachub.tools"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (freezeTicks > 0) {
                freezeTicks--;
                
                // Примусова зупинка для античита
                client.options.forwardKey.setPressed(false);
                client.options.backKey.setPressed(false);
                client.options.leftKey.setPressed(false);
                client.options.rightKey.setPressed(false);
                client.options.jumpKey.setPressed(false);

                // Свапаємо через 2 тіки після зупинки (на 13-му тіку)
                if (pendingSwap && freezeTicks == 1) {
                    performSmartSwap(client);
                    pendingSwap = false;
                }

                // Відновлення руху після закінчення заморозки
                if (freezeTicks == 0 && wasForwardPressed) {
                    client.options.forwardKey.setPressed(true);
                }
            }

            while (swapKey.wasPressed()) {
                wasForwardPressed = client.options.forwardKey.isPressed();
                freezeTicks = 3; 
                pendingSwap = true;
            }

            while (configMenuKey.wasPressed()) {
                client.setScreen(new SwapConfigScreen(client.currentScreen));
            }

            while (debugLogKey.wasPressed()) {
                debugEnabled = !debugEnabled;
                String status = debugEnabled ? "§aпочато" : "§cзакінчено";
                client.player.sendMessage(Text.literal("§7[Swap] §fЛог розробника: " + status), false);
            }
        });
    }

    private void performSmartSwap(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;

        PlayerInventory inv = client.player.getInventory();
        int sourceIndex = -1;
        int targetIndex = -1;

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            String name = stack.getName().getString().toLowerCase();

            if (sourceIndex == -1 && matchesItem(stack, sourceItem, name)) sourceIndex = i;
            if (targetIndex == -1 && matchesItem(stack, targetItem, name)) targetIndex = i;
        }

        if (sourceIndex == -1 || targetIndex == -1) {
            if (debugEnabled) sendMessage(client, "§cПомилка: предмети не знайдено!", 0xFF5555);
            return;
        }

        int slotSource = sourceIndex < 9 ? sourceIndex + 36 : sourceIndex;

        try {
            client.interactionManager.clickSlot(
                client.player.currentScreenHandler.syncId,
                slotSource,
                targetIndex, 
                SlotActionType.SWAP,
                client.player
            );

            if (debugEnabled) sendMessage(client, "§aСвап успішно: " + sourceItem + " ↔ " + targetItem, 0x55FF55);

            // Зворотний свап (міняємо джерело і ціль місцями)
            String tmp = sourceItem;
            sourceItem = targetItem;
            targetItem = tmp;

        } catch (Exception e) {
            if (debugEnabled) sendMessage(client, "§cПомилка: " + e.getMessage(), 0xFF5555);
        }
    }

    private boolean matchesItem(ItemStack stack, String key, String name) {
        switch (key) {
            case "TALISMAN": return stack.getItem() == Items.TOTEM_OF_UNDYING && name.contains("талисман");
            case "SPHERE":   return stack.getItem() == Items.PLAYER_HEAD && name.contains("сфера");
            case "TOTEM":    return stack.getItem() == Items.TOTEM_OF_UNDYING && name.contains("тотем");
            case "APPLE":    return stack.getItem() == Items.GOLDEN_APPLE;
            case "SHIELD":   return stack.getItem() == Items.SHIELD;
            default: return false;
        }
    }

    private void sendMessage(MinecraftClient client, String message, int color) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message).styled(s -> s.withColor(color)), false);
        }
    }
}
