package com.dysonos.service;

import com.dysonos.dto.NetworkRequest;
import com.dysonos.dto.NetworkMetricsResponse;
import com.dysonos.dto.NetworkResponse;
import com.dysonos.exception.NetworkInUseException;
import com.dysonos.exception.NetworkNotFoundException;
import com.dysonos.model.Network;
import com.dysonos.repository.NetworkRepository;
import com.dysonos.repository.NetworkMetricsProjection;
import com.dysonos.repository.ZoneRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NetworkServiceTest {

    @Mock
    private NetworkRepository networkRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @InjectMocks
    private NetworkService networkService;

    @Test
    void createTrimsName() {
        when(networkRepository.save(any(Network.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NetworkResponse response = networkService.create(new NetworkRequest(" production "));

        assertEquals("production", response.name());
        assertEquals(0, response.zoneIds().size());
    }

    @Test
    void findByIdThrowsWhenNetworkDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(networkRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NetworkNotFoundException.class, () -> networkService.findById(id));
    }

    @Test
    void findMetricsReturnsAggregatedNetworkUsage() {
        UUID id = UUID.randomUUID();
        Network network = new Network("production");
        NetworkMetricsProjection projection = mock(NetworkMetricsProjection.class);
        when(networkRepository.findById(id)).thenReturn(Optional.of(network));
        when(networkRepository.findMetricsById(id, com.dysonos.model.NodeStatus.OVERLOADED))
                .thenReturn(Optional.of(projection));
        when(projection.getNetworkId()).thenReturn(id);
        when(projection.getName()).thenReturn("production");
        when(projection.getZoneCount()).thenReturn(2L);
        when(projection.getNodeCount()).thenReturn(3L);
        when(projection.getOverloadedNodeCount()).thenReturn(1L);
        when(projection.getAverageCpuUsage()).thenReturn(72.5);
        when(projection.getAverageMemoryUsage()).thenReturn(61.0);

        NetworkMetricsResponse response = networkService.findMetrics(id);

        assertEquals(id, response.networkId());
        assertEquals(2L, response.zoneCount());
        assertEquals(3L, response.nodeCount());
        assertEquals(1L, response.overloadedNodeCount());
        assertEquals(72.5, response.averageCpuUsage());
        assertEquals(61.0, response.averageMemoryUsage());
    }

    @Test
    void findMetricsReturnsZeroAveragesWhenNetworkHasNoNodes() {
        UUID id = UUID.randomUUID();
        NetworkMetricsProjection projection = mock(NetworkMetricsProjection.class);
        when(networkRepository.findById(id)).thenReturn(Optional.of(new Network("empty")));
        when(networkRepository.findMetricsById(id, com.dysonos.model.NodeStatus.OVERLOADED))
                .thenReturn(Optional.of(projection));
        when(projection.getNetworkId()).thenReturn(id);
        when(projection.getName()).thenReturn("empty");

        NetworkMetricsResponse response = networkService.findMetrics(id);

        assertEquals(0L, response.zoneCount());
        assertEquals(0L, response.nodeCount());
        assertEquals(0L, response.overloadedNodeCount());
        assertEquals(0.0, response.averageCpuUsage());
        assertEquals(0.0, response.averageMemoryUsage());
    }

    @Test
    void deleteRemovesNetworkWithoutZones() {
        UUID id = UUID.randomUUID();
        Network network = new Network("production");
        when(networkRepository.findById(id)).thenReturn(Optional.of(network));
        when(zoneRepository.existsByNetworkId(id)).thenReturn(false);

        networkService.delete(id);

        verify(networkRepository).delete(network);
    }

    @Test
    void deleteRejectsNetworkThatContainsZones() {
        UUID id = UUID.randomUUID();
        Network network = new Network("production");
        when(networkRepository.findById(id)).thenReturn(Optional.of(network));
        when(zoneRepository.existsByNetworkId(id)).thenReturn(true);

        assertThrows(NetworkInUseException.class, () -> networkService.delete(id));

        verify(networkRepository, never()).delete(any(Network.class));
    }
}
