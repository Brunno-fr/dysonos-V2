package com.dysonos.dto;

import com.dysonos.repository.NetworkMetricsProjection;

import java.util.UUID;

public record NetworkMetricsResponse(
        UUID networkId,
        String name,
        long zoneCount,
        long nodeCount,
        long overloadedNodeCount,
        double averageCpuUsage,
        double averageMemoryUsage
) {
    public static NetworkMetricsResponse from(NetworkMetricsProjection projection) {
        return new NetworkMetricsResponse(
                projection.getNetworkId(),
                projection.getName(),
                valueOrZero(projection.getZoneCount()),
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
