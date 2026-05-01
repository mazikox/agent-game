package com.agentgierka.mmo.ai.behaviortree.condition;

import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Domain object tracking the progress of an agent's current goal.
 * Lives in memory and is reset when a new goal is assigned.
 */
@Getter
@ToString
public class GoalProgress {
    private final AtomicInteger killCount = new AtomicInteger(0);
    private final AtomicInteger expGained = new AtomicInteger(0);
    private final AtomicInteger itemsCollected = new AtomicInteger(0);
    private final AtomicInteger bossKills = new AtomicInteger(0);
    private final AtomicInteger eliteKills = new AtomicInteger(0);
    private final Set<String> locationsVisited = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void incrementKills() {
        killCount.incrementAndGet();
    }

    public void addExpGained(int amount) {
        expGained.addAndGet(amount);
    }

    public void addItemsCollected(int amount) {
        itemsCollected.addAndGet(amount);
    }

    public void incrementBossKills() {
        bossKills.incrementAndGet();
    }

    public void incrementEliteKills() {
        eliteKills.incrementAndGet();
    }

    public void addLocationVisited(String locationName) {
        if (locationName != null) {
            locationsVisited.add(locationName);
        }
    }

    public int getKillCount() { return killCount.get(); }
    public int getExpGained() { return expGained.get(); }
    public int getItemsCollected() { return itemsCollected.get(); }
    public int getBossKills() { return bossKills.get(); }
    public int getEliteKills() { return eliteKills.get(); }
    public int getLocationsVisitedCount() { return locationsVisited.size(); }
}
