package ru.mirea.aquarium.backend.team.Konstantinov.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mirea.aquarium.backend.team.Konstantinov.dto.ClientRequest;
import ru.mirea.aquarium.backend.team.Konstantinov.dto.ClientResponse;
import ru.mirea.aquarium.backend.team.Konstantinov.service.ClientService;

import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
public class ClientController {
    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ClientResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(q, active, page, size).map(ClientResponse::from);
    }

    @GetMapping("/{id}")
    public ClientResponse get(@PathVariable UUID id) {
        return ClientResponse.from(service.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse create(@Valid @RequestBody ClientRequest request) {
        return ClientResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    public ClientResponse update(@PathVariable UUID id, @Valid @RequestBody ClientRequest request) {
        return ClientResponse.from(service.update(id, request));
    }

    @PatchMapping("/{id}/active")
    public ClientResponse active(@PathVariable UUID id, @RequestParam boolean value) {
        return ClientResponse.from(service.active(id, value));
    }
}