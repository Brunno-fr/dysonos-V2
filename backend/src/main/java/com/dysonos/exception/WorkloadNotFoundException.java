package com.dysonos.exception;

import java.util.UUID;

public class WorkloadNotFoundException extends RuntimeException {
    public WorkloadNotFoundException(UUID id) {
        super("Workload não encontrada: " + id);
    }
}
