package ru.mirea.aquarium.backend.team.Maslov.dto;

import ru.mirea.aquarium.backend.team.Maslov.domain.Aquarium;
import ru.mirea.aquarium.backend.team.Maslov.domain.AquariumType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AquariumResponse(UUID id, String code, UUID clientId, String name, AquariumType type,
                               BigDecimal volumeLiters, boolean active, boolean archived,
                               Instant createdAt, Instant updatedAt, long version) {
    public static AquariumResponse from(Aquarium a) {
        return new AquariumResponse(a.getId(), a.getCode(), a.getClientId(), a.getName(), a.getType(),
                a.getVolumeLiters(), a.isActive(), a.isArchived(), a.getCreatedAt(), a.getUpdatedAt(), a.getVersion());
    }
}
