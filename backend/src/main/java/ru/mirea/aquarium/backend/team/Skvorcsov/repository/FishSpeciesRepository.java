package ru.mirea.aquarium.backend.team.Skvorcsov.repository;

import org.springframework.data.jpa.repository.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.domain.FishSpecies;
import java.util.*;

public interface FishSpeciesRepository extends JpaRepository<FishSpecies, UUID>, JpaSpecificationExecutor<FishSpecies> {
    Optional<FishSpecies> findByCode(String code);
}
