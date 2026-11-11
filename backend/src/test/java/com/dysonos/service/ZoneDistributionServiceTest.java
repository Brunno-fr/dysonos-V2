package com.dysonos.service;

import com.dysonos.configuration.NodeUsageThresholdProperties;
import com.dysonos.dto.DistributionPlanRequest;
import com.dysonos.dto.DistributionPlanResponse;
import com.dysonos.exception.ZoneNotFoundException;
import com.dysonos.model.Node;
import com.dysonos.model.NodeStatus;
import com.dysonos.repository.NodeRepository;
import com.dysonos.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZoneDistributionServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private NodeRepository nodeRepository;

    private ZoneDistributionService service;

    @BeforeEach
    void setUp() {
        service = new ZoneDistributionService(zoneRepository, nodeRepository, new NodeUsageThresholdProperties());
    }

    @Test
    void planSplitsDemandAcrossOnlineNodesWithoutChangingTheirReadings() {
        UUID zoneId = UUID.randomUUID();
        Node first = node("edge-01", 60, 50);
        Node second = node("edge-02", 60, 50);
        when(zoneRepository.existsById(zoneId)).thenReturn(true);
        when(nodeRepository.findAllByZoneIdAndStatusOrderById(zoneId, NodeStatus.ONLINE))
                .thenReturn(List.of(first, second));

        DistributionPlanResponse response = service.plan(zoneId, new DistributionPlanRequest(30.0, 20.0));

        assertTrue(response.feasible());
        assertEquals(2, response.allocations().size());
        assertEquals(30.0, response.allocations().stream()
                .mapToDouble(DistributionPlanResponse.NodeAllocation::allocatedCpu).sum(), 0.000001);
        assertEquals(20.0, response.allocations().stream()
                .mapToDouble(DistributionPlanResponse.NodeAllocation::allocatedMemory).sum(), 0.000001);
        assertTrue(response.allocations().stream()
                .allMatch(allocation -> allocation.projectedCpuUsage() < 80.0
                        && allocation.projectedMemoryUsage() < 80.0));
        verify(first, never()).updateUsage(org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any());
        verify(second, never()).updateUsage(org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void planReportsInsufficientCapacityWithoutReturningPartialAllocations() {
        UUID zoneId = UUID.randomUUID();
        Node node = nodeWithUsage(70, 70);
        when(zoneRepository.existsById(zoneId)).thenReturn(true);
        when(nodeRepository.findAllByZoneIdAndStatusOrderById(zoneId, NodeStatus.ONLINE))
                .thenReturn(List.of(node));

        DistributionPlanResponse response = service.plan(zoneId, new DistributionPlanRequest(20.0, 10.0));

        assertFalse(response.feasible());
        assertTrue(response.allocations().isEmpty());
        assertEquals(9.99, response.availableCpu(), 0.000001);
        assertEquals(9.99, response.availableMemory(), 0.000001);
    }

    @Test
    void planIgnoresOverloadedNodes() {
        UUID zoneId = UUID.randomUUID();
        when(zoneRepository.existsById(zoneId)).thenReturn(true);
        when(nodeRepository.findAllByZoneIdAndStatusOrderById(zoneId, NodeStatus.ONLINE))
                .thenReturn(List.of());

        DistributionPlanResponse response = service.plan(zoneId, new DistributionPlanRequest(1.0, 1.0));

        assertFalse(response.feasible());
        assertEquals(0.0, response.availableCpu());
        assertEquals(0.0, response.availableMemory());
        verify(nodeRepository).findAllByZoneIdAndStatusOrderById(zoneId, NodeStatus.ONLINE);
    }

    @Test
    void planAllowsZeroDemandWithNoNodes() {
        UUID zoneId = UUID.randomUUID();
        when(zoneRepository.existsById(zoneId)).thenReturn(true);
        when(nodeRepository.findAllByZoneIdAndStatusOrderById(zoneId, NodeStatus.ONLINE))
                .thenReturn(List.of());

        DistributionPlanResponse response = service.plan(zoneId, new DistributionPlanRequest(0.0, 0.0));

        assertTrue(response.feasible());
        assertTrue(response.allocations().isEmpty());
    }

    @Test
    void planThrowsWhenZoneDoesNotExist() {
        UUID zoneId = UUID.randomUUID();
        when(zoneRepository.existsById(zoneId)).thenReturn(false);

        assertThrows(ZoneNotFoundException.class,
                () -> service.plan(zoneId, new DistributionPlanRequest(1.0, 1.0)));
    }

    private Node node(String name, double cpu, double memory) {
        Node node = org.mockito.Mockito.mock(Node.class);
        when(node.getId()).thenReturn(UUID.randomUUID());
        when(node.getName()).thenReturn(name);
        when(node.getCpuUsage()).thenReturn(cpu);
        when(node.getMemoryUsage()).thenReturn(memory);
        return node;
    }

    private Node nodeWithUsage(double cpu, double memory) {
        Node node = org.mockito.Mockito.mock(Node.class);
        when(node.getCpuUsage()).thenReturn(cpu);
        when(node.getMemoryUsage()).thenReturn(memory);
        return node;
    }
}
