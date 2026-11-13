package ru.mirea.aquarium.model;

public enum ServiceType {
    CLEANING("Чистка аквариума"), WATER_CHANGE("Подмена воды"), EQUIPMENT_REPAIR("Ремонт оборудования"),
    FISH_HEALTH("Осмотр рыб"), AQUASCAPING("Оформление аквариума");

    private final String title;
    ServiceType(String title) { this.title = title; }
    @Override public String toString() { return title; }
}
