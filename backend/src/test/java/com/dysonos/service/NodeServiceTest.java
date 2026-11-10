package com.dysonos.service;

import com.dysonos.dto.NodeRequest;
import com.dysonos.dto.NodeResponse;
import com.dysonos.exception.NodeNotFoundException;
import com.dysonos.model.Node;
import com.dysonos.model.NodeStatus;
import com.dysonos.repository.NodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NodeServiceTest {

    @Mock
    private NodeRepository nodeRepository;

    @InjectMocks
    private NodeService nodeService;

    @Test
    void createTrimsNameAndReturnsOnlineNode() {
        Node saved = new Node("edge-01", 25.0, 40.0);
        when(nodeRepository.save(any(Node.class))).thenReturn(saved);

        NodeResponse response = nodeService.create(new NodeRequest(" edge-01 ", 25.0, 40.0));

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
    void deleteRemovesExistingNode() {
        UUID id = UUID.randomUUID();
        Node node = new Node("edge-01", 10.0, 20.0);
        when(nodeRepository.findById(id)).thenReturn(Optional.of(node));

        nodeService.delete(id);

        verify(nodeRepository).delete(node);
    }
}
