package com.dysonos.controller;

import com.dysonos.dto.NodeRequest;
import com.dysonos.dto.NodeResponse;
import com.dysonos.dto.NodeUsageRequest;
import com.dysonos.service.NodeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class NodeController {

    private final NodeService nodeService;

    public NodeController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/nodes")
    public List<NodeResponse> findAll() {
        return nodeService.findAll();
    }

    @GetMapping("/nodes/{id}")
    public NodeResponse findById(@PathVariable UUID id) {
        return nodeService.findById(id);
    }

    @PutMapping("/nodes/{id}/usage")
    public NodeResponse updateUsage(@PathVariable UUID id, @Valid @RequestBody NodeUsageRequest request) {
        return nodeService.updateUsage(id, request);
    }

    @PostMapping("/nodes")
    @ResponseStatus(HttpStatus.CREATED)
    public NodeResponse create(@Valid @RequestBody NodeRequest request) {
        return nodeService.create(request);
    }

    @DeleteMapping("/nodes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        nodeService.delete(id);
    }
}
