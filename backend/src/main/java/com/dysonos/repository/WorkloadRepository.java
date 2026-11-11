package com.dysonos.repository;

import com.dysonos.model.Workload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkloadRepository extends JpaRepository<Workload, UUID> {
    boolean existsByZoneId(UUID zoneId);

    List<Workload> findAllByZoneIdOrderByCreatedAtDesc(UUID zoneId);
}
