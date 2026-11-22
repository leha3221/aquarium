package ru.mirea.aquarium.backend.team.Skvorcsov.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.aquarium.backend.team.Skvorcsov.dto.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.dto.FishDtos.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.service.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/fish")
@Tag(name = "Рыбы", description = "Учет групп рыб одного вида в аквариуме")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@Validated
public class FishController {
    private final FishService fish;
    private final FishBulkService bulk;
    private final FishSummaryService summaries;
    private final FishCsvService csv;
    private final FishPermissions access;
    public FishController(FishService fish, FishBulkService bulk, FishSummaryService summaries,
        FishCsvService csv, FishPermissions access) {
        this.fish = fish; this.bulk = bulk; this.summaries = summaries; this.csv = csv; this.access = access;
    }

    @GetMapping
    @Operation(summary = "Поиск, фильтры и страница групп рыб")
    public PageResponse<FishResponse> list(@Valid @ModelAttribute FishFilter filter,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sort, @RequestParam(defaultValue = "desc") String direction) {
        access.viewer();
        return PageResponse.from(fish.list(filter, page, size, sort, direction));
    }

    @GetMapping("/{id}")
    public FishResponse get(@PathVariable UUID id) { access.viewer(); return fish.get(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public FishResponse create(@Valid @RequestBody FishWrite data) { return fish.create(data, access.editor()); }

    @PutMapping("/{id}")
    public FishResponse update(@PathVariable UUID id, @Valid @RequestBody FishUpdate request) {
        return fish.update(id, request, access.editor());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Архивировать группу; version обязательна")
    public FishResponse archive(@PathVariable UUID id, @RequestParam @PositiveOrZero long version) {
        return fish.archive(id, version, true, access.editor());
    }

    @PostMapping("/{id}/restore")
    public FishResponse restore(@PathVariable UUID id, @Valid @RequestBody VersionRequest request) {
        return fish.archive(id, request.version(), false, access.editor());
    }

    @DeleteMapping("/{id}/permanent") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam @PositiveOrZero long version) {
        fish.delete(id, version, access.admin());
    }

    @PostMapping("/bulk")
    public BulkResult bulk(@Valid @RequestBody BulkRequest request) { return bulk.apply(request, access.editor()); }

    @GetMapping("/summary")
    public Summary summary(@Valid @ModelAttribute FishFilter filter) {
        access.viewer(); return summaries.summary(filter);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(@Valid @ModelAttribute FishFilter filter,
        @RequestParam(defaultValue = "code") String sort, @RequestParam(defaultValue = "asc") String direction) {
        access.viewer();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"fish.csv\"")
            .body(csv.export(filter, sort, direction));
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportPreview preview(@RequestPart("file") MultipartFile file, @RequestParam ImportMode mode) {
        return csv.preview(file, mode, access.editor());
    }

    @PostMapping("/import/{batchId}/apply")
    public ImportResult apply(@PathVariable UUID batchId) { return csv.apply(batchId, access.editor()); }
}
