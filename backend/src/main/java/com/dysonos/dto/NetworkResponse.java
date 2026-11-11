package com.dysonos.dto;

import com.dysonos.model.Network;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NetworkResponse(UUID id, String name, Instant createdAt, List<UUID> zoneIds) {

    public static NetworkResponse from(Network network) {
        List<UUID> zoneIds = network.getZones().stream().map(zone -> zone.getId()).toList();
        return new NetworkResponse(network.getId(), network.getName(), network.getCreatedAt(), zoneIds);
    }
}
