package ru.mirea.aquarium.backend.team.Skvorcsov.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.Repository;
import ru.mirea.aquarium.backend.team.Maslov.domain.Aquarium;
import java.util.*;

public interface FishAquariumRepository extends Repository<Aquarium, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Aquarium a where a.id = :id")
    Optional<Aquarium> lockById(UUID id);
    Optional<Aquarium> findByCode(String code);
}
