package com.dysonos.repository;

import com.dysonos.model.Node;
import com.dysonos.model.NodeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NodeRepository extends JpaRepository<Node, UUID> {
    boolean existsByZoneId(UUID zoneId);

    List<Node> findAllByZoneIdAndStatusOrderById(UUID zoneId, NodeStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select node from Node node where node.zone.id = :zoneId and node.status = :status order by node.id")
    List<Node> findAllByZoneIdAndStatusOrderByIdForUpdate(@Param("zoneId") UUID zoneId,
                                                          @Param("status") NodeStatus status);
}
