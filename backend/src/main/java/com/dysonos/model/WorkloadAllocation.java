package com.dysonos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "workload_allocations", uniqueConstraints =
        @UniqueConstraint(name = "uk_workload_allocations_workload_node", columnNames = {"workload_id", "node_id"}))
public class WorkloadAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workload_id", nullable = false)
    private Workload workload;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "node_id", nullable = false)
    private Node node;

    @Column(name = "cpu_allocated", nullable = false)
    private double cpuAllocated;

    @Column(name = "memory_allocated", nullable = false)
    private double memoryAllocated;

    protected WorkloadAllocation() {
        // Required by JPA.
    }

    public WorkloadAllocation(Workload workload, Node node, double cpuAllocated, double memoryAllocated) {
        this.workload = workload;
        this.node = node;
        this.cpuAllocated = cpuAllocated;
        this.memoryAllocated = memoryAllocated;
    }

    public UUID getId() { return id; }
    public Workload getWorkload() { return workload; }
    public Node getNode() { return node; }
    public double getCpuAllocated() { return cpuAllocated; }
    public double getMemoryAllocated() { return memoryAllocated; }
}
