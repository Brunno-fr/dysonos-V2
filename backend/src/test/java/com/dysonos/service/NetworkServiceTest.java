package com.dysonos.service;

import com.dysonos.dto.NetworkRequest;
import com.dysonos.dto.NetworkResponse;
import com.dysonos.exception.NetworkInUseException;
import com.dysonos.exception.NetworkNotFoundException;
import com.dysonos.model.Network;
import com.dysonos.repository.NetworkRepository;
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
