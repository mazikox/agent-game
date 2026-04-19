package com.agentgierka.mmo.creature.event;

import com.agentgierka.mmo.creature.model.CreatureInstance;
import java.util.UUID;

public record CreatureSpawnedEvent(
    UUID instanceId,
    UUID locationId,
    CreatureInstance instance
) {}
