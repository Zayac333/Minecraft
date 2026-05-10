package com.zayachub.zayacmulti.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ConfigManager {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final File file = new File("config/zayacmulti.json");
    public static ZayacConfig config = new ZayacConfig();

    public static void load() {
        try {
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                config = gson.fromJson(reader, ZayacConfig.class);
                reader.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            file.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(file);
            gson.toJson(config, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}