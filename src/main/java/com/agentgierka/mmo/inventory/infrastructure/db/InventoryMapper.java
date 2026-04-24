package com.agentgierka.mmo.inventory.infrastructure.db;

import com.agentgierka.mmo.inventory.domain.Inventory;
import com.agentgierka.mmo.inventory.domain.ItemStack;
import com.agentgierka.mmo.inventory.domain.ItemDefinition;

import java.util.List;
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
        java.util.Map<java.util.UUID, com.agentgierka.mmo.inventory.domain.ItemStack> domainItems = domain.getAnchoredItems().values().stream()
            .collect(java.util.stream.Collectors.toMap(com.agentgierka.mmo.inventory.domain.ItemStack::getId, item -> item));

        // 1. Remove items that are no longer in the domain
        existingEntity.getItems().removeIf(entity -> !domainItems.containsKey(entity.getId()));

        // 2. Update existing and add new items
        for (java.util.Map.Entry<Integer, com.agentgierka.mmo.inventory.domain.ItemStack> entry : domain.getAnchoredItems().entrySet()) {
            com.agentgierka.mmo.inventory.domain.ItemStack domainItem = entry.getValue();
            int gridIndex = entry.getKey();

            existingEntity.getItems().stream()
                .filter(entity -> entity.getId().equals(domainItem.getId()))
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
                            .id(domainItem.getId())
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
