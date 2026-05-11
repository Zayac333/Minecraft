package com.zayachub.zayacmulti.config;

import java.util.HashSet;
import java.util.Set;

public class ZayacConfig {
    public String sourceItem = "TALISMAN";
    public String targetItem = "SPHERE";
    public boolean radarEnabled = false;
    
    // Поле для збереження списку увімкнених івентів
    public Set<String> enabledEvents = new HashSet<>();
    
    // Нове поле для збереження Discord Webhook URL
    public String discordWebhook = ""; 
}