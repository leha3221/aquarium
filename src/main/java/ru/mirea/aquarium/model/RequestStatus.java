package ru.mirea.aquarium.model;

public enum RequestStatus {
    NEW("Новая"), CONFIRMED("Подтверждена"), IN_PROGRESS("В работе"), COMPLETED("Выполнена"), CANCELLED("Отменена");

    private final String title;
    RequestStatus(String title) { this.title = title; }
    @Override public String toString() { return title; }
}
