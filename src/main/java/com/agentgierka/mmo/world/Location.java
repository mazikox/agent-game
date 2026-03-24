package com.agentgierka.mmo.world;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * Represents a distinct area in the game world such as a city, forest, or dungeon.
 */
@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private LocationType type;

    private Integer width;
    private Integer height;
}
