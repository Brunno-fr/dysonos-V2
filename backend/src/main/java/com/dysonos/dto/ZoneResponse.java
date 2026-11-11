package com.dysonos.dto;

import com.dysonos.model.Zone;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ZoneResponse(UUID id, String name, UUID networkId, Instant createdAt, List<UUID> nodeIds) {

    public static ZoneResponse from(Zone zone) {
        List<UUID> nodeIds = zone.getNodes().stream().map(node -> node.getId()).toList();
        UUID networkId = zone.getNetwork() == null ? null : zone.getNetwork().getId();
        return new ZoneResponse(zone.getId(), zone.getName(), networkId, zone.getCreatedAt(), nodeIds);
    }
}
