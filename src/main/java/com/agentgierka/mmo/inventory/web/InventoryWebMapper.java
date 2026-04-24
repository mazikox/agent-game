package com.agentgierka.mmo.inventory.web;

import com.agentgierka.mmo.inventory.domain.Inventory;
import com.agentgierka.mmo.inventory.domain.ItemStack;
import com.agentgierka.mmo.inventory.web.dto.InventoryResponse;
import com.agentgierka.mmo.inventory.web.dto.ItemStackResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface InventoryWebMapper {

    @Mapping(target = "items", source = "anchoredItems")
    InventoryResponse toResponse(Inventory inventory);

    default List<ItemStackResponse> mapAnchoredItems(Map<Integer, ItemStack> anchoredItems) {
        if (anchoredItems == null) return null;
        return anchoredItems.entrySet().stream()
            .map(entry -> toItemResponse(entry.getValue(), entry.getKey()))
            .collect(Collectors.toList());
    }

    @Mapping(target = "id", source = "item.id")
    @Mapping(target = "definitionId", source = "item.definition.id")
    @Mapping(target = "name", source = "item.definition.name")
    @Mapping(target = "width", source = "item.definition.width")
    @Mapping(target = "height", source = "item.definition.height")
    @Mapping(target = "stackable", source = "item.definition.stackable")
    @Mapping(target = "gridIndex", source = "gridIndex")
    @Mapping(target = "quantity", source = "item.quantity")
    ItemStackResponse toItemResponse(ItemStack item, Integer gridIndex);
}
