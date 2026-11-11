package com.dysonos.controller;

import com.dysonos.dto.NetworkRequest;
import com.dysonos.dto.NetworkMetricsResponse;
import com.dysonos.dto.NetworkResponse;
import com.dysonos.service.NetworkService;
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
@RequestMapping("/api/networks")
public class NetworkController {

    private final NetworkService networkService;

    public NetworkController(NetworkService networkService) {
        this.networkService = networkService;
    }

    @GetMapping
    public List<NetworkResponse> findAll() {
        return networkService.findAll();
    }

    @GetMapping("/{id}")
    public NetworkResponse findById(@PathVariable UUID id) {
        return networkService.findById(id);
    }

    @GetMapping("/{id}/metrics")
    public NetworkMetricsResponse findMetrics(@PathVariable UUID id) {
        return networkService.findMetrics(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NetworkResponse create(@Valid @RequestBody NetworkRequest request) {
        return networkService.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        networkService.delete(id);
    }
}
