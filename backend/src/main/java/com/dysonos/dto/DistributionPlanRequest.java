package com.dysonos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record DistributionPlanRequest(
        @NotNull(message = "A demanda de CPU é obrigatória")
        @DecimalMin(value = "0.0", message = "A demanda de CPU não pode ser negativa")
        Double cpuDemand,

        @NotNull(message = "A demanda de memória é obrigatória")
        @DecimalMin(value = "0.0", message = "A demanda de memória não pode ser negativa")
        Double memoryDemand
) {
}
