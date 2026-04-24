package com.agentgierka.mmo.inventory.web;

import com.agentgierka.mmo.inventory.application.InventoryApplicationService;
import com.agentgierka.mmo.inventory.domain.Inventory;
import com.agentgierka.mmo.inventory.web.dto.InventoryResponse;
import com.agentgierka.mmo.inventory.web.dto.MoveItemRequest;
import com.agentgierka.mmo.inventory.web.dto.RemoveItemRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/characters/{characterId}/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryApplicationService inventoryService;
    private final InventoryWebMapper mapper;

    @GetMapping
    public InventoryResponse getInventory(@PathVariable UUID characterId) {
        Inventory inventory = inventoryService.getInventory(characterId);
        return mapper.toResponse(inventory);
    }

    @PostMapping("/move")
    public InventoryResponse moveItem(
        @PathVariable UUID characterId,
        @RequestBody MoveItemRequest request
    ) {
        inventoryService.moveItem(characterId, request.fromIndex(), request.toIndex());
        return getInventory(characterId);
    }

    @PostMapping("/remove")
    public InventoryResponse removeItem(
        @PathVariable UUID characterId,
        @RequestBody RemoveItemRequest request
    ) {
        inventoryService.removeItem(characterId, request.index(), request.itemId());
        return getInventory(characterId);
    }
}
