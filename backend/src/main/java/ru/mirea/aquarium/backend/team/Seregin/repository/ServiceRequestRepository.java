package ru.mirea.aquarium.backend.team.Seregin.repository;
import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.data.jpa.repository.JpaSpecificationExecutor;import ru.mirea.aquarium.backend.team.Seregin.domain.ServiceRequest;import java.util.UUID;
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest,UUID>,JpaSpecificationExecutor<ServiceRequest>{ }
