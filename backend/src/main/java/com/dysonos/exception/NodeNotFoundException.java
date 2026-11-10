package com.dysonos.exception;

import java.util.UUID;

public class NodeNotFoundException extends RuntimeException {
    public NodeNotFoundException(UUID id) {
        super("Node não encontrado: " + id);
    }
}
