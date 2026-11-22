package ru.mirea.aquarium.backend.team.Skvorcsov.dto;

import jakarta.validation.constraints.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.domain.*;
import java.util.UUID;

public record FishFilter(
    @Size(max = 200) String q, UUID aquariumId, UUID speciesId, FishStatus status,
    Boolean requiresObservation, CarePriority carePriority, @Size(max = 40) String tag,
    ArchiveMode archive, QuantityOp quantityOp, @Min(1) @Max(1000000) Integer quantity) {
    public enum ArchiveMode { ACTIVE, ARCHIVED, ALL }
    public enum QuantityOp { EQ, GT, GTE, LT, LTE }

    public ArchiveMode archiveMode() { return archive == null ? ArchiveMode.ACTIVE : archive; }

    public void validate() {
        if ((quantityOp == null) != (quantity == null))
            throw new IllegalArgumentException("quantityOp и quantity передаются вместе");
    }
}
