package com.dysonos.dto;

import com.dysonos.model.Node;
import com.dysonos.model.NodeStatus;

import java.time.Instant;
import java.util.UUID;

public record NodeResponse(UUID id, String name, double cpuUsage, double memoryUsage,
                           NodeStatus status, Instant createdAt) {

    public static NodeResponse from(Node node) {
        return new NodeResponse(node.getId(), node.getName(), node.getCpuUsage(),
                node.getMemoryUsage(), node.getStatus(), node.getCreatedAt());
    }
}
