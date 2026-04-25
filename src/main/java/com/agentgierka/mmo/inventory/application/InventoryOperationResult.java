package com.agentgierka.mmo.inventory.application;

import com.agentgierka.mmo.inventory.domain.Inventory;
import com.agentgierka.mmo.inventory.domain.InventoryResult;

public record InventoryOperationResult(
    InventoryResult result,
    Inventory inventory
) {}
