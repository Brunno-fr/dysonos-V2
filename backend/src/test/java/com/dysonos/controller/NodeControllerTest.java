package com.dysonos.controller;

import com.dysonos.dto.NodeRequest;
import com.dysonos.dto.NodeResponse;
import com.dysonos.exception.GlobalExceptionHandler;
import com.dysonos.exception.NodeNotFoundException;
import com.dysonos.model.NodeStatus;
import com.dysonos.service.NodeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NodeController.class)
@Import(GlobalExceptionHandler.class)
class NodeControllerTest {

    private static final UUID NODE_ID = UUID.fromString("ce84c4fd-ce9d-4dcd-b94a-cedbf6a2fe3d");
    private static final NodeResponse NODE = new NodeResponse(NODE_ID, "edge-01", 25.0, 40.0,
            NodeStatus.ONLINE, null, Instant.parse("2026-11-10T20:10:53Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NodeService nodeService;

    @Test
    void healthReturnsUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        verifyNoInteractions(nodeService);
    }

    @Test
    void findAllReturnsNodes() throws Exception {
        when(nodeService.findAll()).thenReturn(List.of(NODE));

        mockMvc.perform(get("/api/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(NODE_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("edge-01"))
                .andExpect(jsonPath("$[0].status").value("ONLINE"));
    }

    @Test
    void findByIdReturnsNode() throws Exception {
        when(nodeService.findById(NODE_ID)).thenReturn(NODE);

        mockMvc.perform(get("/api/nodes/{id}", NODE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(NODE_ID.toString()))
                .andExpect(jsonPath("$.name").value("edge-01"));
    }

    @Test
    void findByIdReturnsNotFoundForMissingNode() throws Exception {
        when(nodeService.findById(NODE_ID)).thenThrow(new NodeNotFoundException(NODE_ID));

        mockMvc.perform(get("/api/nodes/{id}", NODE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    void createReturnsCreatedNode() throws Exception {
        when(nodeService.create(any(NodeRequest.class))).thenReturn(NODE);

        mockMvc.perform(post("/api/nodes")
                        .contentType("application/json")
                        .content("""
                                {"name":"edge-01","cpuUsage":25.0,"memoryUsage":40.0}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(NODE_ID.toString()))
                .andExpect(jsonPath("$.status").value("ONLINE"));

        verify(nodeService).create(new NodeRequest("edge-01", 25.0, 40.0, null));
    }

    @ParameterizedTest
    @CsvSource({
            "101, 40, cpuUsage",
            "-1, 40, cpuUsage",
            "25, 101, memoryUsage",
            "25, -1, memoryUsage"
    })
    void createRejectsResourceUsageOutsideAllowedRange(double cpuUsage, double memoryUsage, String field)
            throws Exception {
        String requestBody = """
                {"name":"edge-01","cpuUsage":%s,"memoryUsage":%s}
                """.formatted(cpuUsage, memoryUsage);

        mockMvc.perform(post("/api/nodes")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Falha de validação"))
                .andExpect(jsonPath("$.errors." + field).exists());

        verifyNoInteractions(nodeService);
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/nodes/{id}", NODE_ID))
                .andExpect(status().isNoContent());

        verify(nodeService).delete(NODE_ID);
    }
}
