package com.agentgierka.mmo.creature.repository;

import com.agentgierka.mmo.creature.model.CreatureTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CreatureTemplateRepository extends JpaRepository<CreatureTemplate, UUID> {
}
