package com.agentgierka.mmo.inventory.web.dto;

import java.util.List;

public record InventoryResponse(
    int width,
    int height,
    List<ItemStackResponse> items
) {}
