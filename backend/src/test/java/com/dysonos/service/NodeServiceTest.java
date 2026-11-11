package com.dysonos.service;

import com.dysonos.dto.NodeRequest;
import com.dysonos.dto.NodeResponse;
import com.dysonos.dto.NodeUsageRequest;
import com.dysonos.exception.NodeNotFoundException;
import com.dysonos.exception.ZoneNotFoundException;
import com.dysonos.model.Node;
import com.dysonos.model.NodeStatus;
import com.dysonos.model.Zone;
import com.dysonos.repository.NodeRepository;
import com.dysonos.repository.ZoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NodeServiceTest {

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @InjectMocks
    private NodeService nodeService;

    @Test
    void createTrimsNameAndReturnsOnlineNode() {
        Node saved = new Node("edge-01", 25.0, 40.0);
        when(nodeRepository.save(any(Node.class))).thenReturn(saved);

        NodeResponse response = nodeService.create(new NodeRequest(" edge-01 ", 25.0, 40.0, null));

        assertEquals("edge-01", response.name());
        assertEquals(NodeStatus.ONLINE, response.status());
        verify(nodeRepository).save(any(Node.class));
    }

    @Test
    void findByIdThrowsWhenNodeDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(nodeRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NodeNotFoundException.class, () -> nodeService.findById(id));
    }

    @Test
    void updateUsageReplacesCurrentCpuAndMemoryUsage() {
        UUID id = UUID.randomUUID();
        Node node = new Node("edge-01", 25.0, 40.0);
        when(nodeRepository.findById(id)).thenReturn(Optional.of(node));
        when(nodeRepository.save(node)).thenReturn(node);

        NodeResponse response = nodeService.updateUsage(id, new NodeUsageRequest(81.0, 65.0));

        assertEquals(81.0, response.cpuUsage());
        assertEquals(65.0, response.memoryUsage());
        assertEquals(NodeStatus.ONLINE, response.status());
        verify(nodeRepository).save(node);
    }

    @Test
    void updateUsageThrowsWhenNodeDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(nodeRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NodeNotFoundException.class,
                () -> nodeService.updateUsage(id, new NodeUsageRequest(81.0, 65.0)));

        verify(nodeRepository, never()).save(any(Node.class));
    }

    @Test
    void createAssociatesNodeWithRequestedZone() {
        UUID zoneId = UUID.randomUUID();
        Zone zone = new Zone("south-dc");
        when(zoneRepository.findById(zoneId)).thenReturn(Optional.of(zone));
        when(nodeRepository.save(any(Node.class))).thenAnswer(invocation -> invocation.getArgument(0));

        nodeService.create(new NodeRequest("edge-01", 25.0, 40.0, zoneId));

        var savedNode = forClass(Node.class);
        verify(nodeRepository).save(savedNode.capture());
        assertSame(zone, savedNode.getValue().getZone());
    }

    @Test
    void createThrowsWhenRequestedZoneDoesNotExist() {
        UUID zoneId = UUID.randomUUID();
        when(zoneRepository.findById(zoneId)).thenReturn(Optional.empty());

        assertThrows(ZoneNotFoundException.class,
                () -> nodeService.create(new NodeRequest("edge-01", 25.0, 40.0, zoneId)));

        verify(nodeRepository, never()).save(any(Node.class));
    }

    @Test
    void deleteRemovesExistingNode() {
        UUID id = UUID.randomUUID();
        Node node = new Node("edge-01", 10.0, 20.0);
        when(nodeRepository.findById(id)).thenReturn(Optional.of(node));

        nodeService.delete(id);

        verify(nodeRepository).delete(node);
    }
}
