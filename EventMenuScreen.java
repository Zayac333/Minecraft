package com.zayachub.zayacmulti;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class EventMenuScreen extends Screen {
    public EventMenuScreen() {
        super(Text.of("Налаштування Івентів"));
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100; // Центруємо кнопки
        int y = 25; // Початкова висота
        int step = 22; // Крок між кнопками для компактності

        // Список івентів
        addToggleButton("Вулкан", "вулкан", x, y);
        addToggleButton("Маяк Убийця", "убийца", x, y + step);
        addToggleButton("Гейзер", "гейзер", x, y + step * 2);
        addToggleButton("Метеоритний Дощ", "дождь", x, y + step * 3);
        
        // Нові івенти
        addToggleButton("Загадочный Маяк", "загадочный", x, y + step * 4);
        addToggleButton("Аир-дроп", "аир", x, y + step * 5);
        addToggleButton("Алтарь нежити", "алтарь", x, y + step * 6);

        // Кнопка "Зберегти" (закрити)
        this.addDrawableChild(ButtonWidget.builder(Text.of("§6Зберегти"), button -> this.close())
                .dimensions(x, y + step * 7 + 10, 200, 20).build());
    }

    private void addToggleButton(String label, String key, int x, int y) {
        boolean isEnabled = EventsModLogic.enabledEvents.contains(key);
        String status = isEnabled ? "§a✔" : "§c✖";
        
        this.addDrawableChild(ButtonWidget.builder(Text.of(status + " " + label), button -> {
            if (EventsModLogic.enabledEvents.contains(key)) {
                EventsModLogic.enabledEvents.remove(key);
            } else {
                EventsModLogic.enabledEvents.add(key);
            }
            this.clearAndInit(); // Оновити екран
        }).dimensions(x, y, 200, 20).build());
    }
}
