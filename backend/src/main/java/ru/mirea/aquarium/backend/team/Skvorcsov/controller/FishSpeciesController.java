package ru.mirea.aquarium.backend.team.Skvorcsov.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.dto.FishDtos.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.service.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/fish-species")
@Tag(name = "Виды рыб")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class FishSpeciesController {
    private final FishSpeciesService service;
    private final FishPermissions access;
    public FishSpeciesController(FishSpeciesService service, FishPermissions access) { this.service = service; this.access = access; }

    @GetMapping
    public PageResponse<SpeciesResponse> list(@RequestParam(required = false) Boolean enabled,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        access.viewer(); return PageResponse.from(service.list(enabled, page, size));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public SpeciesResponse create(@Valid @RequestBody SpeciesWrite data) { return service.create(data, access.admin()); }

    @PutMapping("/{id}")
    public SpeciesResponse update(@PathVariable UUID id, @Valid @RequestBody SpeciesUpdate data) {
        return service.update(id, data, access.admin());
    }
}
