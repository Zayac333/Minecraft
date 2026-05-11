package com.zayachub.zayacmulti;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class EventScannerLogic {
    public static boolean isScanning = false;
    public static int startAnarchy, endAnarchy, currentAnarchy;
    public static String webhookUrl = "";
    
    private static StringBuilder reportBuilder = new StringBuilder();
    private static long nextStepTime = 0;
    private static int stage = 0; 
    private static boolean skipCurrentAnarchy = false;
    private static int anarchyCountInReport = 0; // Лічильник для групування

    public static void startScan(int start, int end, String webhook) {
        startAnarchy = start;
        endAnarchy = end;
        currentAnarchy = start;
        webhookUrl = webhook;
        skipCurrentAnarchy = false;
        anarchyCountInReport = 0;
        
        reportBuilder = new StringBuilder();
        reportBuilder.append("📊 **Звіт по івентах (Анархії ").append(start).append("-").append(end).append(")**\n");
        
        isScanning = true;
        stage = 0;
    }

    public static void onTick(MinecraftClient client) {
        if (!isScanning || client.player == null) return;

        long currentTime = System.currentTimeMillis();

        if (skipCurrentAnarchy) {
            stage = 3; 
            skipCurrentAnarchy = false;
        }

        if (currentTime < nextStepTime) return;

        switch (stage) {
            case 0: // Вхід
                client.player.networkHandler.sendChatCommand("an" + currentAnarchy);
                nextStepTime = currentTime + 350; 
                stage = 1;
                break;

            case 1: // Запит
                client.player.networkHandler.sendChatCommand("event delay");
                nextStepTime = currentTime + 400; 
                stage = 2;
                break;

            case 2: // Очікування
                nextStepTime = currentTime + 350; 
                stage = 3;
                break;

            case 3: // Логіка переходу та групування
                anarchyCountInReport++;

                // Якщо це 10-та анархія або остання в списку — надсилаємо групу
                if (anarchyCountInReport >= 10 || currentAnarchy >= endAnarchy) {
                    String finalReport = reportBuilder.toString();
                    // Надсилаємо тільки якщо знайшли хоч щось корисне в цій групі
                    if (finalReport.contains("📍") || finalReport.contains("⏳")) {
                        DiscordWebhook.send(webhookUrl, finalReport);
                    }
                    
                    // Скидаємо звіт та лічильник для наступної десятки
                    reportBuilder = new StringBuilder();
                    anarchyCountInReport = 0;
                }

                if (currentAnarchy >= endAnarchy) {
                    finishScan();
                } else {
                    currentAnarchy++;
                    // Додаємо заголовок для наступної анархії в поточний StringBuilder
                    reportBuilder.append("\n🌍 **AN-").append(currentAnarchy).append("**\n");
                    stage = 0;
                    nextStepTime = currentTime + 500;
                }
                break;
        }
    }

    public static void handleScannerMessage(String rawMessage) {
        if (!isScanning) return;
        
        String messageWithoutColors = rawMessage.replaceAll("§[0-9a-fk-orx]", "");
        // Очищення від !!, #, ┃ та іншого сміття
        String cleanLine = messageWithoutColors.replaceAll("^[!#|┃\\s\\d\\[\\]]+", "").trim();

        if (cleanLine.isEmpty()) return;
        String lower = cleanLine.toLowerCase();

        if (lower.contains("сервер заполнен") || lower.contains("вы были кикнуты")) {
            skipCurrentAnarchy = true;
            return;
        }

        // Перевірка часу (через / до следующего)
        if (lower.contains("через") || lower.contains("до следующего")) {
            if (lower.contains("мин")) {
                try {
                    String[] parts = lower.split(" ");
                    for (int i = 0; i < parts.length; i++) {
                        if (parts[i].contains("мин")) {
                            int minutes = Integer.parseInt(parts[i-1].replaceAll("[^0-9]", ""));
                            if (minutes <= 5) reportBuilder.append("⏳ ").append(cleanLine).append("\n");
                            return;
                        }
                    }
                } catch (Exception ignored) {}
            } else if (lower.contains("сек")) {
                reportBuilder.append("⏳ ").append(cleanLine).append("\n");
                return;
            }
            return;
        }

        String eventDisplayName = getFormattedEventName(lower);
        boolean isDataLine = lower.contains("координаты") || lower.contains("статус") || lower.contains("дроп");

        if (eventDisplayName != null || isDataLine) {
            if (eventDisplayName != null && !isDataLine) {
                reportBuilder.append("📍 **").append(eventDisplayName).append("**\n");
            } else if (!lower.contains("через")) { 
                reportBuilder.append("    ").append(cleanLine).append("\n");
            }
        }
    }

    private static String getFormattedEventName(String key) {
        if (key.contains("убийца")) return "Маяк Убийця";
        if (key.contains("вулкан")) return "Вулкан";
        if (key.contains("гейзер")) return "Гейзер";
        if (key.contains("дождь")) return "Метеоритний Дощ";
        if (key.contains("смерти")) return "Сундук Смерті";
        if (key.contains("адская")) return "Адська Різня";
        if (key.contains("аир")) return "Аір-дроп";
        if (key.contains("мистический")) return "Мистический сундук";
        if (key.contains("алтарь")) return "Вівтар нежиті";
        return null;
    }

    private static void finishScan() {
        isScanning = false;
        sendMessage("§6[Scanner] §aГрупове сканування завершено!");
    }

    private static void sendMessage(String text) {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(Text.literal(text), false);
        }
    }
}