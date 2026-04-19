package com.agentgierka.mmo.creature.adapter;

import com.agentgierka.mmo.creature.event.CreatureKilledEvent;
import com.agentgierka.mmo.creature.event.CreatureSpawnedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreatureWebSocketAdapter {

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void onCreatureSpawned(CreatureSpawnedEvent event) {
        String destination = "/topic/locations/" + event.locationId() + "/creatures";
        log.debug("Broadcasting creature spawn: {} in location {}", event.instanceId(), event.locationId());
        messagingTemplate.convertAndSend(destination, event.instance());
    }

    @EventListener
    public void onCreatureKilled(CreatureKilledEvent event) {
        String destination = "/topic/locations/" + event.locationId() + "/creatures/killed";
        log.debug("Broadcasting creature death: {} in location {}", event.instanceId(), event.locationId());
        messagingTemplate.convertAndSend(destination, event.instanceId());
    }
}
