package com.dysonos.exception;

import java.util.UUID;

public class NetworkNotFoundException extends RuntimeException {
    public NetworkNotFoundException(UUID id) {
        super("Network não encontrada: " + id);
    }
}
