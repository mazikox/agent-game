package com.agentgierka.mmo.inventory.infrastructure.db;

import com.agentgierka.mmo.inventory.domain.Inventory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class InventoryRepository {

    private final EntityManager entityManager;
    private final ItemDefinitionDictionary dictionary;

    public Optional<Inventory> findByCharacterIdWithItems(UUID characterId) {
        return findEntityByCharacterId(characterId)
                .map(entity -> InventoryMapper.toDomain(entity, dictionary));
    }

    @Transactional
    public void save(Inventory inventory, UUID characterId) {
        InventoryEntity entity = findEntityByCharacterId(characterId)
                .orElseGet(() -> createNewEntity(inventory, characterId));

        InventoryMapper.toEntity(inventory, entity);
    }

    private InventoryEntity createNewEntity(Inventory inventory, UUID characterId) {
        InventoryEntity newEntity = InventoryEntity.builder()
                .characterId(characterId)
                .width(inventory.getWidth())
                .height(inventory.getHeight())
                .build();
        entityManager.persist(newEntity);
        return newEntity;
    }

    private Optional<InventoryEntity> findEntityByCharacterId(UUID characterId) {
        try {
            return Optional.of(entityManager.createQuery(
                            "SELECT i FROM InventoryEntity i LEFT JOIN FETCH i.items WHERE i.characterId = :characterId",
                            InventoryEntity.class)
                    .setParameter("characterId", characterId)
                    .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
