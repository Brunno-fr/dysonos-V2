package com.dysonos.dto;

import com.dysonos.repository.ZoneMetricsProjection;

import java.util.UUID;

public record ZoneMetricsResponse(
        UUID zoneId,
        String name,
        UUID networkId,
        long nodeCount,
        long overloadedNodeCount,
        double averageCpuUsage,
        double averageMemoryUsage
) {
    public static ZoneMetricsResponse from(ZoneMetricsProjection projection) {
        return new ZoneMetricsResponse(
                projection.getZoneId(),
                projection.getName(),
                projection.getNetworkId(),
                valueOrZero(projection.getNodeCount()),
                valueOrZero(projection.getOverloadedNodeCount()),
                valueOrZero(projection.getAverageCpuUsage()),
                valueOrZero(projection.getAverageMemoryUsage())
        );
    }

    private static long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private static double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }
}
