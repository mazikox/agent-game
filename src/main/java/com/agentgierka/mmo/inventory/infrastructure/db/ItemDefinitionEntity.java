package com.agentgierka.mmo.inventory.infrastructure.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "item_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemDefinitionEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(name = "max_stack", nullable = false)
    private int maxStack;

    @Column(name = "icon_url")
    private String iconUrl;
}
