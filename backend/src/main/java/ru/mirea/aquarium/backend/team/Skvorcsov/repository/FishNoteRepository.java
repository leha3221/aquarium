package ru.mirea.aquarium.backend.team.Skvorcsov.repository;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.aquarium.backend.team.Skvorcsov.domain.FishNote;
import java.util.*;

public interface FishNoteRepository extends JpaRepository<FishNote, UUID> {
    Page<FishNote> findByFishId(UUID fishId, Pageable pageable);
    Optional<FishNote> findByIdAndFishId(UUID id, UUID fishId);
}
