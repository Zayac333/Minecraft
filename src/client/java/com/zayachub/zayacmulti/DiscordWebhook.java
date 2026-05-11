package com.zayachub.zayacmulti;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.net.URI;

public class DiscordWebhook {
    public static void send(String webhookUrl, String message) {
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        // Використовуємо новий потік, щоб не лагала гра
        new Thread(() -> {
            try {
                URL url = URI.create(webhookUrl).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "Java-Discord-Webhook");
                connection.setDoOutput(true);

                // Формуємо JSON (замінюємо переноси рядків на \n для Discord)
                String json = "{\"content\": \"" + message.replace("\n", "\\n") + "\"}";
                
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                connection.getResponseCode();
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}