package com.dysonos.repository;

import com.dysonos.model.Network;
import com.dysonos.model.NodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface NetworkRepository extends JpaRepository<Network, UUID> {

    @Query("""
            select n.id as networkId,
                   n.name as name,
                   count(distinct z.id) as zoneCount,
                   count(distinct node.id) as nodeCount,
                   count(case when node.status = :overloadedStatus then node.id else null end) as overloadedNodeCount,
                   avg(node.cpuUsage) as averageCpuUsage,
                   avg(node.memoryUsage) as averageMemoryUsage
            from Network n
            left join n.zones z
            left join z.nodes node
            where n.id = :networkId
            group by n.id, n.name
            """)
    Optional<NetworkMetricsProjection> findMetricsById(@Param("networkId") UUID networkId,
                                                       @Param("overloadedStatus") NodeStatus overloadedStatus);
}
