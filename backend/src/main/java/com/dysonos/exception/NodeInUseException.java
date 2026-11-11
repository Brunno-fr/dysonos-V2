package com.dysonos.exception;

import java.util.UUID;

public class NodeInUseException extends RuntimeException {
    public NodeInUseException(UUID nodeId) {
        super("Node " + nodeId + " possui alocações de Workload e não pode ser removido.");
    }
}
