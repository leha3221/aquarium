package ru.mirea.aquarium.backend.team.Skvorcsov.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

@Component
public class FishEvents {
    /** Common audit can subscribe using @TransactionalEventListener(AFTER_COMMIT). */
    public record Changed(String actor, String eventType, String entityType,
        UUID entityId, Instant occurredAt, String description) {}

    private final ApplicationEventPublisher publisher;
    public FishEvents(ApplicationEventPublisher publisher) { this.publisher = publisher; }

    public void record(String actor, String event, String entity, UUID id, String description) {
        publisher.publishEvent(new Changed(actor, event, entity, id, Instant.now(), description));
    }
}
