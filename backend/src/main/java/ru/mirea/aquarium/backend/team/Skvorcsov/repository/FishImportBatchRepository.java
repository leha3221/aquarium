package ru.mirea.aquarium.backend.team.Skvorcsov.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.domain.FishImportBatch;
import java.util.*;

public interface FishImportBatchRepository extends JpaRepository<FishImportBatch, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from FishImportBatch b where b.id = :id")
    Optional<FishImportBatch> lockById(UUID id);
}
