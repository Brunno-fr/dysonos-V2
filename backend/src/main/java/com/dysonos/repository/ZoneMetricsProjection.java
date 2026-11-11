package com.dysonos.repository;

import java.util.UUID;

public interface ZoneMetricsProjection {
    UUID getZoneId();
    String getName();
    UUID getNetworkId();
    Long getNodeCount();
    Long getOverloadedNodeCount();
    Double getAverageCpuUsage();
    Double getAverageMemoryUsage();
}
