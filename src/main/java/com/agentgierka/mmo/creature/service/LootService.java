package com.agentgierka.mmo.creature.service;

import com.agentgierka.mmo.creature.model.LootEntry;
import com.agentgierka.mmo.creature.model.LootTable;
import com.agentgierka.mmo.creature.repository.LootTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LootService {

    private final LootTableRepository lootTableRepository;
    private final Random random = new Random();

    @Transactional(readOnly = true)
    public List<String> rollLoot(UUID templateId, UUID locationId) {
        // TODO: In the future, return List<Item> or similar instead of String
        List<LootTable> creatureTables = lootTableRepository.findByCreatureTemplateId(templateId);
        List<LootTable> locationTables = lootTableRepository.findByLocationId(locationId);

        List<LootTable> allTables = new ArrayList<>();
        allTables.addAll(creatureTables);
        allTables.addAll(locationTables);

        List<String> droppedItems = new ArrayList<>();

        for (LootTable table : allTables) {
            droppedItems.addAll(processLootTable(table));
        }

        return droppedItems;
    }

    private List<String> processLootTable(LootTable table) {
        Map<Integer, List<LootEntry>> groupedEntries = table.getEntries().stream()
                .collect(Collectors.groupingBy(LootEntry::getGroupId));

        List<String> tableDrops = new ArrayList<>();

        groupedEntries.forEach((groupId, entries) -> {
            if (groupId == 0) {
                for (LootEntry entry : entries) {
                    if (shouldDrop(entry.getDropChance())) {
                        int quantity = entry.rollQuantity(random);
                        for (int i = 0; i < quantity; i++) {
                            tableDrops.add(entry.getItemName());
                        }
                    }
                }
            } else {
                Collections.shuffle(entries);
                for (LootEntry entry : entries) {
                    if (shouldDrop(entry.getDropChance())) {
                        int quantity = entry.rollQuantity(random);
                        for (int i = 0; i < quantity; i++) {
                            tableDrops.add(entry.getItemName());
                        }
                        break;
                    }
                }
            }
        });

        return tableDrops;
    }

    private boolean shouldDrop(double chance) {
        return random.nextDouble() < chance;
    }
}
