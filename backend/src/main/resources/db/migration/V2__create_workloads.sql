CREATE TABLE workloads (
    id UUID PRIMARY KEY,
    zone_id UUID NOT NULL REFERENCES zones(id),
    name VARCHAR(120) NOT NULL,
    cpu_demand DOUBLE PRECISION NOT NULL CHECK (cpu_demand >= 0),
    memory_demand DOUBLE PRECISION NOT NULL CHECK (memory_demand >= 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'STOPPED')),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_workloads_zone_created_at ON workloads(zone_id, created_at DESC);

CREATE TABLE workload_allocations (
    id UUID PRIMARY KEY,
    workload_id UUID NOT NULL REFERENCES workloads(id),
    node_id UUID NOT NULL REFERENCES nodes(id),
    cpu_allocated DOUBLE PRECISION NOT NULL CHECK (cpu_allocated >= 0),
    memory_allocated DOUBLE PRECISION NOT NULL CHECK (memory_allocated >= 0),
    CONSTRAINT uk_workload_allocations_workload_node UNIQUE (workload_id, node_id)
);

CREATE INDEX idx_workload_allocations_node ON workload_allocations(node_id);
