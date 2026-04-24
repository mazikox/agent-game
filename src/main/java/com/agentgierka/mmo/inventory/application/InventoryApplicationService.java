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
    public InventoryResult moveItem(UUID characterId, int fromIndex, int toIndex) {
        InventoryResult result = transactionService.moveItem(characterId, fromIndex, toIndex);
        ensureSuccess(result);
        return result;
    }

    @Retryable(
        includes = OptimisticLockingFailureException.class,
        maxRetries = 3,
        delay = 100
    )
    public InventoryResult addItem(UUID characterId, ItemStack item) {
        InventoryResult result = transactionService.addItem(characterId, item);
        ensureSuccess(result);
        return result;
    }

    @Retryable(
        includes = OptimisticLockingFailureException.class,
        maxRetries = 3,
        delay = 100
    )
    public InventoryResult removeItem(UUID characterId, int index, UUID expectedItemId) {
        InventoryResult result = transactionService.removeItem(characterId, index, expectedItemId);
        ensureSuccess(result);
        return result;
    }

    private void ensureSuccess(InventoryResult result) {
        if (result instanceof InventoryResult.Success) {
            return;
        }

        var error = switch (result) {
            case InventoryResult.Collision c -> 
                new ErrorData("Collision at slot " + c.attemptedIndex() + " with slots " + c.collidingSlots(), "INVENTORY_COLLISION");
            case InventoryResult.OutOfBounds o -> 
                new ErrorData("Item " + o.itemWidth() + "x" + o.itemHeight() + " is out of bounds at index " + o.attemptedIndex(), "INVENTORY_OUT_OF_BOUNDS");
            case InventoryResult.NoSpace _ -> 
                new ErrorData("No space in inventory for this item", "INVENTORY_NO_SPACE");
            case InventoryResult.EmptySlot s -> 
                new ErrorData("Slot " + s.index() + " is empty or item ID mismatch", "INVENTORY_EMPTY_SLOT");
            case InventoryResult.Success _ -> null; // Should not happen due to the if check above
        };

        if (error != null) {
            throw new InventoryException(error.message(), error.code());
        }
    }

    private record ErrorData(String message, String code) {}
}
