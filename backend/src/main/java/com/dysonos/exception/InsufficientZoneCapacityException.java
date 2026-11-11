package com.dysonos.exception;

import java.util.UUID;

public class InsufficientZoneCapacityException extends RuntimeException {
    public InsufficientZoneCapacityException(UUID zoneId) {
        super("A Zone " + zoneId + " não tem capacidade ONLINE suficiente para essa demanda.");
    }
}
