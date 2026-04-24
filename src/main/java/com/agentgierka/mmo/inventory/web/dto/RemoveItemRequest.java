package com.agentgierka.mmo.inventory.web.dto;

import java.util.UUID;

public record RemoveItemRequest(int index, UUID itemId) {}
