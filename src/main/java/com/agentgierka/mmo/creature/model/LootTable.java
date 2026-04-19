package com.agentgierka.mmo.creature.model;

import com.agentgierka.mmo.world.Location;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "loot_tables")
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class LootTable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creature_template_id")
    private CreatureTemplate creatureTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @OneToMany(mappedBy = "lootTable", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LootEntry> entries;

    public void addEntry(LootEntry entry) {
        if (entries == null) {
            entries = new java.util.ArrayList<>();
        }
        entries.add(entry);
    }

    public static LootTable forCreature(CreatureTemplate template, String name) {
        return LootTable.builder()
                .creatureTemplate(template)
                .name(name)
                .build();
    }

    public static LootTable forLocation(Location location, String name) {
        return LootTable.builder()
                .location(location)
                .name(name)
                .build();
    }
}
