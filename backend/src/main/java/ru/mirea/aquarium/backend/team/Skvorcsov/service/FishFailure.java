package ru.mirea.aquarium.backend.team.Skvorcsov.service;

public class FishFailure extends RuntimeException {
    private final int status;
    public FishFailure(int status, String message) { super(message); this.status = status; }
    public int status() { return status; }
}
