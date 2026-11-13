package ru.mirea.aquarium.backend.team.Maslov.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.aquarium.backend.team.Maslov.domain.*;
import ru.mirea.aquarium.backend.team.Maslov.dto.AquariumRequest;
import ru.mirea.aquarium.backend.team.Maslov.repository.AquariumRepository;
import java.util.*;

@Service
public class AquariumService {
    private static final Set<String> SORT_FIELDS = Set.of("name", "code", "volumeLiters", "createdAt", "updatedAt");
    private final AquariumRepository repository;
    public AquariumService(AquariumRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public Page<Aquarium> list(String q, UUID clientId, AquariumType type, Boolean active, Boolean archived,
                               Double minVolume, Double maxVolume, String sort, String direction, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Страница должна быть >= 0, размер — от 1 до 100");
        if (!SORT_FIELDS.contains(sort)) throw new IllegalArgumentException("Недопустимое поле сортировки");
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Specification<Aquarium> spec = (root, query, cb) -> cb.conjunction();
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((r, query, cb) -> cb.or(cb.like(cb.lower(r.get("name")), pattern), cb.like(cb.lower(r.get("code")), pattern)));
        }
        if (clientId != null) spec = spec.and((r, query, cb) -> cb.equal(r.get("clientId"), clientId));
        if (type != null) spec = spec.and((r, query, cb) -> cb.equal(r.get("type"), type));
        if (active != null) spec = spec.and((r, query, cb) -> cb.equal(r.get("active"), active));
        if (archived != null) spec = spec.and((r, query, cb) -> cb.equal(r.get("archived"), archived));
        if (minVolume != null) spec = spec.and((r, query, cb) -> cb.ge(r.get("volumeLiters"), minVolume));
        if (maxVolume != null) spec = spec.and((r, query, cb) -> cb.le(r.get("volumeLiters"), maxVolume));
        return repository.findAll(spec, PageRequest.of(page, size, Sort.by(dir, sort)));
    }
    @Transactional(readOnly = true) public Aquarium get(UUID id) { return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Аквариум не найден")); }
    @Transactional public Aquarium create(AquariumRequest dto) {
        if (repository.existsByCode(dto.code())) throw new IllegalArgumentException("Код аквариума уже занят");
        Aquarium a = new Aquarium(); apply(a, dto); return repository.save(a);
    }
    @Transactional public Aquarium update(UUID id, AquariumRequest dto) {
        Aquarium a = get(id);
        if (dto.version() == null || a.getVersion() != dto.version()) throw new OptimisticLockException("Запись уже изменена другим пользователем");
        apply(a, dto); return repository.saveAndFlush(a);
    }
    @Transactional public void archive(UUID id) { get(id).setArchived(true); }
    @Transactional public Aquarium restore(UUID id) { Aquarium a = get(id); a.setArchived(false); return a; }
    private void apply(Aquarium a, AquariumRequest d) { a.setCode(d.code().trim()); a.setClientId(d.clientId()); a.setName(d.name().trim()); a.setType(d.type()); a.setVolumeLiters(d.volumeLiters()); a.setActive(d.active()); }
}
