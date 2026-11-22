package ru.mirea.aquarium.backend.team.Skvorcsov.service;

import jakarta.persistence.*;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.aquarium.backend.team.Maslov.domain.AquariumType;
import ru.mirea.aquarium.backend.team.Skvorcsov.domain.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.dto.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.dto.FishDtos.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.repository.*;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class FishService {
    private static final Set<String> SORT_FIELDS = Set.of("code", "name", "quantity", "createdAt", "updatedAt");
    private final FishRepository fish;
    private final FishSpeciesRepository species;
    private final FishAquariumRepository aquariums;
    private final Validator validator;
    private final EntityManager entityManager;
    private final FishEvents events;
    private final boolean permanentDeleteEnabled;

    public FishService(FishRepository fish, FishSpeciesRepository species, FishAquariumRepository aquariums,
        Validator validator, EntityManager entityManager, FishEvents events,
        @Value("${app.fish.permanent-delete-enabled:false}") boolean permanentDeleteEnabled) {
        this.fish = fish; this.species = species; this.aquariums = aquariums;
        this.validator = validator; this.entityManager = entityManager; this.events = events;
        this.permanentDeleteEnabled = permanentDeleteEnabled;
    }

    public Page<FishResponse> list(FishFilter filter, int page, int size, String sort, String direction) {
        validate(filter);
        return fish.findAll(FishSpecifications.matching(filter), pageable(page, size, sort, direction)).map(FishResponse::from);
    }

    public FishResponse get(UUID id) { return FishResponse.from(entity(id)); }

    @Transactional
    public FishResponse create(FishWrite data, String actor) {
        validateWrite(data, null);
        Fish f = new Fish();
        apply(f, data);
        fish.saveAndFlush(f);
        events.record(actor, "CREATE", "FISH", f.getId(), "Создана группа " + f.getCode());
        return FishResponse.from(f);
    }

    @Transactional
    public FishResponse update(UUID id, FishUpdate request, String actor) {
        validate(request);
        Fish f = entity(id);
        checkVersion(f, request.version());
        if (f.isArchived()) throw new FishFailure(409, "Сначала восстановите группу из архива");
        validateWrite(request.data(), f);
        apply(f, request.data());
        f.touch();
        fish.flush();
        events.record(actor, "UPDATE", "FISH", id, "Изменена группа " + f.getCode());
        return FishResponse.from(f);
    }

    @Transactional
    public FishResponse archive(UUID id, long version, boolean archived, String actor) {
        Fish f = entity(id);
        checkVersion(f, version);
        if (!archived) validateWrite(toWrite(f), f);
        f.setArchived(archived);
        f.touch();
        fish.flush();
        events.record(actor, archived ? "ARCHIVE" : "RESTORE", "FISH", id, f.getCode());
        return FishResponse.from(f);
    }

    @Transactional
    public void delete(UUID id, long version, String actor) {
        if (!permanentDeleteEnabled) throw new FishFailure(403, "Окончательное удаление отключено");
        Fish f = entity(id);
        checkVersion(f, version);
        if (!f.isArchived()) throw new FishFailure(409, "Удалять окончательно можно только архивную группу");
        fish.delete(f);
        fish.flush();
        events.record(actor, "DELETE", "FISH", id, "Удалена группа " + f.getCode());
    }

    /** Called in an independent transaction for each bulk item. */
    @Transactional
    public FishResponse bulkItem(BulkItem item, BulkRequest request, String actor) {
        Fish f = entity(item.id());
        checkVersion(f, item.version());
        switch (request.action()) {
            case ARCHIVE -> f.setArchived(true);
            case RESTORE -> { validateWrite(toWrite(f), f); f.setArchived(false); }
            case SET_STATUS -> {
                editable(f); f.setStatus(request.status());
            }
            case ADD_TAG -> {
                editable(f);
                String tag = FishSpecifications.normalizeTag(request.tag());
                if (!f.getTags().contains(tag) && f.getTags().size() >= 20)
                    throw new FishFailure(400, "Не более 20 тегов на группу");
                f.getTags().add(tag);
            }
            case REMOVE_TAG -> { editable(f); f.getTags().remove(FishSpecifications.normalizeTag(request.tag())); }
        }
        f.touch();
        fish.flush();
        events.record(actor, request.action().name(), "FISH", f.getId(), f.getCode());
        return FishResponse.from(f);
    }

    @Transactional(noRollbackFor = {FishFailure.class, EntityNotFoundException.class})
    public void validateWrite(FishWrite data, Fish existing) {
        validate(data);
        String code = code(data.code());
        fish.findByCode(code).filter(f -> existing == null || !f.getId().equals(existing.getId()))
            .ifPresent(f -> { throw new FishFailure(409, "Код группы уже занят"); });
        var aquarium = aquariums.lockById(data.aquariumId())
            .orElseThrow(() -> new EntityNotFoundException("Аквариум не найден"));
        if (aquarium.getType() == AquariumType.REPTILE)
            throw new FishFailure(400, "Рыб нельзя размещать в аквариуме для рептилий");
        if (!aquarium.isActive() || aquarium.isArchived())
            throw new FishFailure(400, "Аквариум должен быть активным и неархивным");
        FishSpecies s = species.findById(data.speciesId())
            .orElseThrow(() -> new EntityNotFoundException("Вид рыб не найден"));
        entityManager.lock(s, LockModeType.PESSIMISTIC_READ);
        // Disabling a reference must not prevent editing existing groups of this species.
        if (!s.isEnabled() && (existing == null || !s.getId().equals(existing.getSpecies().getId()) || existing.isArchived()))
            throw new FishFailure(400, "Вид рыб отключен");
        Set<String> tags = normalizedTags(data.tags());
        if (tags.stream().anyMatch(t -> t.contains("|") || t.chars().anyMatch(Character::isISOControl)))
            throw new FishFailure(400, "Тег не может содержать | или управляющие символы");
    }

    public Fish entity(UUID id) {
        return fish.findById(id).orElseThrow(() -> new EntityNotFoundException("Группа рыб не найдена"));
    }

    public static void checkVersion(TrackedEntity entity, long version) {
        if (entity.getVersion() != version) throw new OptimisticLockException("Запись уже изменена другим пользователем");
    }

    public static String code(String value) { return value.toUpperCase(Locale.ROOT); }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.SUPPORTS,
        noRollbackFor = FishFailure.class)
    public <T> void validate(T value) {
        var errors = validator.validate(value);
        if (!errors.isEmpty()) throw new FishFailure(400, errors.stream()
            .map(e -> e.getPropertyPath() + ": " + e.getMessage()).sorted().findFirst().orElseThrow());
    }

    public static Pageable pageable(int page, int size, String sort, String direction) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page >= 0; size от 1 до 100");
        if (!SORT_FIELDS.contains(sort)) throw new IllegalArgumentException("Недопустимое поле сортировки");
        if (!Set.of("asc", "desc").contains(direction.toLowerCase(Locale.ROOT)))
            throw new IllegalArgumentException("direction: asc или desc");
        return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sort).and(Sort.by("id")));
    }

    public static FishWrite toWrite(Fish f) {
        return new FishWrite(f.getCode(), f.getAquarium().getId(), f.getSpecies().getId(), f.getName(),
            f.getDescription(), f.getQuantity(), f.getStatus(), f.isRequiresObservation(), new TreeSet<>(f.getTags()));
    }

    private static Set<String> normalizedTags(Set<String> tags) {
        Set<String> result = new TreeSet<>();
        tags.forEach(t -> result.add(FishSpecifications.normalizeTag(t)));
        return result;
    }

    private void apply(Fish f, FishWrite d) {
        f.setCode(code(d.code()));
        f.setAquarium(entityManager.getReference(ru.mirea.aquarium.backend.team.Maslov.domain.Aquarium.class, d.aquariumId()));
        f.setSpecies(entityManager.getReference(FishSpecies.class, d.speciesId()));
        f.setName(d.name().strip());
        f.setDescription(d.description().strip());
        f.setQuantity(d.quantity());
        f.setStatus(d.status());
        f.setRequiresObservation(d.requiresObservation());
        f.getTags().clear();
        f.getTags().addAll(normalizedTags(d.tags()));
    }

    private static void editable(Fish f) {
        if (f.isArchived()) throw new FishFailure(409, "Сначала восстановите группу из архива");
    }
}
