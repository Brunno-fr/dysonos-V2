package com.dysonos.repository;

import com.dysonos.model.WorkloadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface WorkloadAllocationRepository extends JpaRepository<com.dysonos.model.WorkloadAllocation, UUID> {

    boolean existsByNodeId(UUID nodeId);

    @Query("""
            select allocation.node.id as nodeId,
                   sum(allocation.cpuAllocated) as cpuAllocated,
                   sum(allocation.memoryAllocated) as memoryAllocated
            from WorkloadAllocation allocation
            where allocation.node.id in :nodeIds
              and allocation.workload.status = :status
            group by allocation.node.id
            """)
    List<ActiveNodeAllocationProjection> sumAllocationsByNodeIdsAndWorkloadStatus(
            @Param("nodeIds") Collection<UUID> nodeIds,
            @Param("status") WorkloadStatus status);
}
