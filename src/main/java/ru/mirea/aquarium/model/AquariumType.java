package ru.mirea.aquarium.model;

public enum AquariumType {
    FRESHWATER("Пресноводный"), MARINE("Морской"), REPTILE("Для рептилий"), PLANT("Растительный");

    private final String title;
    AquariumType(String title) { this.title = title; }
    @Override public String toString() { return title; }
}
