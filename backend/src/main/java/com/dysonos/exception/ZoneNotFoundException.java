package com.dysonos.exception;

import java.util.UUID;

public class ZoneNotFoundException extends RuntimeException {
    public ZoneNotFoundException(UUID id) {
        super("Zone não encontrada: " + id);
    }
}
