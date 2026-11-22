package ru.mirea.aquarium.backend.team.Skvorcsov.service;

import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.aquarium.backend.team.Skvorcsov.domain.Fish;
import ru.mirea.aquarium.backend.team.Skvorcsov.dto.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.dto.FishDtos.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.repository.FishSpecifications;
import java.util.List;

@Service
public class FishSummaryService {
    private final EntityManager em;
    private final FishService service;
    public FishSummaryService(EntityManager em, FishService service) { this.em = em; this.service = service; }

    @Transactional(readOnly = true)
    public Summary summary(FishFilter supplied) {
        // The summary includes both archive states unless explicitly filtered.
        FishFilter f = supplied.archive() == null
            ? new FishFilter(supplied.q(), supplied.aquariumId(), supplied.speciesId(), supplied.status(),
                supplied.requiresObservation(), supplied.carePriority(), supplied.tag(),
                FishFilter.ArchiveMode.ALL, supplied.quantityOp(), supplied.quantity())
            : supplied;
        service.validate(f);
        f.validate();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Fish> r = query.from(Fish.class);
        query.multiselect(cb.count(r), sumBoolean(cb, r.get("archived"), false),
            sumBoolean(cb, r.get("archived"), true), sumBoolean(cb, r.get("requiresObservation"), true),
            cb.sumAsLong(r.get("quantity")), cb.avg(r.get("quantity")));
        query.where(FishSpecifications.matching(f).toPredicate(r, query, cb));
        Tuple totals = em.createQuery(query).getSingleResult();
        return new Summary(number(totals, 0).longValue(), number(totals, 1).longValue(),
            number(totals, 2).longValue(), number(totals, 3).longValue(), number(totals, 4).longValue(),
            number(totals, 5).doubleValue(), grouped(f, "species"), grouped(f, "status"), grouped(f, "priority"));
    }

    private Expression<Long> sumBoolean(CriteriaBuilder cb, Path<Boolean> field, boolean value) {
        return cb.sum(cb.<Long>selectCase().when(cb.equal(field, value), 1L).otherwise(0L));
    }

    private List<GroupCount> grouped(FishFilter f, String field) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Fish> r = query.from(Fish.class);
        Expression<?> key;
        Expression<String> name;
        if (field.equals("species")) {
            key = r.get("species").get("id"); name = r.get("species").get("name");
        } else if (field.equals("status")) {
            key = r.get("status"); name = r.get("status").as(String.class);
        } else {
            key = FishSpecifications.priority(r, cb); name = FishSpecifications.priority(r, cb);
        }
        query.multiselect(key, name, cb.count(r), cb.sumAsLong(r.get("quantity")))
            .where(FishSpecifications.matching(f).toPredicate(r, query, cb))
            .groupBy(key, name);
        return em.createQuery(query).getResultList().stream()
            .map(t -> new GroupCount(t.get(0).toString(), t.get(1).toString(),
                number(t, 2).longValue(), number(t, 3).longValue()))
            .sorted(java.util.Comparator.comparing(GroupCount::key)).toList();
    }

    private static Number number(Tuple t, int position) {
        return t.get(position) == null ? 0L : (Number) t.get(position);
    }
}
