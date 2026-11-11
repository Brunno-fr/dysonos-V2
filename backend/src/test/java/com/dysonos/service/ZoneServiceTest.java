package com.dysonos.service;

import com.dysonos.dto.ZoneRequest;
import com.dysonos.dto.ZoneResponse;
import com.dysonos.exception.NetworkNotFoundException;
import com.dysonos.exception.ZoneInUseException;
import com.dysonos.exception.ZoneNotFoundException;
import com.dysonos.model.Zone;
import com.dysonos.model.Network;
import com.dysonos.repository.NetworkRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZoneServiceTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private NetworkRepository networkRepository;

    @InjectMocks
    private ZoneService zoneService;

    @Test
    void createTrimsName() {
        when(zoneRepository.save(any(Zone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ZoneResponse response = zoneService.create(new ZoneRequest(" south-dc ", null));

        assertEquals("south-dc", response.name());
        assertEquals(0, response.nodeIds().size());
    }

    @Test
    void createAssociatesZoneWithRequestedNetwork() {
        UUID networkId = UUID.randomUUID();
        Network network = new Network("production");
        when(networkRepository.findById(networkId)).thenReturn(Optional.of(network));
        when(zoneRepository.save(any(Zone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        zoneService.create(new ZoneRequest("south-dc", networkId));

        var savedZone = org.mockito.ArgumentCaptor.forClass(Zone.class);
        verify(zoneRepository).save(savedZone.capture());
        assertEquals(network, savedZone.getValue().getNetwork());
    }

    @Test
    void createThrowsWhenRequestedNetworkDoesNotExist() {
        UUID networkId = UUID.randomUUID();
        when(networkRepository.findById(networkId)).thenReturn(Optional.empty());

        assertThrows(NetworkNotFoundException.class,
                () -> zoneService.create(new ZoneRequest("south-dc", networkId)));
    }

    @Test
    void findByIdThrowsWhenZoneDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(zoneRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ZoneNotFoundException.class, () -> zoneService.findById(id));
    }

    @Test
    void deleteRemovesUnusedZone() {
        UUID id = UUID.randomUUID();
        Zone zone = new Zone("south-dc");
        when(zoneRepository.findById(id)).thenReturn(Optional.of(zone));
        when(nodeRepository.existsByZoneId(id)).thenReturn(false);

        zoneService.delete(id);

        verify(zoneRepository).delete(zone);
    }

    @Test
    void deleteRejectsZoneThatContainsNodes() {
        UUID id = UUID.randomUUID();
        Zone zone = new Zone("south-dc");
        when(zoneRepository.findById(id)).thenReturn(Optional.of(zone));
        when(nodeRepository.existsByZoneId(id)).thenReturn(true);

        assertThrows(ZoneInUseException.class, () -> zoneService.delete(id));

        verify(zoneRepository, never()).delete(any(Zone.class));
    }
}
