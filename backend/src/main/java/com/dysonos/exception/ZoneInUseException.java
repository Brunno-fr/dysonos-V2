package com.dysonos.exception;

import java.util.UUID;

public class ZoneInUseException extends RuntimeException {
    public ZoneInUseException(UUID id) {
        super("Zone não pode ser removida enquanto possuir Nodes: " + id);
    }
}
