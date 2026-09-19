package ru.mirea.aquarium.service;

import ru.mirea.aquarium.exception.BusinessException;
import ru.mirea.aquarium.exception.EntityNotFoundException;
import ru.mirea.aquarium.model.*;
import ru.mirea.aquarium.repository.ClientRepository;
import ru.mirea.aquarium.repository.ServiceRequestRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class ServiceRequestService {
    private final ServiceRequestRepository repository;
    private final ClientRepository clientRepository;

    public ServiceRequestService(ServiceRequestRepository repository, ClientRepository clientRepository) {
        this.repository = repository;
        this.clientRepository = clientRepository;
    }

    public ServiceRequest create(ServiceRequest r) {
        validate(r);
        return repository.create(r);
    }

    public List<ServiceRequest> findAll() { return repository.findAll(); }

    public ServiceRequest get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Заявка с ID " + id + " не найдена."));
    }

    public void update(ServiceRequest r) {
        ServiceRequest old = get(r.getId());
        validate(r);
        validateStatusTransition(old.getStatus(), r.getStatus());
        repository.update(r);
    }

    public void delete(long id) {
        ServiceRequest r = get(id);
        if (r.getStatus() == RequestStatus.IN_PROGRESS)
            throw new BusinessException("Нельзя удалить заявку, которая уже выполняется.");
        repository.delete(id);
    }

    public List<ServiceRequest> searchByDescription(String text) {
        String q = text.toLowerCase();
        return findAll().stream()
                .filter(r -> r.getDescription().toLowerCase().contains(q))
                .toList();
    }

    public List<ServiceRequest> searchByClient(String text) {
        String q = text.toLowerCase();
        return findAll().stream()
                .filter(r -> r.getClientName().toLowerCase().contains(q))
                .toList();
    }

    public List<ServiceRequest> filterByStatus(RequestStatus status) {
        return findAll().stream().filter(r -> r.getStatus() == status).toList();
    }

    public List<ServiceRequest> filterByPriority(int minPriority) {
        return findAll().stream().filter(r -> r.getPriority() >= minPriority).toList();
    }

    public List<ServiceRequest> sortByDate(boolean ascending) {
        Comparator<ServiceRequest> cmp = Comparator.comparing(ServiceRequest::getScheduledAt);
        if (!ascending) cmp = cmp.reversed();
        return findAll().stream().sorted(cmp).toList();
    }

    public List<ServiceRequest> sortByPrice(boolean ascending) {
        Comparator<ServiceRequest> cmp = Comparator.comparing(ServiceRequest::getPrice);
        if (!ascending) cmp = cmp.reversed();
        return findAll().stream().sorted(cmp).toList();
    }

    public long countStatus(RequestStatus status) {
        return findAll().stream().filter(r -> r.getStatus() == status).count();
    }

    public long countHighPriority() {
        return findAll().stream().filter(r -> r.getPriority() >= 4).count();
    }

    public BigDecimal totalRevenueCompleted() {
        return findAll().stream()
                .filter(r -> r.getStatus() == RequestStatus.COMPLETED)
                .map(ServiceRequest::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validate(ServiceRequest r) {
        if (clientRepository.findById(r.getClientId()).isEmpty())
            throw new BusinessException("Указанный клиент не существует.");
        if (r.getAquariumType() == null || r.getServiceType() == null)
            throw new BusinessException("Тип аквариума и тип услуги обязательны.");
        if (r.getDescription() == null || r.getDescription().isBlank())
            throw new BusinessException("Описание заявки обязательно.");
        if (r.getStatus() == null)
            throw new BusinessException("Статус обязателен.");
        if (r.getPriority() < 1 || r.getPriority() > 5)
            throw new BusinessException("Приоритет должен быть от 1 до 5.");
        if (r.getPrice() == null || r.getPrice().signum() < 0)
            throw new BusinessException("Стоимость не может быть отрицательной.");
        if (r.getScheduledAt() == null)
            throw new BusinessException("Дата обслуживания обязательна.");
        if (r.getCreatedAt() == null)
            r.setCreatedAt(LocalDateTime.now());
    }

    private void validateStatusTransition(RequestStatus oldStatus, RequestStatus newStatus) {
        if (oldStatus == newStatus) return;
        boolean allowed = switch (oldStatus) {
            case NEW -> newStatus == RequestStatus.CONFIRMED || newStatus == RequestStatus.CANCELLED;
            case CONFIRMED -> newStatus == RequestStatus.IN_PROGRESS || newStatus == RequestStatus.CANCELLED;
            case IN_PROGRESS -> newStatus == RequestStatus.COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!allowed)
            throw new BusinessException("Запрещён переход статуса: " + oldStatus + " -> " + newStatus);
    }
}
