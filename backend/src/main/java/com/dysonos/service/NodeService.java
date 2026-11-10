package com.dysonos.service;

import com.dysonos.dto.NodeRequest;
import com.dysonos.dto.NodeResponse;
import com.dysonos.exception.NodeNotFoundException;
import com.dysonos.model.Node;
import com.dysonos.repository.NodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NodeService {

    private final NodeRepository nodeRepository;

    public NodeService(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    public List<NodeResponse> findAll() {
        return nodeRepository.findAll().stream().map(NodeResponse::from).toList();
    }

    public NodeResponse findById(UUID id) {
        return NodeResponse.from(getNode(id));
    }

    @Transactional
    public NodeResponse create(NodeRequest request) {
        Node node = new Node(request.name().trim(), request.cpuUsage(), request.memoryUsage());
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
