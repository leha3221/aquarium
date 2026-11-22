package ru.mirea.aquarium.backend.team.Skvorcsov.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.domain.*;
import java.time.Instant;
import java.util.*;

public final class FishDtos {
    private FishDtos() {}

    /** Stable JSON contract for the Android client; never serializes PageImpl itself. */
    public record PageResponse<T>(List<T> content, int number, int size, long totalElements,
        int totalPages, boolean first, boolean last) {
        public static <T> PageResponse<T> from(org.springframework.data.domain.Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
        }
    }

    public record FishWrite(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_-]{0,31}") String code,
        @NotNull UUID aquariumId, @NotNull UUID speciesId,
        @NotBlank @Size(max = 120) String name,
        @NotNull @Size(max = 1000) String description,
        @NotNull @Min(1) @Max(1000000) Integer quantity,
        @NotNull FishStatus status,
        @NotNull Boolean requiresObservation,
        @NotNull @Size(max = 20) Set<@NotBlank @Size(max = 40) String> tags) {}

    public record FishUpdate(@NotNull @Valid FishWrite data, @NotNull @PositiveOrZero Long version) {}
    public record VersionRequest(@NotNull @PositiveOrZero Long version) {}
    public record SpeciesWrite(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_-]{0,31}") String code,
        @NotBlank @Size(max = 120) String name,
        @NotNull @Size(max = 1000) String description,
        @NotNull Boolean enabled) {}
    public record SpeciesUpdate(@NotNull @Valid SpeciesWrite data, @NotNull @PositiveOrZero Long version) {}
    public record NoteWrite(@NotBlank @Size(max = 2000) String text, @NotNull Boolean pinned) {}
    public record NoteUpdate(@NotNull @Valid NoteWrite data, @NotNull @PositiveOrZero Long version) {}
    public record FishResponse(UUID id, String code, UUID aquariumId, String aquariumCode,
        UUID speciesId, String speciesCode, String speciesName, String name, String description,
        int quantity, FishStatus status, boolean requiresObservation, CarePriority carePriority,
        Set<String> tags, boolean archived, Instant createdAt, Instant updatedAt, long version) {
        public static FishResponse from(Fish f) {
            return new FishResponse(f.getId(), f.getCode(), f.getAquarium().getId(), f.getAquarium().getCode(),
                f.getSpecies().getId(), f.getSpecies().getCode(), f.getSpecies().getName(), f.getName(),
                f.getDescription(), f.getQuantity(), f.getStatus(), f.isRequiresObservation(), f.getCarePriority(),
                new TreeSet<>(f.getTags()), f.isArchived(), f.getCreatedAt(), f.getUpdatedAt(), f.getVersion());
        }
    }
    public record SpeciesResponse(UUID id, String code, String name, String description,
        boolean enabled, Instant createdAt, Instant updatedAt, long version) {
        public static SpeciesResponse from(FishSpecies s) {
            return new SpeciesResponse(s.getId(), s.getCode(), s.getName(), s.getDescription(),
                s.isEnabled(), s.getCreatedAt(), s.getUpdatedAt(), s.getVersion());
        }
    }
    public record NoteResponse(UUID id, UUID fishId, String text, String author, boolean pinned,
        Instant createdAt, Instant updatedAt, long version) {
        public static NoteResponse from(FishNote n) {
            return new NoteResponse(n.getId(), n.getFish().getId(), n.getText(), n.getAuthor(),
                n.isPinned(), n.getCreatedAt(), n.getUpdatedAt(), n.getVersion());
        }
    }
    public enum BulkAction { ARCHIVE, RESTORE, SET_STATUS, ADD_TAG, REMOVE_TAG }
    public record BulkItem(@NotNull UUID id, @NotNull @PositiveOrZero Long version) {}
    public record BulkRequest(@NotEmpty @Size(max = 100) List<@NotNull @Valid BulkItem> items,
        @NotNull BulkAction action, FishStatus status, @Size(max = 40) String tag) {}
    public record ItemError(UUID id, int status, String message) {}
    public record BulkResult(List<FishResponse> successes, List<ItemError> errors) {}
    public record GroupCount(String key, String name, long groups, long individuals) {}
    public record Summary(long total, long active, long archived, long requiresObservation,
        long individuals, double averageQuantity, List<GroupCount> bySpecies,
        List<GroupCount> byStatus, List<GroupCount> byCarePriority) {}
    public enum ImportMode { NEW_ONLY, UPSERT }
    public record CsvError(long row, String message) {}
    public record ImportRow(long row, UUID targetId, Long version, FishWrite data) {}
    public record ImportPreview(UUID batchId, int validRows, int invalidRows, Instant expiresAt,
        List<CsvError> errors) {}
    public record ImportResult(int created, int updated) {}
}
