package com.dysonos.repository;

import com.dysonos.model.Node;
import com.dysonos.model.NodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NodeRepository extends JpaRepository<Node, UUID> {
    boolean existsByZoneId(UUID zoneId);

    List<Node> findAllByZoneIdAndStatusOrderById(UUID zoneId, NodeStatus status);
}
