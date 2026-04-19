package com.agentgierka.mmo.creature.web.dto;

import com.agentgierka.mmo.creature.model.CreatureRank;
import com.agentgierka.mmo.creature.model.CreatureState;

import java.util.UUID;

/**
 * Data Transfer Object for Creature information in the REST/WebSocket API.
 */
public record CreatureDto(
        UUID instanceId,
        String name,
        CreatureRank rank,
        CreatureState state,
        int x,
        int y,
        int currentHp,
        int maxHp,
        int level
) {}
