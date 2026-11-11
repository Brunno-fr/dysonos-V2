package com.dysonos.service;

import com.dysonos.dto.NodeRequest;
import com.dysonos.dto.NodeResponse;
import com.dysonos.dto.NodeUsageRequest;
import com.dysonos.configuration.NodeUsageThresholdProperties;
import com.dysonos.exception.NodeNotFoundException;
import com.dysonos.exception.ZoneNotFoundException;
import com.dysonos.model.Node;
import com.dysonos.model.NodeStatus;
import com.dysonos.model.Zone;
import com.dysonos.repository.NodeRepository;
import com.dysonos.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NodeService {

    private final NodeRepository nodeRepository;
    private final ZoneRepository zoneRepository;
    private final NodeUsageThresholdProperties usageThresholds;

    public NodeService(NodeRepository nodeRepository, ZoneRepository zoneRepository,
                       NodeUsageThresholdProperties usageThresholds) {
        this.nodeRepository = nodeRepository;
        this.zoneRepository = zoneRepository;
        this.usageThresholds = usageThresholds;
    }

    public List<NodeResponse> findAll() {
        return nodeRepository.findAll().stream().map(NodeResponse::from).toList();
    }

    public NodeResponse findById(UUID id) {
        return NodeResponse.from(getNode(id));
    }

    @Transactional
    public NodeResponse updateUsage(UUID id, NodeUsageRequest request) {
        Node node = getNode(id);
        boolean overloaded = request.cpuUsage() >= usageThresholds.getCpuOverloadThreshold()
                || request.memoryUsage() >= usageThresholds.getMemoryOverloadThreshold();
        NodeStatus status = overloaded ? NodeStatus.OVERLOADED : NodeStatus.ONLINE;
        node.updateUsage(request.cpuUsage(), request.memoryUsage(), status);
        return NodeResponse.from(nodeRepository.save(node));
    }

    @Transactional
    public NodeResponse create(NodeRequest request) {
        Zone zone = request.zoneId() == null ? null : zoneRepository.findById(request.zoneId())
                .orElseThrow(() -> new ZoneNotFoundException(request.zoneId()));
        Node node = new Node(request.name().trim(), request.cpuUsage(), request.memoryUsage(), zone);
        return NodeResponse.from(nodeRepository.save(node));
    }

    @Transactional
    public void delete(UUID id) {
        nodeRepository.delete(getNode(id));
    }

    private Node getNode(UUID id) {
        return nodeRepository.findById(id).orElseThrow(() -> new NodeNotFoundException(id));
    }
}
