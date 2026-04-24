package com.agentgierka.mmo.inventory.exception;

import com.agentgierka.mmo.exception.GameBaseException;

public class InventoryException extends GameBaseException {
    public InventoryException(String message, String errorCode) {
        super(message, errorCode);
    }
}
