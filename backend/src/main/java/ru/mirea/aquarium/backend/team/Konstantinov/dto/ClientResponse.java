package ru.mirea.aquarium.backend.team.Konstantinov.dto;

import ru.mirea.aquarium.backend.team.Konstantinov.domain.Client;
import java.time.Instant;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        String fullName,
        String phone,
        String email,
        String address,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getFullName(),
                client.getPhone(),
                client.getEmail(),
                client.getAddress(),
                client.isActive(),
                client.getCreatedAt(),
                client.getUpdatedAt(),
                client.getVersion()
        );
    }
}