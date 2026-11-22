package ru.mirea.aquarium.backend.team.Skvorcsov.domain;

/** A service priority, not a medical diagnosis. */
public enum CarePriority {
    LOW, MEDIUM, HIGH;

    public static CarePriority calculate(FishStatus status, boolean requiresObservation) {
        if (status == FishStatus.TREATMENT) return HIGH;
        if (status == FishStatus.ADAPTING || status == FishStatus.QUARANTINE || requiresObservation) return MEDIUM;
        return LOW;
    }
}
