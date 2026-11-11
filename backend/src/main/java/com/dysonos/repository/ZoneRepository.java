package com.dysonos.repository;

import com.dysonos.model.Zone;
import com.dysonos.model.NodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ZoneRepository extends JpaRepository<Zone, UUID> {
    boolean existsByNetworkId(UUID networkId);

    @Query("""
            select z.id as zoneId,
                   z.name as name,
                   network.id as networkId,
                   count(node.id) as nodeCount,
                   count(case when node.status = :overloadedStatus then node.id else null end) as overloadedNodeCount,
                   avg(node.cpuUsage) as averageCpuUsage,
                   avg(node.memoryUsage) as averageMemoryUsage
            from Zone z
            left join z.network network
            left join z.nodes node
            where z.id = :zoneId
            group by z.id, z.name, network.id
            """)
    Optional<ZoneMetricsProjection> findMetricsById(@Param("zoneId") UUID zoneId,
                                                    @Param("overloadedStatus") NodeStatus overloadedStatus);
}
