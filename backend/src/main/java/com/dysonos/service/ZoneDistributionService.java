package com.dysonos.service;

import com.dysonos.configuration.NodeUsageThresholdProperties;
import com.dysonos.dto.DistributionPlanRequest;
import com.dysonos.dto.DistributionPlanResponse;
import com.dysonos.model.Node;
import com.dysonos.model.NodeStatus;
import com.dysonos.repository.NodeRepository;
import com.dysonos.repository.ZoneRepository;
import com.dysonos.exception.ZoneNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ZoneDistributionService {

    private static final double RESERVED_HEADROOM = 0.01;
    private static final double FRACTION_EPSILON = 1.0e-9;

    private final ZoneRepository zoneRepository;
    private final NodeRepository nodeRepository;
    private final NodeUsageThresholdProperties usageThresholds;

    public ZoneDistributionService(ZoneRepository zoneRepository, NodeRepository nodeRepository,
                                   NodeUsageThresholdProperties usageThresholds) {
        this.zoneRepository = zoneRepository;
        this.nodeRepository = nodeRepository;
        this.usageThresholds = usageThresholds;
    }

    public DistributionPlanResponse plan(UUID zoneId, DistributionPlanRequest request) {
        if (!zoneRepository.existsById(zoneId)) {
            throw new ZoneNotFoundException(zoneId);
        }

        List<NodeCapacity> capacities = nodeRepository
                .findAllByZoneIdAndStatusOrderById(zoneId, NodeStatus.ONLINE).stream()
                .map(this::capacityOf)
                .toList();

        double availableCpu = capacities.stream().mapToDouble(NodeCapacity::availableCpu).sum();
        double availableMemory = capacities.stream().mapToDouble(NodeCapacity::availableMemory).sum();

        List<NodeCapacity> candidates = capacities.stream()
                .filter(capacity -> capacity.maxDemandFraction(request.cpuDemand(), request.memoryDemand()) > 0)
                .sorted(Comparator.comparingDouble(
                        (NodeCapacity capacity) -> capacity.maxDemandFraction(request.cpuDemand(), request.memoryDemand()))
                        .reversed()
                        .thenComparing(capacity -> capacity.node().getId()))
                .toList();

        double maxAllocatableFraction = candidates.stream()
                .mapToDouble(capacity -> capacity.maxDemandFraction(request.cpuDemand(), request.memoryDemand()))
                .sum();
        boolean feasible = request.cpuDemand() == 0.0 && request.memoryDemand() == 0.0
                || maxAllocatableFraction + FRACTION_EPSILON >= 1.0;

        List<DistributionPlanResponse.NodeAllocation> allocations = feasible
                ? buildAllocations(candidates, request)
                : List.of();

        return new DistributionPlanResponse(zoneId, feasible, request.cpuDemand(), request.memoryDemand(),
                availableCpu, availableMemory, allocations);
    }

    private NodeCapacity capacityOf(Node node) {
        double cpuLimit = Math.max(0, usageThresholds.getCpuOverloadThreshold() - RESERVED_HEADROOM);
        double memoryLimit = Math.max(0, usageThresholds.getMemoryOverloadThreshold() - RESERVED_HEADROOM);
        return new NodeCapacity(node,
                Math.max(0, cpuLimit - node.getCpuUsage()),
                Math.max(0, memoryLimit - node.getMemoryUsage()));
    }

    private List<DistributionPlanResponse.NodeAllocation> buildAllocations(List<NodeCapacity> candidates,
                                                                            DistributionPlanRequest request) {
        if (request.cpuDemand() == 0.0 && request.memoryDemand() == 0.0) {
            return List.of();
        }

        List<DistributionPlanResponse.NodeAllocation> allocations = new ArrayList<>();
        double remainingFraction = 1.0;
        for (NodeCapacity capacity : candidates) {
            if (remainingFraction <= FRACTION_EPSILON) {
                break;
            }
            double fraction = Math.min(remainingFraction,
                    capacity.maxDemandFraction(request.cpuDemand(), request.memoryDemand()));
            if (fraction <= 0) {
                continue;
            }

            double allocatedCpu = request.cpuDemand() * fraction;
            double allocatedMemory = request.memoryDemand() * fraction;
            Node node = capacity.node();
            allocations.add(new DistributionPlanResponse.NodeAllocation(node.getId(), node.getName(),
                    allocatedCpu, allocatedMemory,
                    node.getCpuUsage() + allocatedCpu,
                    node.getMemoryUsage() + allocatedMemory));
            remainingFraction -= fraction;
        }
        return List.copyOf(allocations);
    }

    private record NodeCapacity(Node node, double availableCpu, double availableMemory) {
        double maxDemandFraction(double cpuDemand, double memoryDemand) {
            double cpuFraction = cpuDemand == 0.0 ? Double.POSITIVE_INFINITY : availableCpu / cpuDemand;
            double memoryFraction = memoryDemand == 0.0 ? Double.POSITIVE_INFINITY : availableMemory / memoryDemand;
            return Math.min(1.0, Math.min(cpuFraction, memoryFraction));
        }
    }
}
