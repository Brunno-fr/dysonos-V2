package com.dysonos.repository;

import java.util.UUID;

public interface ActiveNodeAllocationProjection {
    UUID getNodeId();
    Double getCpuAllocated();
    Double getMemoryAllocated();
}
