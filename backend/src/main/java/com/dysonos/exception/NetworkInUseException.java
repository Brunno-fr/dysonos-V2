package com.dysonos.exception;

import java.util.UUID;

public class NetworkInUseException extends RuntimeException {
    public NetworkInUseException(UUID id) {
        super("Network não pode ser removida enquanto possuir Zones: " + id);
    }
}
