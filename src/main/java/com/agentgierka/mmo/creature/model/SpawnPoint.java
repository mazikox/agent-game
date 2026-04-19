package com.agentgierka.mmo.creature.model;

import com.agentgierka.mmo.world.Location;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "spawn_points")
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SpawnPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creature_template_id", nullable = false)
    private CreatureTemplate creatureTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    private int centerX;
    private int centerY;

    private int wanderRadius;

    private int respawnSeconds;

    @Builder.Default
    private boolean active = true;

    public static SpawnPoint create(CreatureTemplate template, Location location, int cx, int cy, int radius, int respawn) {
        return SpawnPoint.builder()
                .creatureTemplate(template)
                .location(location)
                .centerX(cx)
                .centerY(cy)
                .wanderRadius(radius)
                .respawnSeconds(respawn)
                .build();
    }
}
