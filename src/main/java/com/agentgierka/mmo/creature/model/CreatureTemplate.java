package com.agentgierka.mmo.creature.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "creature_templates")
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CreatureTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreatureRank rank = CreatureRank.NORMAL;

    private int level;
    private int baseHp;
    private int baseDamage;
    private int experienceReward;

    private int aggroRadius;

    private String iconUrl;

    @OneToMany(mappedBy = "creatureTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LootTable> lootTables;

    public static CreatureTemplate create(String name, CreatureRank rank, int level, int baseHp, int baseDamage, int expReward, int aggroRadius) {
        return CreatureTemplate.builder()
                .name(name)
                .rank(rank)
                .level(level)
                .baseHp(baseHp)
                .baseDamage(baseDamage)
                .experienceReward(expReward)
                .aggroRadius(aggroRadius)
                .build();
    }

    public static CreatureTemplate create(String name, CreatureRank rank, int level, int baseHp, int baseDamage, int expReward, int aggroRadius, String iconUrl) {
        return CreatureTemplate.builder()
                .name(name)
                .rank(rank)
                .level(level)
                .baseHp(baseHp)
                .baseDamage(baseDamage)
                .experienceReward(expReward)
                .aggroRadius(aggroRadius)
                .iconUrl(iconUrl)
                .build();
    }

    public int getScaledHp() {
        return (int) (baseHp * getRankMultiplier() + (level * 20));
    }

    public int getScaledDamage() {
        return (int) (baseDamage * getRankMultiplier() + (level * 2));
    }

    public int getScaledExperience() {
        return (int) (experienceReward * getRankMultiplier() + (level * 5));
    }

    private double getRankMultiplier() {
        return switch (rank) {
            case ELITE -> 2.0;
            case RARE -> 1.5;
            case BOSS -> 5.0;
            default -> 1.0;
        };
    }
}
