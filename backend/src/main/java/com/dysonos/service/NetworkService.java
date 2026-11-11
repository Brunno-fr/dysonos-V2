package com.dysonos.service;

import com.dysonos.dto.NetworkRequest;
import com.dysonos.dto.NetworkMetricsResponse;
import com.dysonos.dto.NetworkResponse;
import com.dysonos.exception.NetworkInUseException;
import com.dysonos.exception.NetworkNotFoundException;
import com.dysonos.model.Network;
import com.dysonos.model.NodeStatus;
import com.dysonos.repository.NetworkRepository;
import com.dysonos.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NetworkService {

    private final NetworkRepository networkRepository;
    private final ZoneRepository zoneRepository;

    public NetworkService(NetworkRepository networkRepository, ZoneRepository zoneRepository) {
        this.networkRepository = networkRepository;
        this.zoneRepository = zoneRepository;
    }

    public List<NetworkResponse> findAll() {
        return networkRepository.findAll().stream().map(NetworkResponse::from).toList();
    }

    public NetworkResponse findById(UUID id) {
        return NetworkResponse.from(getNetwork(id));
    }

    public NetworkMetricsResponse findMetrics(UUID id) {
        getNetwork(id);
        return networkRepository.findMetricsById(id, NodeStatus.OVERLOADED)
                .map(NetworkMetricsResponse::from)
                .orElseThrow(() -> new NetworkNotFoundException(id));
    }

    @Transactional
    public NetworkResponse create(NetworkRequest request) {
        Network network = new Network(request.name().trim());
        return NetworkResponse.from(networkRepository.save(network));
    }

    @Transactional
    public void delete(UUID id) {
        Network network = getNetwork(id);
        if (zoneRepository.existsByNetworkId(id)) {
            throw new NetworkInUseException(id);
        }
        networkRepository.delete(network);
    }

    private Network getNetwork(UUID id) {
        return networkRepository.findById(id).orElseThrow(() -> new NetworkNotFoundException(id));
    }
}
