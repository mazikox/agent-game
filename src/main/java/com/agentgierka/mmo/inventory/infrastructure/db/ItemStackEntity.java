package com.agentgierka.mmo.inventory.infrastructure.db;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemStackEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(name = "inventory_id", nullable = false)
    private UUID inventoryId;

    @Column(name = "item_definition_id", nullable = false)
    private String itemDefinitionId;

    @Column(name = "grid_index", nullable = false)
    private int gridIndex;

    @Column(name = "quantity", nullable = false)
    private int quantity;

}
