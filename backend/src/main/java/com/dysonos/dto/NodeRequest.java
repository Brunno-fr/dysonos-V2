package com.dysonos.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NodeRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String name,

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
