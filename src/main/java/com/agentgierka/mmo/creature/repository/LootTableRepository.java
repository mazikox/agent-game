package com.agentgierka.mmo.creature.repository;

import com.agentgierka.mmo.creature.model.LootTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LootTableRepository extends JpaRepository<LootTable, UUID> {
    List<LootTable> findByCreatureTemplateId(UUID templateId);
    List<LootTable> findByLocationId(UUID locationId);
}
