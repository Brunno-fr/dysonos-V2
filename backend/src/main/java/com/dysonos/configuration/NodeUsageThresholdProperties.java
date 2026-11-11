package com.dysonos.configuration;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "dysonos.node")
public class NodeUsageThresholdProperties {

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private double cpuOverloadThreshold = 80.0;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private double memoryOverloadThreshold = 80.0;

    public double getCpuOverloadThreshold() {
        return cpuOverloadThreshold;
    }

    public void setCpuOverloadThreshold(double cpuOverloadThreshold) {
        this.cpuOverloadThreshold = cpuOverloadThreshold;
    }

    public double getMemoryOverloadThreshold() {
        return memoryOverloadThreshold;
    }

    public void setMemoryOverloadThreshold(double memoryOverloadThreshold) {
        this.memoryOverloadThreshold = memoryOverloadThreshold;
    }
}
