package com.dysonos.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkloadRequest(
        @NotBlank(message = "O nome da demanda é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String name,

        @NotNull(message = "A demanda de CPU é obrigatória")
        @DecimalMin(value = "0.0", message = "A demanda de CPU não pode ser negativa")
        Double cpuDemand,

        @NotNull(message = "A demanda de memória é obrigatória")
        @DecimalMin(value = "0.0", message = "A demanda de memória não pode ser negativa")
        Double memoryDemand
) {
    @AssertTrue(message = "A demanda de CPU e memória não pode ser zero ao mesmo tempo")
    public boolean hasResourceDemand() {
        return cpuDemand != null && memoryDemand != null && (cpuDemand > 0 || memoryDemand > 0);
    }
}
