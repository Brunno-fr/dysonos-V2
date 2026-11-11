CREATE TABLE IF NOT EXISTS networks (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS zones (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    network_id UUID,
    CONSTRAINT fk_zones_network FOREIGN KEY (network_id) REFERENCES networks(id)
);

CREATE TABLE IF NOT EXISTS nodes (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    cpu_usage DOUBLE PRECISION NOT NULL,
    memory_usage DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    zone_id UUID,
    CONSTRAINT fk_nodes_zone FOREIGN KEY (zone_id) REFERENCES zones(id),
    CONSTRAINT nodes_status_check CHECK (status IN ('ONLINE', 'OVERLOADED'))
);
