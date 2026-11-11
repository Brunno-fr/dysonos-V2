package com.dysonos.service;

import com.dysonos.dto.DistributionPlanResponse;
import com.dysonos.dto.WorkloadRequest;
import com.dysonos.dto.WorkloadResponse;
import com.dysonos.exception.InsufficientZoneCapacityException;
import com.dysonos.model.Node;
import com.dysonos.model.NodeStatus;
import com.dysonos.model.Workload;
import com.dysonos.model.WorkloadStatus;
import com.dysonos.model.Zone;
import com.dysonos.repository.NodeRepository;
import com.dysonos.repository.WorkloadRepository;
import com.dysonos.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkloadServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private WorkloadRepository workloadRepository;

    @Mock
    private ZoneDistributionService zoneDistributionService;

    private WorkloadService service;

    @BeforeEach
    void setUp() {
        service = new WorkloadService(zoneRepository, nodeRepository, workloadRepository, zoneDistributionService);
    }

    @Test
    void createPersistsDemandAndItsNodeAllocation() {
        UUID zoneId = UUID.randomUUID();
        UUID nodeId = UUID.randomUUID();
        Zone zone = org.mockito.Mockito.mock(Zone.class);
        Node node = org.mockito.Mockito.mock(Node.class);
        when(zoneRepository.findById(zoneId)).thenReturn(Optional.of(zone));
        when(zone.getId()).thenReturn(zoneId);
        when(nodeRepository.findAllByZoneIdAndStatusOrderByIdForUpdate(zoneId, NodeStatus.ONLINE))
                .thenReturn(List.of(node));
        when(zoneDistributionService.plan(zoneId, new com.dysonos.dto.DistributionPlanRequest(30.0, 20.0)))
                .thenReturn(new DistributionPlanResponse(zoneId, true, 30, 20, 50, 50,
                        List.of(new DistributionPlanResponse.NodeAllocation(nodeId, "edge-01", 30, 20, 55, 60))));
        when(nodeRepository.findAllById(List.of(nodeId))).thenReturn(List.of(node));
        when(node.getId()).thenReturn(nodeId);
        when(node.getName()).thenReturn("edge-01");
        when(workloadRepository.save(any(Workload.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkloadResponse response = service.create(zoneId, new WorkloadRequest(" api-service ", 30.0, 20.0));

        assertEquals("api-service", response.name());
        assertEquals(zoneId, response.zoneId());
        assertEquals(WorkloadStatus.ACTIVE, response.status());
        assertEquals(1, response.allocations().size());
        assertEquals(nodeId, response.allocations().getFirst().nodeId());
        assertEquals(30.0, response.allocations().getFirst().cpuAllocated());
        assertEquals(20.0, response.allocations().getFirst().memoryAllocated());

        ArgumentCaptor<Workload> workload = ArgumentCaptor.forClass(Workload.class);
        verify(workloadRepository).save(workload.capture());
        assertEquals(1, workload.getValue().getAllocations().size());
    }

    @Test
    void createDoesNotPersistWhenZoneCannotFitDemand() {
        UUID zoneId = UUID.randomUUID();
        when(zoneRepository.findById(zoneId)).thenReturn(Optional.of(new Zone("south-dc")));
        when(nodeRepository.findAllByZoneIdAndStatusOrderByIdForUpdate(zoneId, NodeStatus.ONLINE))
                .thenReturn(List.of());
        when(zoneDistributionService.plan(zoneId, new com.dysonos.dto.DistributionPlanRequest(100.0, 100.0)))
                .thenReturn(new DistributionPlanResponse(zoneId, false, 100, 100, 20, 20, List.of()));

        assertThrows(InsufficientZoneCapacityException.class,
                () -> service.create(zoneId, new WorkloadRequest("large", 100.0, 100.0)));

        verify(workloadRepository, never()).save(any(Workload.class));
    }

    @Test
    void stopMarksWorkloadAsStoppedAndKeepsItsHistory() {
        UUID workloadId = UUID.randomUUID();
        Zone zone = new Zone("south-dc");
        Workload workload = new Workload("api-service", 30, 20, zone);
        when(workloadRepository.findById(workloadId)).thenReturn(Optional.of(workload));
        when(workloadRepository.save(workload)).thenReturn(workload);

        WorkloadResponse response = service.stop(workloadId);

        assertEquals(WorkloadStatus.STOPPED, response.status());
        assertEquals("api-service", response.name());
        verify(workloadRepository).save(workload);
    }
}
