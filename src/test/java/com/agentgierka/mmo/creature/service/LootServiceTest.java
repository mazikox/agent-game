package com.agentgierka.mmo.creature.service;

import com.agentgierka.mmo.creature.model.LootEntry;
import com.agentgierka.mmo.creature.model.LootTable;
import com.agentgierka.mmo.creature.repository.LootTableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LootServiceTest {

    @Mock
    private LootTableRepository lootTableRepository;

    @org.mockito.Spy
    private java.util.Random random = new java.util.Random();

    @InjectMocks
    private LootService lootService;

    @Test
    void shouldRollLootCorrectlyWithGroups() {
        UUID templateId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        LootTable table = LootTable.builder().entries(new ArrayList<>()).build();
        
        table.getEntries().add(LootEntry.builder().itemName("Item A").dropChance(1.0).groupId(1).lootTable(table).build());
        table.getEntries().add(LootEntry.builder().itemName("Item B").dropChance(1.0).groupId(1).lootTable(table).build());
        table.getEntries().add(LootEntry.builder().itemName("Common Item").dropChance(1.0).groupId(0).lootTable(table).build());

        when(lootTableRepository.findByCreatureTemplateId(templateId)).thenReturn(List.of(table));
        when(lootTableRepository.findByLocationId(locationId)).thenReturn(List.of());

        List<String> result = lootService.rollLoot(templateId, locationId);

        assertThat(result).hasSize(2);
        assertThat(result).contains("Common Item");
        assertThat(result).anyMatch(item -> item.equals("Item A") || item.equals("Item B"));
    }

    @Test
    void shouldMergeMobAndLocationLoot() {
        UUID templateId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        
        LootTable mobTable = LootTable.builder().entries(new ArrayList<>()).build();
        mobTable.getEntries().add(LootEntry.builder().itemName("Mob Item").dropChance(1.0).groupId(0).lootTable(mobTable).build());
        
        LootTable locTable = LootTable.builder().entries(new ArrayList<>()).build();
        locTable.getEntries().add(LootEntry.builder().itemName("Map Item").dropChance(1.0).groupId(0).lootTable(locTable).build());

        when(lootTableRepository.findByCreatureTemplateId(templateId)).thenReturn(List.of(mobTable));
        when(lootTableRepository.findByLocationId(locationId)).thenReturn(List.of(locTable));

        List<String> result = lootService.rollLoot(templateId, locationId);

        assertThat(result).containsExactlyInAnyOrder("Mob Item", "Map Item");
    }

    @Test
    void shouldHandleEmptyTablesGracefully() {
        UUID templateId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        when(lootTableRepository.findByCreatureTemplateId(templateId)).thenReturn(List.of());
        when(lootTableRepository.findByLocationId(locationId)).thenReturn(List.of());

        List<String> result = lootService.rollLoot(templateId, locationId);

        assertThat(result).isEmpty();
    }
}
