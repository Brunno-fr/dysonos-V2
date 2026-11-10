package com.dysonos.dto;

import com.dysonos.model.Zone;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ZoneResponse(UUID id, String name, Instant createdAt, List<UUID> nodeIds) {

    public static ZoneResponse from(Zone zone) {
        List<UUID> nodeIds = zone.getNodes().stream().map(node -> node.getId()).toList();
        return new ZoneResponse(zone.getId(), zone.getName(), zone.getCreatedAt(), nodeIds);
    }
}
