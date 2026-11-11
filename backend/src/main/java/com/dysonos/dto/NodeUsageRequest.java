package com.dysonos.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record NodeUsageRequest(
        @NotNull(message = "O uso de CPU é obrigatório")
        @DecimalMin(value = "0.0", message = "O uso de CPU deve ser no mínimo 0")
        @DecimalMax(value = "100.0", message = "O uso de CPU deve ser no máximo 100")
        Double cpuUsage,

        @NotNull(message = "O uso de memória é obrigatório")
        @DecimalMin(value = "0.0", message = "O uso de memória deve ser no mínimo 0")
        @DecimalMax(value = "100.0", message = "O uso de memória deve ser no máximo 100")
        Double memoryUsage
) {
}
