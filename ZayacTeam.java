package com.zayachub.waypoints;

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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WaypointsModClient implements ClientModInitializer {
    public static KeyBinding toggleAutoCoordsKey;
    public static AtomicBoolean autoCoordsEnabled = new AtomicBoolean(false);
    public static volatile TargetInfo currentTarget = null;

    private int tickCounter = 0;
    private static final ScheduledExecutorService SCHED = Executors.newSingleThreadScheduledExecutor();
    private static volatile ScheduledFuture<?> clearFuture = null;

    @Override
    public void onInitializeClient() {
        toggleAutoCoordsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zayachub.toggle_auto_coords",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
                "category.zayachub.waypoints"
        ));

        // Обробка повідомлень чату (для отримання координат)
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            handleChatMessage(message.getString());
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) handleChatMessage(message.getString());
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleAutoCoordsKey.wasPressed()) {
                boolean newVal = !autoCoordsEnabled.get();
                autoCoordsEnabled.set(newVal);
                if (client.player != null) {
                    client.player.sendMessage(Text.of("§7[Radar] §fAuto /clan coords: " + (newVal ? "§aON" : "§cOFF")), false);
                }
            }

            if (autoCoordsEnabled.get() && client.player != null) {
                tickCounter++;
                if (tickCounter >= 100) { // 5 секунд
                    tickCounter = 0;
                    if (client.getNetworkHandler() != null) {
                        client.getNetworkHandler().sendChatCommand("clan coords");
                    }
                }
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || currentTarget == null) return;

            TargetInfo t = currentTarget;
            Vec3d playerPos = mc.player.getPos();
            Vec3d targetPos = new Vec3d(t.x + 0.5, t.y + 0.5, t.z + 0.5);

            double dx = targetPos.x - playerPos.x;
            double dz = targetPos.z - playerPos.z;
            
            // Математичний кут на ціль
            double targetAngle = Math.atan2(dz, dx);
            
            // ПРАВИЛЬНИЙ РОЗРАХУНОК КУТА (без дублювання змінних)
            double yawRad = Math.toRadians(mc.player.getYaw());
            double relativeAngle = targetAngle - yawRad - Math.PI / 2;

            int screenW = mc.getWindow().getScaledWidth();
            int centerX = screenW / 2;
            int topY = 35; 

            TextRenderer tr = mc.textRenderer;
            String arrow = "▲";
            int arrowW = tr.getWidth(arrow);

            // Малювання стрілки
            drawContext.getMatrices().push();
            drawContext.getMatrices().translate(centerX, topY, 0);
            
            // Використовуємо rotation() для 1.21.4
            drawContext.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotation((float) relativeAngle));
            
            drawContext.drawText(tr, arrow, -arrowW / 2, -4, 0xFFAAFF, true);
            drawContext.getMatrices().pop();

            // Текст під стрілкою
            drawContext.drawText(tr, t.name, centerX - tr.getWidth(t.name) / 2, topY + 15, 0xFFFFFF, true);

            String distStr = (int) playerPos.distanceTo(targetPos) + "m";
            drawContext.drawText(tr, distStr, centerX - tr.getWidth(distStr) / 2, topY + 27, 0xCCCCCC, true);
        });
    }

 public static void handleChatMessage(String message) {
    if (message == null || message.isEmpty()) return;
    
    String lowerMessage = message.toLowerCase();
    
    // 1. ПЕРЕВІРКА НА ПРИСУТНІСТЬ СВІТУ
    // Тепер ми продовжуємо ТІЛЬКИ якщо повідомлення МІСТИТЬ ці слова
    boolean isLegitCoords = lowerMessage.contains("lobby") || 
                            lowerMessage.contains("world") || 
                            lowerMessage.contains("nether");

    if (!isLegitCoords) {
        return; // Якщо назви світу немає — ігноруємо повідомлення (це може бути маяк або просто цифри)
    }

    try {
        // Регулярка для пошуку трьох чисел
        Matcher nums = Pattern.compile("(-?\\d+);?\\s+(-?\\d+);?\\s+(-?\\d+)").matcher(message);
        
        if (nums.find()) {
            int x = Integer.parseInt(nums.group(1));
            int y = Integer.parseInt(nums.group(2));
            int z = Integer.parseInt(nums.group(3));

            // Захист від помилкових цифр (висота в Minecraft не може бути такою)
            if (y > 320 || y < -64) return;

            // Спроба знайти нікнейм
            String name = "Clanmate";
            String cleaned = message.replaceAll("\\[.*?\\]", "").trim();
            String[] parts = cleaned.split("\\s+");
            
            if (parts.length > 0) {
                for (String part : parts) {
                    // Шукаємо перше слово, яке не є координатою і не назвою світу
                    String pLow = part.toLowerCase();
                    if (part.length() > 2 && !part.contains(";") && 
                        !pLow.contains("world") && !pLow.contains("lobby") && !pLow.contains("nether")) {
                        name = part.replace(":", "");
                        break;
                    }
                }
            }

            currentTarget = new TargetInfo(name, x, y, z);
            scheduleClearTargetInSeconds(60); // 1 хвилина, щоб бачити ціль
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    private static synchronized void scheduleClearTargetInSeconds(int seconds) {
        if (clearFuture != null && !clearFuture.isDone()) clearFuture.cancel(false);
        clearFuture = SCHED.schedule(() -> currentTarget = null, seconds, TimeUnit.SECONDS);
    }

    public static class TargetInfo {
        public final String name;
        public final int x, y, z;
        public TargetInfo(String name, int x, int y, int z) {
            this.name = name; this.x = x; this.y = y; this.z = z;
        }
    }
}
