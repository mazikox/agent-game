package com.agentgierka.mmo.inventory.infrastructure.db;

import com.agentgierka.mmo.inventory.domain.Inventory;
import com.agentgierka.mmo.inventory.domain.ItemDefinition;
import com.agentgierka.mmo.inventory.domain.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class InventoryMapper {

    public static Inventory toDomain(InventoryEntity entity, ItemDefinitionDictionary dictionary) {
        Inventory inventory = new Inventory(entity.getWidth(), entity.getHeight());
        
        for (ItemStackEntity itemEntity : entity.getItems()) {
            ItemDefinition def = dictionary.getById(itemEntity.getItemDefinitionId());
            ItemStack stack = new ItemStack(
                itemEntity.getId(),
                def,
                itemEntity.getQuantity()
            );
            inventory.placeItem(itemEntity.getGridIndex(), stack);
        }
        
        return inventory;
    }

    public static InventoryEntity toEntity(Inventory domain, InventoryEntity existingEntity) {
        Map<UUID, ItemStack> domainItems = domain.getAnchoredItems().values().stream()
            .collect(Collectors.toMap(ItemStack::getId, item -> item));

        // 1. Remove items that are no longer in the domain
        existingEntity.getItems().removeIf(entity -> !domainItems.containsKey(entity.getId()));

        // 2. Update existing and add new items
        for (Map.Entry<Integer, ItemStack> entry : domain.getAnchoredItems().entrySet()) {
            ItemStack domainItem = entry.getValue();
            int gridIndex = entry.getKey();

            existingEntity.getItems().stream()
                .filter(entity -> entity.getId() != null && entity.getId().equals(domainItem.getId()))
                .findFirst()
                .ifPresentOrElse(
                    entity -> {
                        // Update existing
                        entity.setGridIndex(gridIndex);
                        entity.setQuantity(domainItem.getQuantity());
                    },
                    () -> {
                        // Add new
                        existingEntity.getItems().add(ItemStackEntity.builder()
                            .inventoryId(existingEntity.getId())
                            .itemDefinitionId(domainItem.getDefinition().id())
                            .gridIndex(gridIndex)
                            .quantity(domainItem.getQuantity())
                            .build());
                    }
                );
        }

        return existingEntity;
    }
}
