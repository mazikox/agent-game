package com.agentgierka.mmo.creature.adapter;

import com.agentgierka.mmo.creature.event.CreatureKilledEvent;
import com.agentgierka.mmo.creature.event.CreatureSpawnedEvent;
import com.agentgierka.mmo.creature.model.CreatureInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreatureWebSocketAdapterTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private CreatureWebSocketAdapter adapter;

    @Test
    void shouldBroadcastSpawnedCreature() {
        UUID locationId = UUID.randomUUID();
        CreatureInstance instance = CreatureInstance.builder().instanceId(UUID.randomUUID()).build();
        CreatureSpawnedEvent event = new CreatureSpawnedEvent(instance.getInstanceId(), locationId, instance);

        adapter.onCreatureSpawned(event);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/locations/" + locationId + "/creatures"),
                eq(instance)
        );
    }

    @Test
    void shouldBroadcastKilledCreature() {
        UUID locationId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        CreatureKilledEvent event = new CreatureKilledEvent(instanceId, locationId, templateId, java.util.List.of());

        adapter.onCreatureKilled(event);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/locations/" + locationId + "/creatures/killed"),
                eq(instanceId)
        );
    }
}
