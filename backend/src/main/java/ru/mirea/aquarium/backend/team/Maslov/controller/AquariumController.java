package ru.mirea.aquarium.backend.team.Maslov.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Аквариумы", description = "Создание, просмотр, изменение и архивирование аквариумов")
public class AquariumController {
    private final AquariumService service;
    public AquariumController(AquariumService service) { this.service = service; }
    @Operation(summary = "Получить страницу аквариумов", description = "Поддерживает поиск, фильтры, сортировку и серверную пагинацию.")
    @GetMapping public Page<AquariumResponse> list(@RequestParam(required=false) String q, @RequestParam(required=false) UUID clientId,
            @RequestParam(required=false) AquariumType type, @RequestParam(required=false) Boolean active,
            @RequestParam(required=false) Boolean archived, @RequestParam(required=false) Double minVolume,
            @RequestParam(required=false) Double maxVolume, @RequestParam(defaultValue="createdAt") String sort,
            @RequestParam(defaultValue="desc") String direction, @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        return service.list(q, clientId, type, active, archived, minVolume, maxVolume, sort, direction, page, size).map(AquariumResponse::from);
    }
    @Operation(summary = "Получить аквариум по UUID")
    @GetMapping("/{id}") public AquariumResponse get(@PathVariable UUID id) { return AquariumResponse.from(service.get(id)); }
    @Operation(summary = "Создать аквариум")
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public AquariumResponse create(@Valid @RequestBody AquariumRequest dto) { return AquariumResponse.from(service.create(dto)); }
    @Operation(summary = "Изменить аквариум", description = "В запросе передайте актуальную version из карточки аквариума.")
    @PutMapping("/{id}") public AquariumResponse update(@PathVariable UUID id, @Valid @RequestBody AquariumRequest dto) { return AquariumResponse.from(service.update(id, dto)); }
    @Operation(summary = "Архивировать аквариум")
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void archive(@PathVariable UUID id) { service.archive(id); }
    @Operation(summary = "Восстановить аквариум из архива")
    @PostMapping("/{id}/restore") public AquariumResponse restore(@PathVariable UUID id) { return AquariumResponse.from(service.restore(id)); }
}
