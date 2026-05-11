package com.zayachub.zayacmulti.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ConfigManager {
    // Створюємо екземпляр GSON для роботи з JSON форматом
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // Шлях до файлу конфігурації у папці config
    private static final File file = new File("config/zayacmulti.json");
    
    // Статичний об'єкт конфігу, до якого ми звертаємося з усього моду
    public static ZayacConfig config = new ZayacConfig();

    /**
     * Завантажує налаштування з файлу на диску в об'єкт config.
     * Якщо файл не існує, використовуються значення за замовчуванням.
     */
    public static void load() {
        try {
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                config = gson.fromJson(reader, ZayacConfig.class);
                reader.close();
            }
        } catch (Exception e) {
            System.err.println("[ZayacHub] Помилка при завантаженні конфігу!");
            e.printStackTrace();
        }
    }

    /**
     * Зберігає поточні значення об'єкта config у файл на диску.
     */
    public static void save() {
        try {
            // Створюємо папку config, якщо її ще немає
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            
            FileWriter writer = new FileWriter(file);
            gson.toJson(config, writer);
            writer.close();
        } catch (Exception e) {
            System.err.println("[ZayacHub] Помилка при збереженні конфігу!");
            e.printStackTrace();
        }
    }
}