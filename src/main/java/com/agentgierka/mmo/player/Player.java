package com.agentgierka.mmo.player;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * Represents a Master Player who controls multiple agents in the game world.
 */
@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    private Long gold;

    /** 
     * Master's charisma affects agent performance and loyalty bonuses.
     */
    private Integer charisma; 
}
