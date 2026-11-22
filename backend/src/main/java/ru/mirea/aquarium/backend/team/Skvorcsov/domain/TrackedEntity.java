package ru.mirea.aquarium.backend.team.Skvorcsov.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class TrackedEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Version @Column(nullable = false)
    private long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void created() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate
    void updated() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void touch() { updatedAt = Instant.now(); }
}
