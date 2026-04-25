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
    private volatile Map<String, ItemDefinition> cache = Map.of();

    @PostConstruct
    public void load() {
        Map<String, ItemDefinition> newCache = repo.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toUnmodifiableMap(ItemDefinition::id, d -> d));
        this.cache = newCache;
    }

    public ItemDefinition getById(String id) {
        ItemDefinition def = cache.get(id);
        if (def == null) {
            throw new ItemDefinitionNotFoundException(id);
        }
        return def;
    }

    public void reload() {
        load();
    }

    public static class ItemDefinitionNotFoundException extends RuntimeException {
        public ItemDefinitionNotFoundException(String id) {
            super("Item definition not found: " + id);
        }
    }
}
