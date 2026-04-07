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
@ToString
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    @ToString.Exclude
    private String password;

    private Long gold;

    /** 
     * Master's charisma affects agent performance and loyalty bonuses.
     */
    private Integer charisma;

    @Builder.Default
    private Integer maxThinkingSteps = 1;

    public static Player create(String username, String encodedPassword) {
        return Player.builder()
                .username(username)
                .password(encodedPassword)
                .gold(100L)
                .charisma(10)
                .maxThinkingSteps(1)
                .build();
    }
}
