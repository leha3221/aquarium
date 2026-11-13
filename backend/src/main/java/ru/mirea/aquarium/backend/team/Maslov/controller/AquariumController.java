package ru.mirea.aquarium.backend.team.Maslov.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mirea.aquarium.backend.team.Maslov.domain.*;
import ru.mirea.aquarium.backend.team.Maslov.dto.AquariumRequest;
import ru.mirea.aquarium.backend.team.Maslov.dto.AquariumResponse;
import ru.mirea.aquarium.backend.team.Maslov.service.AquariumService;
import java.util.UUID;

@RestController
@RequestMapping("/api/aquariums")
public class AquariumController {
    private final AquariumService service;
    public AquariumController(AquariumService service) { this.service = service; }
    @GetMapping public Page<AquariumResponse> list(@RequestParam(required=false) String q, @RequestParam(required=false) UUID clientId,
            @RequestParam(required=false) AquariumType type, @RequestParam(required=false) Boolean active,
            @RequestParam(required=false) Boolean archived, @RequestParam(required=false) Double minVolume,
            @RequestParam(required=false) Double maxVolume, @RequestParam(defaultValue="createdAt") String sort,
            @RequestParam(defaultValue="desc") String direction, @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        return service.list(q, clientId, type, active, archived, minVolume, maxVolume, sort, direction, page, size).map(AquariumResponse::from);
    }
    @GetMapping("/{id}") public AquariumResponse get(@PathVariable UUID id) { return AquariumResponse.from(service.get(id)); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public AquariumResponse create(@Valid @RequestBody AquariumRequest dto) { return AquariumResponse.from(service.create(dto)); }
    @PutMapping("/{id}") public AquariumResponse update(@PathVariable UUID id, @Valid @RequestBody AquariumRequest dto) { return AquariumResponse.from(service.update(id, dto)); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void archive(@PathVariable UUID id) { service.archive(id); }
    @PostMapping("/{id}/restore") public AquariumResponse restore(@PathVariable UUID id) { return AquariumResponse.from(service.restore(id)); }
}
