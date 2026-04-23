package com.agentgierka.mmo.inventory.domain;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {

    Optional<Inventory> findByCharacterIdWithItems(UUID characterId);

    void save(Inventory inventory);
}
