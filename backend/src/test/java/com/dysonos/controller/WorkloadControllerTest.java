package com.dysonos.controller;

import com.dysonos.dto.WorkloadRequest;
import com.dysonos.dto.WorkloadResponse;
import com.dysonos.exception.GlobalExceptionHandler;
import com.dysonos.exception.InsufficientZoneCapacityException;
import com.dysonos.model.WorkloadStatus;
import com.dysonos.service.WorkloadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkloadController.class)
@Import(GlobalExceptionHandler.class)
class WorkloadControllerTest {

    private static final UUID ZONE_ID = UUID.fromString("743b1fd6-b0a6-4b6a-8680-67c57fdf2cd7");
    private static final UUID WORKLOAD_ID = UUID.fromString("de3d2ca4-78fc-49ea-bcbf-1a58639dad8e");
    private static final UUID NODE_ID = UUID.fromString("ce84c4fd-ce9d-4dcd-b94a-cedbf6a2fe3d");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkloadService workloadService;

    @Test
    void createReturnsPersistedWorkloadAndAllocation() throws Exception {
        WorkloadResponse response = new WorkloadResponse(WORKLOAD_ID, "api-service", ZONE_ID,
                30, 20, WorkloadStatus.ACTIVE, Instant.parse("2026-11-11T14:00:00Z"),
                List.of(new WorkloadResponse.Allocation(NODE_ID, "edge-01", 30, 20)));
        when(workloadService.create(ZONE_ID, new WorkloadRequest("api-service", 30.0, 20.0)))
                .thenReturn(response);

        mockMvc.perform(post("/api/zones/{zoneId}/workloads", ZONE_ID)
                        .contentType("application/json")
                        .content("""
                                {"name":"api-service","cpuDemand":30,"memoryDemand":20}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(WORKLOAD_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.allocations[0].nodeId").value(NODE_ID.toString()))
                .andExpect(jsonPath("$.allocations[0].cpuAllocated").value(30.0));
    }

    @Test
    void createRejectsNegativeOrEmptyDemand() throws Exception {
        mockMvc.perform(post("/api/zones/{zoneId}/workloads", ZONE_ID)
                        .contentType("application/json")
                        .content("""
                                {"name":"api-service","cpuDemand":0,"memoryDemand":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.resourceDemand").exists());

        verifyNoInteractions(workloadService);
    }

    @Test
    void createReturnsConflictWhenCapacityIsInsufficient() throws Exception {
        when(workloadService.create(ZONE_ID, new WorkloadRequest("large", 100.0, 100.0)))
                .thenThrow(new InsufficientZoneCapacityException(ZONE_ID));

        mockMvc.perform(post("/api/zones/{zoneId}/workloads", ZONE_ID)
                        .contentType("application/json")
                        .content("""
                                {"name":"large","cpuDemand":100,"memoryDemand":100}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Capacidade insuficiente"));
    }

    @Test
    void deleteStopsWorkloadAndReturnsUpdatedStatus() throws Exception {
        WorkloadResponse response = new WorkloadResponse(WORKLOAD_ID, "api-service", ZONE_ID,
                30, 20, WorkloadStatus.STOPPED, Instant.parse("2026-11-11T14:00:00Z"), List.of());
        when(workloadService.stop(WORKLOAD_ID)).thenReturn(response);

        mockMvc.perform(delete("/api/workloads/{id}", WORKLOAD_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STOPPED"));

        verify(workloadService).stop(WORKLOAD_ID);
    }
}
