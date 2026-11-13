package ru.mirea.aquarium.backend.team.Maslov.dto;

import jakarta.validation.constraints.*;
import ru.mirea.aquarium.backend.team.Maslov.domain.AquariumType;
import java.math.BigDecimal;
import java.util.UUID;

public record AquariumRequest(
        @NotBlank @Size(max = 32) String code,
        @NotNull UUID clientId,
        @NotBlank @Size(max = 120) String name,
        @NotNull AquariumType type,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal volumeLiters,
        @NotNull Boolean active,
        Long version) { }
