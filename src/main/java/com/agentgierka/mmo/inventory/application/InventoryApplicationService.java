package com.agentgierka.mmo.inventory.application;

import com.agentgierka.mmo.inventory.domain.Inventory;
import com.agentgierka.mmo.inventory.domain.InventoryResult;
import com.agentgierka.mmo.inventory.domain.ItemStack;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryApplicationService {

    private final InventoryTransactionService transactionService;

    public Inventory getInventory(UUID characterId) {
        return transactionService.getInventory(characterId);
    }

    @Retryable(
        includes = OptimisticLockingFailureException.class,
        maxRetries = 3,
        delay = 100
    )
    public InventoryOperationResult moveItem(UUID characterId, int fromIndex, int toIndex) {
        return transactionService.moveItem(characterId, fromIndex, toIndex);
    }

    @Retryable(
        includes = OptimisticLockingFailureException.class,
        maxRetries = 3,
        delay = 100
    )
    public InventoryOperationResult addItem(UUID characterId, ItemStack item) {
        return transactionService.addItem(characterId, item.copy());
    }

    @Retryable(
        includes = OptimisticLockingFailureException.class,
        maxRetries = 3,
        delay = 100
    )
    public InventoryOperationResult removeItem(UUID characterId, int index, UUID expectedItemId) {
        return transactionService.removeItem(characterId, index, expectedItemId);
    }
}
