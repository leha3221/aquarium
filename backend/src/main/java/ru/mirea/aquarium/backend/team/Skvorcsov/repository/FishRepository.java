package ru.mirea.aquarium.backend.team.Skvorcsov.repository;

import org.springframework.data.jpa.repository.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.domain.Fish;
import java.util.*;

public interface FishRepository extends JpaRepository<Fish, UUID>, JpaSpecificationExecutor<Fish> {
    Optional<Fish> findByCode(String code);
}
