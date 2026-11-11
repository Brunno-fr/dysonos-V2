package com.dysonos.repository;

import java.util.UUID;

public interface NetworkMetricsProjection {
    UUID getNetworkId();
    String getName();
    Long getZoneCount();
    Long getNodeCount();
    Long getOverloadedNodeCount();
    Double getAverageCpuUsage();
    Double getAverageMemoryUsage();
}
