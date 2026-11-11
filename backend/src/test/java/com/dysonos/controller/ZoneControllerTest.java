package com.dysonos.controller;

import com.dysonos.dto.ZoneRequest;
import com.dysonos.dto.ZoneResponse;
import com.dysonos.exception.GlobalExceptionHandler;
import com.dysonos.exception.ZoneInUseException;
import com.dysonos.exception.ZoneNotFoundException;
import com.dysonos.service.ZoneService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ZoneController.class)
@Import(GlobalExceptionHandler.class)
class ZoneControllerTest {

    private static final UUID ZONE_ID = UUID.fromString("743b1fd6-b0a6-4b6a-8680-67c57fdf2cd7");
    private static final UUID NODE_ID = UUID.fromString("ce84c4fd-ce9d-4dcd-b94a-cedbf6a2fe3d");
    private static final ZoneResponse ZONE = new ZoneResponse(ZONE_ID, "south-dc", null,
            Instant.parse("2026-11-10T20:10:53Z"), List.of(NODE_ID));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ZoneService zoneService;

    @Test
    void findAllReturnsZones() throws Exception {
        when(zoneService.findAll()).thenReturn(List.of(ZONE));

        mockMvc.perform(get("/api/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ZONE_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("south-dc"))
                .andExpect(jsonPath("$[0].nodeIds[0]").value(NODE_ID.toString()));
    }

    @Test
    void findByIdReturnsZone() throws Exception {
        when(zoneService.findById(ZONE_ID)).thenReturn(ZONE);

        mockMvc.perform(get("/api/zones/{id}", ZONE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ZONE_ID.toString()))
                .andExpect(jsonPath("$.nodeIds[0]").value(NODE_ID.toString()));
    }

    @Test
    void findByIdReturnsNotFoundForMissingZone() throws Exception {
        when(zoneService.findById(ZONE_ID)).thenThrow(new ZoneNotFoundException(ZONE_ID));

        mockMvc.perform(get("/api/zones/{id}", ZONE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    void createReturnsCreatedZone() throws Exception {
        when(zoneService.create(any(ZoneRequest.class))).thenReturn(ZONE);

        mockMvc.perform(post("/api/zones")
                        .contentType("application/json")
                        .content("""
                                {"name":"south-dc"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ZONE_ID.toString()))
                .andExpect(jsonPath("$.name").value("south-dc"));

        verify(zoneService).create(new ZoneRequest("south-dc", null));
    }

    @Test
    void createRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/zones")
                        .contentType("application/json")
                        .content("""
                                {"name":"  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        verifyNoInteractions(zoneService);
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/zones/{id}", ZONE_ID))
                .andExpect(status().isNoContent());

        verify(zoneService).delete(ZONE_ID);
    }

    @Test
    void deleteReturnsConflictWhenZoneHasNodes() throws Exception {
        org.mockito.Mockito.doThrow(new ZoneInUseException(ZONE_ID))
                .when(zoneService).delete(ZONE_ID);

        mockMvc.perform(delete("/api/zones/{id}", ZONE_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Zone em uso"));
    }
}
