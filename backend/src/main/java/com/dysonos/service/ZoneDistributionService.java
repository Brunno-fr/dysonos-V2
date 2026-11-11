package com.dysonos.service;

import com.dysonos.configuration.NodeUsageThresholdProperties;
import com.dysonos.dto.DistributionPlanRequest;
import com.dysonos.dto.DistributionPlanResponse;
import com.dysonos.model.Node;
import com.dysonos.model.NodeStatus;
import com.dysonos.model.WorkloadStatus;
import com.dysonos.repository.ActiveNodeAllocationProjection;
import com.dysonos.repository.NodeRepository;
import com.dysonos.repository.WorkloadAllocationRepository;
import com.dysonos.repository.ZoneRepository;
import com.dysonos.exception.ZoneNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ZoneDistributionService {

    private static final double RESERVED_HEADROOM = 0.01;
    private static final double FRACTION_EPSILON = 1.0e-9;

    private final ZoneRepository zoneRepository;
    private final NodeRepository nodeRepository;
    private final WorkloadAllocationRepository workloadAllocationRepository;
    private final NodeUsageThresholdProperties usageThresholds;

    public ZoneDistributionService(ZoneRepository zoneRepository, NodeRepository nodeRepository,
                                   WorkloadAllocationRepository workloadAllocationRepository,
                                   NodeUsageThresholdProperties usageThresholds) {
        this.zoneRepository = zoneRepository;
        this.nodeRepository = nodeRepository;
        this.workloadAllocationRepository = workloadAllocationRepository;
        this.usageThresholds = usageThresholds;
    }

    public DistributionPlanResponse plan(UUID zoneId, DistributionPlanRequest request) {
        if (!zoneRepository.existsById(zoneId)) {
            throw new ZoneNotFoundException(zoneId);
        }

        List<Node> onlineNodes = nodeRepository.findAllByZoneIdAndStatusOrderById(zoneId, NodeStatus.ONLINE);
        Map<UUID, ActiveNodeAllocationProjection> reservations = onlineNodes.isEmpty() ? Map.of()
                : workloadAllocationRepository.sumAllocationsByNodeIdsAndWorkloadStatus(
                        onlineNodes.stream().map(Node::getId).toList(), WorkloadStatus.ACTIVE).stream()
                .collect(Collectors.toMap(ActiveNodeAllocationProjection::getNodeId, Function.identity()));
        List<NodeCapacity> capacities = onlineNodes.stream()
                .map(node -> capacityOf(node, reservations.get(node.getId())))
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

    private NodeCapacity capacityOf(Node node, ActiveNodeAllocationProjection reservations) {
        double cpuLimit = Math.max(0, usageThresholds.getCpuOverloadThreshold() - RESERVED_HEADROOM);
        double memoryLimit = Math.max(0, usageThresholds.getMemoryOverloadThreshold() - RESERVED_HEADROOM);
        double reservedCpu = reservations == null || reservations.getCpuAllocated() == null
                ? 0.0 : reservations.getCpuAllocated();
        double reservedMemory = reservations == null || reservations.getMemoryAllocated() == null
                ? 0.0 : reservations.getMemoryAllocated();
        return new NodeCapacity(node,
                reservedCpu,
                reservedMemory,
                Math.max(0, cpuLimit - node.getCpuUsage() - reservedCpu),
                Math.max(0, memoryLimit - node.getMemoryUsage() - reservedMemory));
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
                    node.getCpuUsage() + capacity.reservedCpu() + allocatedCpu,
                    node.getMemoryUsage() + capacity.reservedMemory() + allocatedMemory));
            remainingFraction -= fraction;
        }
        return List.copyOf(allocations);
    }

    private record NodeCapacity(Node node, double reservedCpu, double reservedMemory,
                                double availableCpu, double availableMemory) {
        double maxDemandFraction(double cpuDemand, double memoryDemand) {
            double cpuFraction = cpuDemand == 0.0 ? Double.POSITIVE_INFINITY : availableCpu / cpuDemand;
            double memoryFraction = memoryDemand == 0.0 ? Double.POSITIVE_INFINITY : availableMemory / memoryDemand;
            return Math.min(1.0, Math.min(cpuFraction, memoryFraction));
        }
    }
}
