package ru.mirea.aquarium.backend.team.Maslov.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;

public enum AquariumType {
    FRESHWATER("Пресноводный"), MARINE("Морской"), REPTILE("Для рептилий"), PLANT("Растительный");

    private final String title;
    AquariumType(String title) { this.title = title; }
    @JsonValue public String title() { return title; }
    @JsonCreator public static AquariumType fromJson(String value) {
        return Arrays.stream(values()).filter(v -> v.name().equalsIgnoreCase(value) || v.title.equalsIgnoreCase(value))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Неизвестный тип аквариума"));
    }
}
