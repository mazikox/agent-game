package com.agentgierka.mmo.inventory.application;

import com.agentgierka.mmo.inventory.domain.Inventory;
import com.agentgierka.mmo.inventory.domain.InventoryRepository;
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

    public InventoryResult moveItem(UUID characterId, int fromIndex, int toIndex) {
        Inventory inventory = inventoryRepository.findByCharacterIdWithItems(characterId)
            .orElseThrow(() -> new InventoryNotFoundException(characterId));

        InventoryResult result = inventory.processMove(fromIndex, toIndex);

        if (result instanceof InventoryResult.Success) {
            inventoryRepository.save(inventory);
        }

        return result;
    }
}
