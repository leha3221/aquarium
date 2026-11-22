package ru.mirea.aquarium.backend.team.Skvorcsov.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import ru.mirea.aquarium.backend.team.Skvorcsov.dto.FishDtos.*;
import java.util.*;

@Service
public class FishBulkService {
    private final FishService service;
    private final TransactionTemplate transaction;

    public FishBulkService(FishService service, PlatformTransactionManager manager) {
        this.service = service;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public BulkResult apply(BulkRequest request, String actor) {
        service.validate(request);
        if (request.action() == BulkAction.SET_STATUS && request.status() == null)
            throw new FishFailure(400, "Для SET_STATUS обязателен status");
        if ((request.action() == BulkAction.ADD_TAG || request.action() == BulkAction.REMOVE_TAG)
            && (request.tag() == null || request.tag().isBlank() || request.tag().contains("|")
                || request.tag().chars().anyMatch(Character::isISOControl)))
            throw new FishFailure(400, "Укажите корректный тег");
        List<FishResponse> successes = new ArrayList<>();
        List<ItemError> errors = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (BulkItem item : request.items()) {
            if (!seen.add(item.id())) { errors.add(new ItemError(item.id(), 400, "Повтор UUID")); continue; }
            try {
                successes.add(transaction.execute(tx -> service.bulkItem(item, request, actor)));
            } catch (FishFailure e) {
                errors.add(new ItemError(item.id(), e.status(), e.getMessage()));
            } catch (EntityNotFoundException e) {
                errors.add(new ItemError(item.id(), 404, e.getMessage()));
            } catch (OptimisticLockException | OptimisticLockingFailureException e) {
                errors.add(new ItemError(item.id(), 409, "Запись уже изменена другим пользователем"));
            } catch (DataIntegrityViolationException | PessimisticLockingFailureException e) {
                errors.add(new ItemError(item.id(), 409, "Конфликт изменения данных"));
            }
        }
        return new BulkResult(successes, errors);
    }
}
