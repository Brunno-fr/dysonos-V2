package com.dysonos.controller;

import com.dysonos.dto.NetworkRequest;
import com.dysonos.dto.NetworkResponse;
import com.dysonos.exception.GlobalExceptionHandler;
import com.dysonos.exception.NetworkInUseException;
import com.dysonos.exception.NetworkNotFoundException;
import com.dysonos.service.NetworkService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NetworkController.class)
@Import(GlobalExceptionHandler.class)
class NetworkControllerTest {

    private static final UUID NETWORK_ID = UUID.fromString("adc0037e-6d1b-44e1-9e95-d8320c9bb5e0");
    private static final UUID ZONE_ID = UUID.fromString("743b1fd6-b0a6-4b6a-8680-67c57fdf2cd7");
    private static final NetworkResponse NETWORK = new NetworkResponse(NETWORK_ID, "production",
            Instant.parse("2026-11-10T20:10:53Z"), List.of(ZONE_ID));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NetworkService networkService;

    @Test
    void findAllReturnsNetworks() throws Exception {
        when(networkService.findAll()).thenReturn(List.of(NETWORK));

        mockMvc.perform(get("/api/networks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(NETWORK_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("production"))
                .andExpect(jsonPath("$[0].zoneIds[0]").value(ZONE_ID.toString()));
    }

    @Test
    void findByIdReturnsNetwork() throws Exception {
        when(networkService.findById(NETWORK_ID)).thenReturn(NETWORK);

        mockMvc.perform(get("/api/networks/{id}", NETWORK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(NETWORK_ID.toString()))
                .andExpect(jsonPath("$.zoneIds[0]").value(ZONE_ID.toString()));
    }

    @Test
    void findByIdReturnsNotFoundForMissingNetwork() throws Exception {
        when(networkService.findById(NETWORK_ID)).thenThrow(new NetworkNotFoundException(NETWORK_ID));

        mockMvc.perform(get("/api/networks/{id}", NETWORK_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    void createReturnsCreatedNetwork() throws Exception {
        when(networkService.create(any(NetworkRequest.class))).thenReturn(NETWORK);

        mockMvc.perform(post("/api/networks")
                        .contentType("application/json")
                        .content("""
                                {"name":"production"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(NETWORK_ID.toString()))
                .andExpect(jsonPath("$.name").value("production"));

        verify(networkService).create(new NetworkRequest("production"));
    }

    @Test
    void createRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/networks")
                        .contentType("application/json")
                        .content("""
                                {"name":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verifyNoInteractions(networkService);
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/networks/{id}", NETWORK_ID))
                .andExpect(status().isNoContent());

        verify(networkService).delete(NETWORK_ID);
    }

    @Test
    void deleteReturnsConflictWhenNetworkHasZones() throws Exception {
        doThrow(new NetworkInUseException(NETWORK_ID)).when(networkService).delete(NETWORK_ID);

        mockMvc.perform(delete("/api/networks/{id}", NETWORK_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Network em uso"));
    }
}
