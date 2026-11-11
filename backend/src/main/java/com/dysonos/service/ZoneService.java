package com.dysonos.service;

import com.dysonos.dto.ZoneRequest;
import com.dysonos.dto.ZoneMetricsResponse;
import com.dysonos.dto.ZoneResponse;
import com.dysonos.exception.NetworkNotFoundException;
import com.dysonos.exception.ZoneInUseException;
import com.dysonos.exception.ZoneNotFoundException;
import com.dysonos.model.Network;
import com.dysonos.model.NodeStatus;
import com.dysonos.model.Zone;
import com.dysonos.repository.NetworkRepository;
import com.dysonos.repository.NodeRepository;
import com.dysonos.repository.ZoneRepository;
import com.dysonos.repository.WorkloadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final NodeRepository nodeRepository;
    private final NetworkRepository networkRepository;
    private final WorkloadRepository workloadRepository;

    public ZoneService(ZoneRepository zoneRepository, NodeRepository nodeRepository,
                       NetworkRepository networkRepository, WorkloadRepository workloadRepository) {
        this.zoneRepository = zoneRepository;
        this.nodeRepository = nodeRepository;
        this.networkRepository = networkRepository;
        this.workloadRepository = workloadRepository;
    }

    public List<ZoneResponse> findAll() {
        return zoneRepository.findAll().stream().map(ZoneResponse::from).toList();
    }

    public ZoneResponse findById(UUID id) {
        return ZoneResponse.from(getZone(id));
    }

    public ZoneMetricsResponse findMetrics(UUID id) {
        getZone(id);
        return zoneRepository.findMetricsById(id, NodeStatus.OVERLOADED)
                .map(ZoneMetricsResponse::from)
                .orElseThrow(() -> new ZoneNotFoundException(id));
    }

    @Transactional
    public ZoneResponse create(ZoneRequest request) {
        Network network = request.networkId() == null ? null : networkRepository.findById(request.networkId())
                .orElseThrow(() -> new NetworkNotFoundException(request.networkId()));
        Zone zone = new Zone(request.name().trim(), network);
        return ZoneResponse.from(zoneRepository.save(zone));
    }

    @Transactional
    public void delete(UUID id) {
        Zone zone = getZone(id);
        if (nodeRepository.existsByZoneId(id)) {
            throw new ZoneInUseException(id);
        }
        if (workloadRepository.existsByZoneId(id)) {
            throw new ZoneInUseException(id);
        }
        zoneRepository.delete(zone);
    }

    private Zone getZone(UUID id) {
        return zoneRepository.findById(id).orElseThrow(() -> new ZoneNotFoundException(id));
    }
}
