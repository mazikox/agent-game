package com.agentgierka.mmo.creature.adapter;

import com.agentgierka.mmo.creature.event.CreatureKilledEvent;
import com.agentgierka.mmo.creature.event.CreatureSpawnedEvent;
import com.agentgierka.mmo.creature.model.CreatureInstance;
import com.agentgierka.mmo.creature.web.dto.CreatureDto;
import com.agentgierka.mmo.creature.web.mapper.CreatureMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreatureWebSocketAdapterTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private CreatureMapper creatureMapper;

    @InjectMocks
    private CreatureWebSocketAdapter adapter;

    @Test
    void shouldBroadcastSpawnedCreature() {
        UUID locationId = UUID.randomUUID();
        CreatureInstance instance = CreatureInstance.builder().instanceId(UUID.randomUUID()).build();
        CreatureSpawnedEvent event = new CreatureSpawnedEvent(instance.getInstanceId(), locationId, instance);

        CreatureDto dto = new CreatureDto(instance.getInstanceId(), "Test", null, null, 0, 0, 0, 0, 1);
        when(creatureMapper.toDto(instance)).thenReturn(dto);

        adapter.onCreatureSpawned(event);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/locations/" + locationId + "/creatures"),
                eq(dto)
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
