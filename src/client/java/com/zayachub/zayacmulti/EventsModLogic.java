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

    private static String pendingEventName = null;
    private static long pendingEventTime = 0;

    public static final Set<String> enabledEvents = new HashSet<>();
    public static KeyBinding configKey;

    // Змінено на static init(), щоб викликати з головного класу
    public static void init() {
        enabledEvents.add("убийца");
        enabledEvents.add("гейзер");
        enabledEvents.add("вулкан");
        enabledEvents.add("дождь");
        enabledEvents.add("загадочный");
        enabledEvents.add("аир");
        enabledEvents.add("алтарь");

        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zayacmulti.events_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "category.zayacmulti.menu"
        ));
    }

    // Метод для тіку (викликається в ZayacHubClient)
    public static void onTick(MinecraftClient client) {
        while (configKey.wasPressed()) {
            client.setScreen(new EventMenuScreen());
        }
    }

    // Метод для рендерингу (викликається в ZayacHubClient)
    public static void renderHud(DrawContext drawContext) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || currentEvent == null) return;

        EventTarget e = currentEvent;
        Vec3d playerPos = mc.player.getPos();
        Vec3d targetPos = new Vec3d(e.x + 0.5, e.y + 0.5, e.z + 0.5);

        double dx = targetPos.x - playerPos.x;
        double dz = targetPos.z - playerPos.z;
        double targetAngle = Math.atan2(dz, dx);
        double yawRad = Math.toRadians(mc.player.getYaw());
        double relativeAngle = targetAngle - yawRad - Math.PI / 2;

        int screenW = mc.getWindow().getScaledWidth();
        int centerX = (screenW / 2) - 110;
        int topY = 75;

        TextRenderer tr = mc.textRenderer;
        drawContext.getMatrices().push();
        drawContext.getMatrices().translate((float)centerX, (float)topY, 0.0f);
        drawContext.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotation((float) relativeAngle));
        drawContext.drawText(tr, "▲", -tr.getWidth("▲") / 2, -4, 0xFFD700, true);
        drawContext.getMatrices().pop();

        drawContext.drawText(tr, "§6" + e.name, centerX - tr.getWidth(e.name) / 2, topY + 12, 0xFFFFFF, true);
        String distStr = (int) playerPos.distanceTo(targetPos) + "m";
        drawContext.drawText(tr, distStr, centerX - tr.getWidth(distStr) / 2, topY + 22, 0xCCCCCC, true);
    }

    public static void handleEventMessage(Text text) {
        if (text == null) return;
        String message = text.getString();
        
        if (message.isEmpty() || message.contains("[Events]")) return;

        String cleanMessage = message.replaceAll("§[0-9a-fk-or]", "");
        String lower = cleanMessage.toLowerCase();

        String detectedKey = null;
        for (String key : enabledEvents) {
            if (lower.contains(key)) {
                detectedKey = key;
                break;
            }
        }

        if (detectedKey != null) {
            pendingEventName = getPrettyName(detectedKey);
            pendingEventTime = System.currentTimeMillis();
        }

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
            if (pendingEventName != null && (System.currentTimeMillis() - pendingEventTime) < 3000) {
                currentEvent = new EventTarget(pendingEventName, coords[0], coords[1], coords[2]);
                logToChat("§aЦіль знайдено: §f" + currentEvent.name);
                pendingEventName = null;
                scheduleClear(300);
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
        public EventTarget(String name, int x, int y, int z) { 
            this.name = name; this.x = x; this.y = y; this.z = z; 
        }
    }
}