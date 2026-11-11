package com.dysonos.controller;

import com.dysonos.dto.WorkloadRequest;
import com.dysonos.dto.WorkloadResponse;
import com.dysonos.service.WorkloadService;
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
@RequestMapping("/api")
public class WorkloadController {

    private final WorkloadService workloadService;

    public WorkloadController(WorkloadService workloadService) {
        this.workloadService = workloadService;
    }

    @PostMapping("/zones/{zoneId}/workloads")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkloadResponse create(@PathVariable UUID zoneId, @Valid @RequestBody WorkloadRequest request) {
        return workloadService.create(zoneId, request);
    }

    @GetMapping("/zones/{zoneId}/workloads")
    public List<WorkloadResponse> findByZone(@PathVariable UUID zoneId) {
        return workloadService.findByZone(zoneId);
    }

    @DeleteMapping("/workloads/{id}")
    public WorkloadResponse stop(@PathVariable UUID id) {
        return workloadService.stop(id);
    }
}
