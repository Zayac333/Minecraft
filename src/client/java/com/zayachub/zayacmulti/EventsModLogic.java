package com.zayachub.zayacmulti;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventsModLogic {
    public static volatile EventTarget currentEvent = null;
    private static final ScheduledExecutorService SCHED = Executors.newSingleThreadScheduledExecutor();
    private static volatile ScheduledFuture<?> clearFuture = null;

    // "Пам'ять" мода
    private static String pendingEventName = null;
    private static long pendingEventTime = 0;
    private static int pendingDuration = 0; // Зберігаємо знайдені секунди
    private static long pendingDurationTime = 0; // Коли ми востаннє бачили "Статус"

    public static final Set<String> enabledEvents = new HashSet<>();
    public static KeyBinding configKey;

    public static void init() {
        enabledEvents.add("убийца");
        enabledEvents.add("гейзер");
        enabledEvents.add("вулкан");
        enabledEvents.add("дождь");
        enabledEvents.add("загадочный");
        enabledEvents.add("аир");
        enabledEvents.add("алтарь");
        enabledEvents.add("смерти");

        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zayacmulti.events_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "category.zayacmulti.menu"
        ));
    }

    public static void onTick(MinecraftClient client) {
        while (configKey.wasPressed()) {
            client.setScreen(new EventMenuScreen());
        }
    }

    public static void renderHud(DrawContext drawContext) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || currentEvent == null) return;

        EventTarget e = currentEvent;
        Vec3d playerPos = mc.player.getPos();
        Vec3d targetPos = new Vec3d(e.x + 0.5, e.y + 0.5, e.z + 0.5);

        // Напрямок
        double dx = targetPos.x - playerPos.x;
        double dz = targetPos.z - playerPos.z;
        double targetAngle = Math.atan2(dz, dx);
        double yawRad = Math.toRadians(mc.player.getYaw());
        double relativeAngle = targetAngle - yawRad - Math.PI / 2;

        int screenW = mc.getWindow().getScaledWidth();
        int centerX = (screenW / 2) - 110;
        int topY = 75;

        TextRenderer tr = mc.textRenderer;

        // Стрілка
        drawContext.getMatrices().push();
        drawContext.getMatrices().translate((float)centerX, (float)topY, 0.0f);
        drawContext.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotation((float) relativeAngle));
        drawContext.drawText(tr, "▲", -tr.getWidth("▲") / 2, -4, 0xFFD700, true);
        drawContext.getMatrices().pop();

        // Текстова інформація
        drawContext.drawText(tr, "§6" + e.name, centerX - tr.getWidth(e.name) / 2, topY + 12, 0xFFFFFF, true);
        
        String distStr = (int) playerPos.distanceTo(targetPos) + "m";
        drawContext.drawText(tr, distStr, centerX - tr.getWidth(distStr) / 2, topY + 22, 0xCCCCCC, true);

        // ВІДОБРАЖЕННЯ ЧАСУ
        if (e.durationSeconds > 0) {
            long elapsed = (System.currentTimeMillis() - e.startTime) / 1000;
            long remaining = e.durationSeconds - elapsed;

            if (remaining > 0) {
                String timeStr = String.format("§e%d:%02d", remaining / 60, remaining % 60);
                drawContext.drawText(tr, timeStr, centerX - tr.getWidth(timeStr) / 2, topY + 32, 0xFFFFFF, true);
            }
        }
    }

    public static void handleEventMessage(Text text) {
    if (text == null) return;
    String message = text.getString();
    if (message.isEmpty() || message.contains("[Events]")) return;

    String cleanMessage = message.replaceAll("§[0-9a-fk-or]", "");
    String lower = cleanMessage.toLowerCase();

    // 1. ПЕРЕВІРКА НА НАЗВУ ІВЕНТУ
    for (String key : enabledEvents) {
        if (lower.contains(key)) {
            pendingEventName = getPrettyName(key);
            pendingEventTime = System.currentTimeMillis();
            break;
        }
    }

    // 2. ПЕРЕВІРКА НА СТАТУС (ЧАС) - ОБРОБКА ХВИЛИН ТА СЕКУНД
    if (lower.contains("статус")) {
        int totalSeconds = 0;
        
        // Шукаємо хвилини
        Pattern minPattern = Pattern.compile("(\\d+)\\s*мин");
        Matcher minMatcher = minPattern.matcher(lower);
        if (minMatcher.find()) {
            totalSeconds += Integer.parseInt(minMatcher.group(1)) * 60;
        }
        
        // Шукаємо секунди (цифри, після яких йде "сек", або просто цифри в кінці, якщо немає "мин")
        Pattern secPattern = Pattern.compile("(\\d+)\\s*сек");
        Matcher secMatcher = secPattern.matcher(lower);
        if (secMatcher.find()) {
            totalSeconds += Integer.parseInt(secMatcher.group(1));
        } else if (totalSeconds == 0) { 
            // Якщо не знайшли ні "мин", ні "сек", шукаємо просто будь-яке число (для формату "Статус: 377")
            Pattern simpleDigit = Pattern.compile("(\\d+)");
            Matcher digitMatcher = simpleDigit.matcher(lower);
            if (digitMatcher.find()) {
                totalSeconds = Integer.parseInt(digitMatcher.group(1));
            }
        }

        if (totalSeconds > 0) {
            pendingDuration = totalSeconds;
            pendingDurationTime = System.currentTimeMillis();
        }
    }

    // 3. ПЕРЕВІРКА НА КООРДИНАТИ (ФІНАЛ)
    Matcher m = Pattern.compile("(-?\\d+)").matcher(cleanMessage);
    int[] coords = new int[3];
    int count = 0;
    while (m.find() && count < 3) {
        try {
            coords[count] = Integer.parseInt(m.group(1));
            count++;
        } catch (NumberFormatException ignored) {}
    }

    if (count == 3) {
        long now = System.currentTimeMillis();
        if (pendingEventName != null && (now - pendingEventTime) < 4000) {
            int finalSeconds = 0;
            if ((now - pendingDurationTime) < 4000) {
                finalSeconds = pendingDuration;
            }

            currentEvent = new EventTarget(pendingEventName, coords[0], coords[1], coords[2], finalSeconds);
            logToChat("§aЦіль знайдено: §f" + currentEvent.name + (finalSeconds > 0 ? " §7(" + finalSeconds + "с)" : ""));
            
            pendingEventName = null;
            pendingDuration = 0;
            scheduleClear(finalSeconds > 0 ? finalSeconds + 10 : 300);
        }
    }
}

    private static String getPrettyName(String key) {
        if (key.contains("убийца")) return "Маяк Убийця";
        if (key.contains("вулкан")) return "Вулкан";
        if (key.contains("гейзер")) return "Гейзер";
        if (key.contains("дождь")) return "Метеоритний Дощ";
        if (key.contains("загадочный")) return "Загадковий Маяк";
        if (key.contains("аир")) return "Аір-дроп";
        if (key.contains("алтарь")) return "Вівтар нежиті";
        if (key.contains("смерти")) return "Сундук Смерті";
        return key;
    }

    private static void logToChat(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.of("§8[§6Events§8] " + text), false);
        }
    }

    private static synchronized void scheduleClear(int seconds) {
        if (clearFuture != null && !clearFuture.isDone()) clearFuture.cancel(false);
        clearFuture = SCHED.schedule(() -> currentEvent = null, seconds, TimeUnit.SECONDS);
    }

    public static class EventTarget {
        public final String name;
        public final int x, y, z;
        public final int durationSeconds;
        public final long startTime;

        public EventTarget(String name, int x, int y, int z, int durationSeconds) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.durationSeconds = durationSeconds;
            this.startTime = System.currentTimeMillis();
        }
    }
}