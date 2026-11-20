package ru.mirea.aquarium.backend.team.Konstantinov.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClientRequest(@NotBlank @Size(max = 120) String fullName, @NotBlank @Size(max = 30) String phone,
                            @Email @Size(max = 160) String email, @Size(max = 255) String address,
                            @NotNull Boolean active, Long version) {
}
