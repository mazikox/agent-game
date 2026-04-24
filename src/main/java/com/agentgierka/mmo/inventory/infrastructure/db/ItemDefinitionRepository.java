package com.agentgierka.mmo.inventory.infrastructure.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemDefinitionRepository extends JpaRepository<ItemDefinitionEntity, String> {
}
