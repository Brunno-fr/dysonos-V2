package com.dysonos.controller;

import com.dysonos.dto.ZoneRequest;
import com.dysonos.dto.ZoneMetricsResponse;
import com.dysonos.dto.ZoneResponse;
import com.dysonos.dto.DistributionPlanRequest;
import com.dysonos.dto.DistributionPlanResponse;
import com.dysonos.service.ZoneService;
import com.dysonos.service.ZoneDistributionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/zones")
public class ZoneController {

    private final ZoneService zoneService;
    private final ZoneDistributionService zoneDistributionService;

    public ZoneController(ZoneService zoneService, ZoneDistributionService zoneDistributionService) {
        this.zoneService = zoneService;
        this.zoneDistributionService = zoneDistributionService;
    }

    @GetMapping
    public List<ZoneResponse> findAll() {
        return zoneService.findAll();
    }

    @GetMapping("/{id}")
    public ZoneResponse findById(@PathVariable UUID id) {
        return zoneService.findById(id);
    }

    @GetMapping("/{id}/metrics")
    public ZoneMetricsResponse findMetrics(@PathVariable UUID id) {
        return zoneService.findMetrics(id);
    }

    @PostMapping("/{id}/distribution-plan")
    public DistributionPlanResponse createDistributionPlan(@PathVariable UUID id,
                                                           @Valid @RequestBody DistributionPlanRequest request) {
        return zoneDistributionService.plan(id, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ZoneResponse create(@Valid @RequestBody ZoneRequest request) {
        return zoneService.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        zoneService.delete(id);
    }
}
