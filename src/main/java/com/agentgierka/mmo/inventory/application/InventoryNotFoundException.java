package com.agentgierka.mmo.inventory.application;

import com.agentgierka.mmo.exception.GameBaseException;

public class InventoryNotFoundException extends GameBaseException {
    public InventoryNotFoundException(java.util.UUID characterId) {
        super("Inventory not found for character: " + characterId, "INVENTORY_NOT_FOUND");
    }
}
