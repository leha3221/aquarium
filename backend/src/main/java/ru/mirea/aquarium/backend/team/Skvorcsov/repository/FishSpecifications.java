package ru.mirea.aquarium.backend.team.Skvorcsov.repository;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.aquarium.backend.team.Skvorcsov.domain.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.dto.FishFilter;
import java.util.*;

public final class FishSpecifications {
    private FishSpecifications() {}

    public static Specification<Fish> matching(FishFilter f) {
        f.validate();
        return (r, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (f.q() != null && !f.q().isBlank()) {
                String pattern = "%" + escape(f.q().strip().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(r.get("code")), pattern, '\\'),
                    cb.like(cb.lower(r.get("name")), pattern, '\\'),
                    cb.like(cb.lower(r.get("description")), pattern, '\\'),
                    cb.like(cb.lower(r.get("species").get("name")), pattern, '\\')));
            }
            if (f.aquariumId() != null) predicates.add(cb.equal(r.get("aquarium").get("id"), f.aquariumId()));
            if (f.speciesId() != null) predicates.add(cb.equal(r.get("species").get("id"), f.speciesId()));
            if (f.status() != null) predicates.add(cb.equal(r.get("status"), f.status()));
            if (f.requiresObservation() != null)
                predicates.add(cb.equal(r.get("requiresObservation"), f.requiresObservation()));
            if (f.archiveMode() != FishFilter.ArchiveMode.ALL)
                predicates.add(cb.equal(r.get("archived"), f.archiveMode() == FishFilter.ArchiveMode.ARCHIVED));
            if (f.tag() != null && !f.tag().isBlank()) {
                // EXISTS avoids duplicate groups and incorrect page totals with several tags.
                Subquery<Integer> tags = query.subquery(Integer.class);
                Root<Fish> correlated = tags.correlate(r);
                SetJoin<Fish, String> tag = correlated.joinSet("tags");
                tags.select(cb.literal(1)).where(cb.equal(tag, normalizeTag(f.tag())));
                predicates.add(cb.exists(tags));
            }
            if (f.quantityOp() != null) {
                Path<Integer> quantity = r.get("quantity");
                predicates.add(switch (f.quantityOp()) {
                    case EQ -> cb.equal(quantity, f.quantity());
                    case GT -> cb.greaterThan(quantity, f.quantity());
                    case GTE -> cb.greaterThanOrEqualTo(quantity, f.quantity());
                    case LT -> cb.lessThan(quantity, f.quantity());
                    case LTE -> cb.lessThanOrEqualTo(quantity, f.quantity());
                });
            }
            if (f.carePriority() != null)
                predicates.add(cb.equal(priority(r, cb), f.carePriority().name()));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** SQL CASE mirrors CarePriority.calculate; integration tests cover the truth table. */
    public static Expression<String> priority(Root<Fish> r, CriteriaBuilder cb) {
        return cb.<String>selectCase()
            .when(cb.equal(r.get("status"), FishStatus.TREATMENT), "HIGH")
            .when(cb.or(r.get("status").in(FishStatus.ADAPTING, FishStatus.QUARANTINE),
                cb.isTrue(r.get("requiresObservation"))), "MEDIUM")
            .otherwise("LOW");
    }

    public static String normalizeTag(String value) { return value.strip().toLowerCase(Locale.ROOT); }
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
