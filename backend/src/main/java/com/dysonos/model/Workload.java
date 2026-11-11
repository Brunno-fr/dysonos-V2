package com.dysonos.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workloads")
public class Workload {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "cpu_demand", nullable = false)
    private double cpuDemand;

    @Column(name = "memory_demand", nullable = false)
    private double memoryDemand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkloadStatus status = WorkloadStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @OneToMany(mappedBy = "workload", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkloadAllocation> allocations = new ArrayList<>();

    protected Workload() {
        // Required by JPA.
    }

    public Workload(String name, double cpuDemand, double memoryDemand, Zone zone) {
        this.name = name;
        this.cpuDemand = cpuDemand;
        this.memoryDemand = memoryDemand;
        this.zone = zone;
        this.status = WorkloadStatus.ACTIVE;
    }

    public void addAllocation(WorkloadAllocation allocation) {
        allocations.add(allocation);
    }

    public void stop() {
        status = WorkloadStatus.STOPPED;
    }

    @PrePersist
    void setCreatedAtIfMissing() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public double getCpuDemand() { return cpuDemand; }
    public double getMemoryDemand() { return memoryDemand; }
    public WorkloadStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Zone getZone() { return zone; }
    public List<WorkloadAllocation> getAllocations() { return allocations; }
}
