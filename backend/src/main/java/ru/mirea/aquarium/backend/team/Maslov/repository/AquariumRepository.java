package ru.mirea.aquarium.backend.team.Maslov.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.mirea.aquarium.backend.team.Maslov.domain.Aquarium;
import java.util.UUID;

public interface AquariumRepository extends JpaRepository<Aquarium, UUID>, JpaSpecificationExecutor<Aquarium> {
    boolean existsByCode(String code);
}
