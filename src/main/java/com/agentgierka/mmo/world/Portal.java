package com.agentgierka.mmo.world;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * Represents a portal that teleports an agent from one location to another.
 */
@Entity
@Table(name = "portals")
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_location_id")
    private Location sourceLocation;

    private Integer sourceX;
    private Integer sourceY;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_location_id")
    private Location targetLocation;

    private Integer targetX;
    private Integer targetY;
}
