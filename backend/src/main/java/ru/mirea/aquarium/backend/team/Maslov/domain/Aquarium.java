package ru.mirea.aquarium.backend.team.Maslov.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "aquariums", indexes = {
        @Index(name = "ix_aquarium_client", columnList = "client_id"),
        @Index(name = "ix_aquarium_archived_updated", columnList = "archived, updated_at")
})
public class Aquarium {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "external_code", nullable = false, unique = true, length = 32)
    private String code;
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    @Column(nullable = false, length = 120)
    private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    private AquariumType type;
    @Column(name = "volume_liters", nullable = false, precision = 10, scale = 2)
    private BigDecimal volumeLiters;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private boolean archived = false;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version @Column(nullable = false)
    private long version;

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public AquariumType getType() { return type; }
    public void setType(AquariumType type) { this.type = type; }
    public BigDecimal getVolumeLiters() { return volumeLiters; }
    public void setVolumeLiters(BigDecimal volumeLiters) { this.volumeLiters = volumeLiters; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
