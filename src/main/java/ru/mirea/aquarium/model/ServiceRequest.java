package ru.mirea.aquarium.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ServiceRequest {
    private long id;
    private long clientId;
    private AquariumType aquariumType;
    private ServiceType serviceType;
    private String description;
    private RequestStatus status;
    private int priority;
    private LocalDateTime createdAt;
    private LocalDateTime scheduledAt;
    private BigDecimal price;
    private String clientName;

    public ServiceRequest(long id, long clientId, AquariumType aquariumType,
                          ServiceType serviceType, String description,
                          RequestStatus status, int priority,
                          LocalDateTime createdAt, LocalDateTime scheduledAt,
                          BigDecimal price, String clientName) {
        this.id = id;
        this.clientId = clientId;
        this.aquariumType = aquariumType;
        this.serviceType = serviceType;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.createdAt = createdAt;
        this.scheduledAt = scheduledAt;
        this.price = price;
        this.clientName = clientName;
    }

    public ServiceRequest(long clientId, AquariumType aquariumType,
                          ServiceType serviceType, String description,
                          RequestStatus status, int priority,
                          LocalDateTime createdAt, LocalDateTime scheduledAt,
                          BigDecimal price) {
        this(0, clientId, aquariumType, serviceType, description, status,
                priority, createdAt, scheduledAt, price, null);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getClientId() { return clientId; }
    public void setClientId(long clientId) { this.clientId = clientId; }
    public AquariumType getAquariumType() { return aquariumType; }
    public void setAquariumType(AquariumType aquariumType) { this.aquariumType = aquariumType; }
    public ServiceType getServiceType() { return serviceType; }
    public void setServiceType(ServiceType serviceType) { this.serviceType = serviceType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    @Override
    public String toString() {
        return String.format("#%d | Клиент: %s | Аквариум: %s | Услуга: %s | " +
                        "Статус: %s | Приоритет: %d | План: %s | Цена: %s",
                id, clientName == null ? clientId : clientName, aquariumType,
                serviceType, status, priority, scheduledAt, price);
    }
}
