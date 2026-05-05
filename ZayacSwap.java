package com.zayachub.swapmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class SwapModClient implements ClientModInitializer {
    public static String sourceItem = "TALISMAN";
    public static String targetItem = "SPHERE";
    public static KeyBinding swapKey;
    public static KeyBinding configMenuKey;

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

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (swapKey.wasPressed()) {
                if (client.player != null) {
                    performSmartSwap(client);
                }
            }
            while (configMenuKey.wasPressed()) {
                client.setScreen(new SwapConfigScreen(client.currentScreen));
            }
        });
    }

    private void performSmartSwap(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) {
            sendMessage(client, "Помилка: немає доступу до гравця або менеджера взаємодії", 0xFF0000);
            return;
        }

        PlayerInventory inv = client.player.getInventory();
        int sourceIndex = -1;
        int targetIndex = -1;

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            String name = stack.getName().getString().toLowerCase();

            if (sourceIndex == -1 && name.contains(sourceItem.toLowerCase())) {
                sourceIndex = i;
            }
            if (targetIndex == -1 && name.contains(targetItem.toLowerCase())) {
                targetIndex = i;
            }
        }

        if (sourceIndex == -1) {
            sendMessage(client, "Помилка: не знайдено предмет '" + sourceItem + "'", 0xFF0000);
            return;
        }
        if (targetIndex == -1) {
            sendMessage(client, "Помилка: не знайдено предмет '" + targetItem + "'", 0xFF0000);
            return;
        }

        int slotSource = sourceIndex < 9 ? sourceIndex + 36 : sourceIndex;
        int slotTarget = targetIndex < 9 ? targetIndex + 36 : targetIndex;

        try {
            client.interactionManager.clickSlot(
                client.player.currentScreenHandler.syncId,
                slotSource,
                slotTarget,
                SlotActionType.SWAP,
                client.player
            );
            sendMessage(client, "Свап успішно!", 0x00FF00);
        } catch (Exception e) {
            sendMessage(client, "Помилка свапу: " + e.getMessage(), 0xFF0000);
        }
    }

    private void sendMessage(MinecraftClient client, String message, int color) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message).styled(style -> style.withColor(color)), false);
        }
    }
}
