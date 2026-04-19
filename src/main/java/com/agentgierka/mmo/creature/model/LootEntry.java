package com.agentgierka.mmo.creature.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Random;
import java.util.UUID;

@Entity
@Table(name = "loot_entries")
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class LootEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loot_table_id", nullable = false)
    private LootTable lootTable;

    @Column(nullable = false)
    private String itemName;

    private double dropChance;
    @Builder.Default
    private int minQuantity = 1;
    @Builder.Default
    private int maxQuantity = 1;
    @Builder.Default
    private int groupId = 0;

    public static LootEntry create(LootTable table, String item, double chance, int min, int max, int group) {
        return LootEntry.builder()
                .lootTable(table)
                .itemName(item)
                .dropChance(chance)
                .minQuantity(min)
                .maxQuantity(max)
                .groupId(group)
                .build();
    }

    public int rollQuantity(Random random) {
        if (maxQuantity <= minQuantity) {
            return minQuantity;
        }
        return random.nextInt(maxQuantity - minQuantity + 1) + minQuantity;
    }
}
