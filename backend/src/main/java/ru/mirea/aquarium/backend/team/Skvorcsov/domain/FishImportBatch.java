package ru.mirea.aquarium.backend.team.Skvorcsov.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "fish_import_batches")
public class FishImportBatch extends TrackedEntity {
    @Column(nullable = false, length = 255)
    private String owner;
    @Column(nullable = false, columnDefinition = "text")
    private String plan;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private boolean consumed;

    public String getOwner() { return owner; }
    public void setOwner(String value) { owner = value; }
    public String getPlan() { return plan; }
    public void setPlan(String value) { plan = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { expiresAt = value; }
    public boolean isConsumed() { return consumed; }
    public void setConsumed(boolean value) { consumed = value; }
}
