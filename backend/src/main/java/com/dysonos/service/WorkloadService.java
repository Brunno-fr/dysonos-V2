package com.dysonos.service;

import com.dysonos.dto.DistributionPlanRequest;
import com.dysonos.dto.DistributionPlanResponse;
import com.dysonos.dto.WorkloadRequest;
import com.dysonos.dto.WorkloadResponse;
import com.dysonos.exception.InsufficientZoneCapacityException;
import com.dysonos.exception.NodeNotFoundException;
import com.dysonos.exception.WorkloadNotFoundException;
import com.dysonos.exception.ZoneNotFoundException;
import com.dysonos.model.Node;
import com.dysonos.model.NodeStatus;
import com.dysonos.model.Workload;
import com.dysonos.model.WorkloadAllocation;
import com.dysonos.model.WorkloadStatus;
import com.dysonos.model.Zone;
import com.dysonos.repository.NodeRepository;
import com.dysonos.repository.WorkloadRepository;
import com.dysonos.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WorkloadService {

    private final ZoneRepository zoneRepository;
    private final NodeRepository nodeRepository;
    private final WorkloadRepository workloadRepository;
    private final ZoneDistributionService zoneDistributionService;

    public WorkloadService(ZoneRepository zoneRepository, NodeRepository nodeRepository,
                           WorkloadRepository workloadRepository,
                           ZoneDistributionService zoneDistributionService) {
        this.zoneRepository = zoneRepository;
        this.nodeRepository = nodeRepository;
        this.workloadRepository = workloadRepository;
        this.zoneDistributionService = zoneDistributionService;
    }

    @Transactional
    public WorkloadResponse create(UUID zoneId, WorkloadRequest request) {
        Zone zone = zoneRepository.findById(zoneId).orElseThrow(() -> new ZoneNotFoundException(zoneId));
        nodeRepository.findAllByZoneIdAndStatusOrderByIdForUpdate(zoneId, NodeStatus.ONLINE);

        DistributionPlanResponse plan = zoneDistributionService.plan(zoneId,
                new DistributionPlanRequest(request.cpuDemand(), request.memoryDemand()));
        if (!plan.feasible()) {
            throw new InsufficientZoneCapacityException(zoneId);
        }

        List<UUID> nodeIds = plan.allocations().stream()
                .map(DistributionPlanResponse.NodeAllocation::nodeId)
                .toList();
        Map<UUID, Node> nodesById = nodeRepository.findAllById(nodeIds).stream()
                .collect(Collectors.toMap(Node::getId, Function.identity()));

        Workload workload = new Workload(request.name().trim(), request.cpuDemand(), request.memoryDemand(), zone);
        for (DistributionPlanResponse.NodeAllocation allocation : plan.allocations()) {
            Node node = nodesById.get(allocation.nodeId());
            if (node == null) {
                throw new NodeNotFoundException(allocation.nodeId());
            }
            workload.addAllocation(new WorkloadAllocation(workload, node,
                    allocation.allocatedCpu(), allocation.allocatedMemory()));
        }
        return WorkloadResponse.from(workloadRepository.save(workload));
    }

    public List<WorkloadResponse> findByZone(UUID zoneId) {
        if (!zoneRepository.existsById(zoneId)) {
            throw new ZoneNotFoundException(zoneId);
        }
        return workloadRepository.findAllByZoneIdOrderByCreatedAtDesc(zoneId).stream()
                .map(WorkloadResponse::from)
                .toList();
    }

    @Transactional
    public WorkloadResponse stop(UUID workloadId) {
        Workload workload = workloadRepository.findById(workloadId)
                .orElseThrow(() -> new WorkloadNotFoundException(workloadId));
        if (workload.getStatus() == WorkloadStatus.ACTIVE) {
            workload.stop();
            workloadRepository.save(workload);
        }
        return WorkloadResponse.from(workload);
    }
}
