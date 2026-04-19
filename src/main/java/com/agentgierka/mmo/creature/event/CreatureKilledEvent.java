package com.agentgierka.mmo.creature.event;

import java.util.List;
import java.util.UUID;

public record CreatureKilledEvent(
    UUID instanceId,
    UUID locationId,
    UUID templateId,
    List<String> drops
) {}
