package com.agentgierka.mmo.creature.event;

import com.agentgierka.mmo.creature.model.CreatureRank;
import java.util.List;
import java.util.UUID;

public record CreatureKilledEvent(
    UUID instanceId,
    UUID locationId,
    UUID templateId,
    UUID killerId,
    List<String> drops,
    int expReward,
    CreatureRank rank
) {}
