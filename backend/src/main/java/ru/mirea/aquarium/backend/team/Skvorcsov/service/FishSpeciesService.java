package ru.mirea.aquarium.backend.team.Skvorcsov.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.aquarium.backend.team.Skvorcsov.domain.FishSpecies;
import ru.mirea.aquarium.backend.team.Skvorcsov.dto.FishDtos.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.repository.FishSpeciesRepository;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class FishSpeciesService {
    private final FishSpeciesRepository repository;
    private final FishService fishService;
    private final FishEvents events;

    public FishSpeciesService(FishSpeciesRepository repository, FishService fishService, FishEvents events) {
        this.repository = repository; this.fishService = fishService; this.events = events;
    }

    public Page<SpeciesResponse> list(Boolean enabled, int page, int size) {
        Specification<FishSpecies> spec = (r, q, cb) ->
            enabled == null ? cb.conjunction() : cb.equal(r.get("enabled"), enabled);
        return repository.findAll(spec, FishService.pageable(page, size, "name", "asc")).map(SpeciesResponse::from);
    }

    @Transactional
    public SpeciesResponse create(SpeciesWrite data, String actor) {
        fishService.validate(data);
        unique(data.code(), null);
        FishSpecies s = new FishSpecies();
        apply(s, data);
        repository.saveAndFlush(s);
        events.record(actor, "CREATE", "FISH_SPECIES", s.getId(), s.getCode());
        return SpeciesResponse.from(s);
    }

    @Transactional
    public SpeciesResponse update(UUID id, SpeciesUpdate data, String actor) {
        fishService.validate(data);
        FishSpecies s = repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Вид рыб не найден"));
        FishService.checkVersion(s, data.version());
        unique(data.data().code(), id);
        apply(s, data.data());
        s.touch();
        repository.flush();
        events.record(actor, "UPDATE", "FISH_SPECIES", id, s.getCode());
        return SpeciesResponse.from(s);
    }

    private void unique(String code, UUID id) {
        repository.findByCode(FishService.code(code)).filter(s -> !s.getId().equals(id))
            .ifPresent(s -> { throw new FishFailure(409, "Код вида уже занят"); });
    }

    private void apply(FishSpecies s, SpeciesWrite data) {
        s.setCode(FishService.code(data.code())); s.setName(data.name().strip());
        s.setDescription(data.description().strip()); s.setEnabled(data.enabled());
    }
}
