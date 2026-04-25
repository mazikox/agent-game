package com.agentgierka.mmo.inventory.web;

import com.agentgierka.mmo.inventory.application.InventoryApplicationService;
import com.agentgierka.mmo.inventory.domain.Inventory;
import com.agentgierka.mmo.inventory.web.dto.InventoryResponse;
import com.agentgierka.mmo.inventory.web.dto.MoveItemRequest;
import com.agentgierka.mmo.inventory.web.dto.RemoveItemRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.ErrorResponseException;
import com.agentgierka.mmo.inventory.domain.InventoryResult;
import com.agentgierka.mmo.security.PlayerPrincipal;
import com.agentgierka.mmo.inventory.application.InventoryOperationResult;

@RestController
@RequestMapping("/api/v1/characters/{characterId}/inventory")
@RequiredArgsConstructor
@PreAuthorize("@agentSecurity.isOwner(#characterId, principal.username)")
public class InventoryController {

    private final InventoryApplicationService inventoryService;
    private final InventoryWebMapper mapper;

    @GetMapping
    public InventoryResponse getInventory(
        @PathVariable UUID characterId,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        Inventory inventory = inventoryService.getInventory(characterId);
        return mapper.toResponse(inventory);
    }

    @PostMapping("/move")
    public InventoryResponse moveItem(
        @PathVariable UUID characterId,
        @AuthenticationPrincipal PlayerPrincipal principal,
        @RequestBody MoveItemRequest request
    ) {
        InventoryOperationResult opResult = inventoryService.moveItem(characterId, request.fromIndex(), request.toIndex());
        return handleResult(opResult);
    }

    @PostMapping("/remove")
    public InventoryResponse removeItem(
        @PathVariable UUID characterId,
        @AuthenticationPrincipal PlayerPrincipal principal,
        @RequestBody RemoveItemRequest request
    ) {
        InventoryOperationResult opResult = inventoryService.removeItem(characterId, request.index(), request.itemId());
        return handleResult(opResult);
    }



    private InventoryResponse handleResult(InventoryOperationResult opResult) {
        InventoryResult result = opResult.result();
        if (result instanceof InventoryResult.Success) {
            return mapper.toResponse(opResult.inventory());
        }

        throw switch (result) {
            case InventoryResult.Collision c -> 
                new ErrorResponseException(HttpStatus.CONFLICT, createProblemDetail(HttpStatus.CONFLICT, "Collision with slots: " + c.collidingSlots()), null);
            case InventoryResult.OutOfBounds o -> 
                new ErrorResponseException(HttpStatus.UNPROCESSABLE_CONTENT, createProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, "Item out of bounds"), null);
            case InventoryResult.NoSpace _ -> 
                new ErrorResponseException(HttpStatus.UNPROCESSABLE_CONTENT, createProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, "No space in inventory"), null);
            case InventoryResult.EmptySlot _ -> 
                new ErrorResponseException(HttpStatus.UNPROCESSABLE_CONTENT, createProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, "Slot is empty or ID mismatch"), null);
            default -> new ErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR, createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected inventory result"), null);
        };
    }

    private ProblemDetail createProblemDetail(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle("Inventory Error");
        return pd;
    }
}
