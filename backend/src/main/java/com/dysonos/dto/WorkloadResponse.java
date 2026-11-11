package com.dysonos.dto;

import com.dysonos.model.Workload;
import com.dysonos.model.WorkloadStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkloadResponse(
        UUID id,
        String name,
        UUID zoneId,
        double cpuDemand,
        double memoryDemand,
        WorkloadStatus status,
        Instant createdAt,
        List<Allocation> allocations
) {
    public WorkloadResponse {
        allocations = List.copyOf(allocations);
    }

    public static WorkloadResponse from(Workload workload) {
        List<Allocation> allocations = workload.getAllocations().stream()
                .map(allocation -> new Allocation(allocation.getNode().getId(), allocation.getNode().getName(),
                        allocation.getCpuAllocated(), allocation.getMemoryAllocated()))
                .toList();
        return new WorkloadResponse(workload.getId(), workload.getName(), workload.getZone().getId(),
                workload.getCpuDemand(), workload.getMemoryDemand(), workload.getStatus(),
                workload.getCreatedAt(), allocations);
    }

    public record Allocation(UUID nodeId, String nodeName, double cpuAllocated, double memoryAllocated) {
    }
}
