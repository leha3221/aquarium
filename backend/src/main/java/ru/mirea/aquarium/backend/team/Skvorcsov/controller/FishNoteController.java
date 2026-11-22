package ru.mirea.aquarium.backend.team.Skvorcsov.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.dto.FishDtos.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.service.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/fish/{fishId}/notes")
@Tag(name = "Заметки о рыбах")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@Validated
public class FishNoteController {
    private final FishNoteService service;
    private final FishPermissions access;
    public FishNoteController(FishNoteService service, FishPermissions access) { this.service = service; this.access = access; }

    @GetMapping
    public PageResponse<NoteResponse> list(@PathVariable UUID fishId, @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        access.viewer(); return PageResponse.from(service.list(fishId, page, size));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(@PathVariable UUID fishId, @Valid @RequestBody NoteWrite data) {
        return service.create(fishId, data, access.editor());
    }
    @PutMapping("/{id}")
    public NoteResponse update(@PathVariable UUID fishId, @PathVariable UUID id, @Valid @RequestBody NoteUpdate request) {
        return service.update(fishId, id, request, access.editor());
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID fishId, @PathVariable UUID id, @RequestParam @PositiveOrZero long version) {
        service.delete(fishId, id, version, access.editor());
    }
}
