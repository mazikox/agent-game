package com.agentgierka.mmo.inventory.infrastructure.db;

import com.agentgierka.mmo.inventory.domain.ItemDefinition;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ItemDefinitionDictionary {

    private final ItemDefinitionRepository repo;
    private final ItemDefinitionMapper mapper;
    private Map<String, ItemDefinition> cache;

    @PostConstruct
    public void load() {
        this.cache = repo.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toUnmodifiableMap(ItemDefinition::id, d -> d));
    }

    public ItemDefinition getById(String id) {
        ItemDefinition def = cache.get(id);
        if (def == null) {
            throw new RuntimeException("Item definition not found: " + id);
        }
        return def;
    }

    public void reload() {
        load();
    }
}
