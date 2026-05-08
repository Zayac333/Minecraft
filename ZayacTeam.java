package com.zayachub.zayaclan;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WaypointsModClient implements ClientModInitializer {
    public static KeyBinding toggleAutoCoordsKey;
    public static final AtomicBoolean autoCoordsEnabled = new AtomicBoolean(false);
    
    // Масив на 2 цілі (дві стрілки)
    public static final TargetInfo[] targets = new TargetInfo[2];
    private int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        // Реєстрація клавіші F9
        toggleAutoCoordsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zayachub.toggle_auto_coords",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
                "category.zayachub.waypoints"
        ));

        // Обробка системних повідомлень та чату
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            handleChatMessage(message.getString());
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) handleChatMessage(message.getString());
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Логіка перемикання радара
            while (toggleAutoCoordsKey.wasPressed()) {
                boolean newVal = !autoCoordsEnabled.get();
                autoCoordsEnabled.set(newVal);
                client.player.sendMessage(Text.of("§7[Radar] §fРадар: " + (newVal ? "§aУВІМКНЕНО" : "§cВИМКНЕНО")), false);
            }

            // Автоматичне прописування команди кожні 5 секунд
            if (autoCoordsEnabled.get() && client.getNetworkHandler() != null) {
                tickCounter++;
                if (tickCounter >= 100) { 
                    tickCounter = 0;
                    client.getNetworkHandler().sendChatCommand("clan coords");
                }
            }

            // Очищення старих міток через 60 секунд
            long now = System.currentTimeMillis();
            for (int i = 0; i < targets.length; i++) {
                if (targets[i] != null && (now - targets[i].timestamp) > 60000) {
                    targets[i] = null;
                }
            }
        });

        // Рендеринг стрілок на екрані
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;

            TextRenderer tr = mc.textRenderer;
            int screenW = mc.getWindow().getScaledWidth();

            for (int i = 0; i < targets.length; i++) {
                TargetInfo t = targets[i];
                if (t == null) continue;

                // Позиціювання: перша стрілка -140 від центру, друга -240 (лівіше)
                int offsetX = (i == 0) ? -250 : -350;
                int centerX = (screenW / 2) + offsetX;
                int topY = 75; 

                Vec3d playerPos = mc.player.getPos();
                Vec3d targetPos = new Vec3d(t.x + 0.5, t.y + 0.5, t.z + 0.5);

                double dx = targetPos.x - playerPos.x;
                double dz = targetPos.z - playerPos.z;
                double targetAngle = Math.atan2(dz, dx);
                double yawRad = Math.toRadians(mc.player.getYaw());
                double relativeAngle = targetAngle - yawRad - Math.PI / 2;

                drawContext.getMatrices().push();
                drawContext.getMatrices().translate((float)centerX, (float)topY, 0.0f);
                drawContext.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotation((float) relativeAngle));
                
                int color = (i == 0) ? 0x00FF00 : 0x00AAAA; // Зелений для першої, бірюзовий для другої
                drawContext.drawText(tr, "▲", -tr.getWidth("▲") / 2, -4, color, true);
                drawContext.getMatrices().pop();

                // Текст з нікнеймом та дистанцією
                drawContext.drawText(tr, "§b" + t.name, centerX - tr.getWidth(t.name) / 2, topY + 12, 0xFFFFFF, true);
                String distStr = (int) playerPos.distanceTo(targetPos) + "m";
                drawContext.drawText(tr, distStr, centerX - tr.getWidth(distStr) / 2, topY + 22, 0xAAAAAA, true);
            }
        });
    }

    public static void handleChatMessage(String message) {
        if (message == null || message.isEmpty()) return;
        
        // Видаляємо кольори Minecraft (§) для коректного пошуку
        String clean = message.replaceAll("§[0-9a-fk-or]", "").trim();
        String lower = clean.toLowerCase();

        // Обробляємо тільки якщо рядок містить назву світу
        if (lower.contains("world") || lower.contains("lobby") || lower.contains("nether")) {
            try {
                // ПРАВИЛЬНА РЕГУЛЯРКА: Шукаємо координати ТІЛЬКИ ПІСЛЯ назви локації
                // Це ігнорує будь-які цифри в нікнеймах на початку рядка
                Pattern p = Pattern.compile("(?:lobby|world|nether)[^0-9-]+(-?\\d+)[^0-9-]+(-?\\d+)[^0-9-]+(-?\\d+)");
                Matcher m = p.matcher(lower);
                
                if (m.find()) {
                    int x = Integer.parseInt(m.group(1));
                    int y = Integer.parseInt(m.group(2));
                    int z = Integer.parseInt(m.group(3));

                    // Пошук нікнейму: беремо перше слово, що не є стрілкою [↑] чи світом
                    String name = "Clanmate";
                    String[] words = clean.split("[\\s\\(\\[;]+");
                    for (String word : words) {
                        String w = word.trim();
                        if (w.length() > 2 && !w.contains("↑") && 
                            !w.equalsIgnoreCase("lobby") && 
                            !w.equalsIgnoreCase("world") && 
                            !w.equalsIgnoreCase("nether")) {
                            name = w.replace(":", "");
                            break;
                        }
                    }

                    updateTargets(name, x, y, z);
                }
            } catch (Exception ignored) {}
        }
    }

    private static void updateTargets(String name, int x, int y, int z) {
        // Якщо цей гравець вже є в списку, оновлюємо його координати
        for (int i = 0; i < targets.length; i++) {
            if (targets[i] != null && targets[i].name.equals(name)) {
                targets[i] = new TargetInfo(name, x, y, z);
                return;
            }
        }
        // Якщо гравця немає, шукаємо вільне місце для нової стрілки
        for (int i = 0; i < targets.length; i++) {
            if (targets[i] == null) {
                targets[i] = new TargetInfo(name, x, y, z);
                return;
            }
        }
        // Якщо обидва слоти зайняті, видаляємо старий і ставимо новий (циклічно)
        targets[0] = targets[1];
        targets[1] = new TargetInfo(name, x, y, z);
    }

    public static class TargetInfo {
        public final String name;
        public final int x, y, z;
        public final long timestamp;
        public TargetInfo(String name, int x, int y, int z) {
            this.name = name; this.x = x; this.y = y; this.z = z;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
