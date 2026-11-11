package com.dysonos.dto;

import java.util.List;
import java.util.UUID;

public record DistributionPlanResponse(
        UUID zoneId,
        boolean feasible,
        double requestedCpu,
        double requestedMemory,
        double availableCpu,
        double availableMemory,
        List<NodeAllocation> allocations
) {
    public DistributionPlanResponse {
        allocations = List.copyOf(allocations);
    }

    public record NodeAllocation(
            UUID nodeId,
            String nodeName,
            double allocatedCpu,
            double allocatedMemory,
            double projectedCpuUsage,
            double projectedMemoryUsage
    ) {
    }
}
