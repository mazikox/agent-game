package com.agentgierka.mmo.inventory.application;

import com.agentgierka.mmo.inventory.domain.Inventory;
import com.agentgierka.mmo.inventory.domain.ItemStack;
import com.agentgierka.mmo.inventory.infrastructure.db.InventoryRepository;
import com.agentgierka.mmo.inventory.domain.InventoryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class InventoryTransactionService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public Inventory getInventory(UUID characterId) {
        return inventoryRepository.findByCharacterIdWithItems(characterId)
            .orElseThrow(() -> new InventoryNotFoundException(characterId));
    }

    public InventoryOperationResult moveItem(UUID characterId, int fromIndex, int toIndex) {
        Inventory inventory = getInventory(characterId);

        InventoryResult result = inventory.processMove(fromIndex, toIndex);

        if (result instanceof InventoryResult.Success || result instanceof InventoryResult.SwapSuccess) {
            inventoryRepository.save(inventory, characterId);
        }

        return new InventoryOperationResult(result, inventory);
    }

    public InventoryOperationResult addItem(UUID characterId, ItemStack item) {
        Inventory inventory = getInventory(characterId);

        InventoryResult result = inventory.addItem(item);

        if (result instanceof InventoryResult.Success || result instanceof InventoryResult.SwapSuccess) {
            inventoryRepository.save(inventory, characterId);
        }

        return new InventoryOperationResult(result, inventory);
    }

    public InventoryOperationResult removeItem(UUID characterId, int index, UUID expectedItemId) {
        Inventory inventory = getInventory(characterId);

        ItemStack itemAtSlot = inventory.getAnchoredItems().get(index);
        if (itemAtSlot == null || !itemAtSlot.getId().equals(expectedItemId)) {
            return new InventoryOperationResult(new InventoryResult.EmptySlot(index), inventory);
        }

        inventory.removeItem(index);
        inventoryRepository.save(inventory, characterId);

        return new InventoryOperationResult(new InventoryResult.Success(index, -1), inventory);
    }
}
