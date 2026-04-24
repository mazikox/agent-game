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

    public InventoryResult moveItem(UUID characterId, int fromIndex, int toIndex) {
        Inventory inventory = getInventory(characterId);

        InventoryResult result = inventory.processMove(fromIndex, toIndex);

        if (result instanceof InventoryResult.Success) {
            inventoryRepository.save(inventory, characterId);
        }

        return result;
    }

    public InventoryResult addItem(UUID characterId, ItemStack item) {
        Inventory inventory = getInventory(characterId);

        InventoryResult result = inventory.addItem(item);

        if (result instanceof InventoryResult.Success) {
            inventoryRepository.save(inventory, characterId);
        }

        return result;
    }

    public InventoryResult removeItem(UUID characterId, int index, UUID expectedItemId) {
        Inventory inventory = getInventory(characterId);

        ItemStack itemAtSlot = inventory.getAnchoredItems().get(index);
        if (itemAtSlot == null || !itemAtSlot.getId().equals(expectedItemId)) {
            return new InventoryResult.EmptySlot(index);
        }

        inventory.removeItem(index);
        inventoryRepository.save(inventory, characterId);

        return new InventoryResult.Success(index, -1);
    }
}
