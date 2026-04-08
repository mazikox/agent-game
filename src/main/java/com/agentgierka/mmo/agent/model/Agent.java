package com.agentgierka.mmo.agent.model;

import com.agentgierka.mmo.ai.model.Perception;
import com.agentgierka.mmo.ai.model.Thought;
import com.agentgierka.mmo.player.Player;
import com.agentgierka.mmo.world.Location;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "agents")
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location currentLocation;

    private Integer x;
    private Integer y;


    @Embedded
    @Builder.Default
    private AgentStats stats = AgentStats.createInitial();

    private Integer targetX;
    private Integer targetY;

    @Builder.Default
    private Integer speed = 1;

    @Enumerated(EnumType.STRING)
    private AgentStatus status;

    private String goal;

    private Integer remainingThinkingSteps;

    private String currentTask;

    private String currentActionDescription;

    /**
     * Official factory method for creating new agents with guaranteed valid state.
     */
    public static Agent create(String name, Player owner, Location location, Integer x, Integer y, Integer speed) {
        return Agent.builder()
                .name(name)
                .owner(owner)
                .currentLocation(location)
                .x(x).y(y)
                .speed(speed != null ? speed : 1)
                .status(AgentStatus.IDLE)
                .stats(AgentStats.createInitial())
                .currentActionDescription("Agent initialized: " + name)
                .build();
    }

    public void assignGoal(String newGoal, int quota) {
        this.goal = newGoal;
        this.remainingThinkingSteps = quota;
        this.currentTask = "Waiting for goal analysis...";
        this.status = AgentStatus.IDLE;
    }

    public void cancelCurrentGoal() {
        this.goal = null;
        this.targetX = null;
        this.targetY = null;
        this.status = AgentStatus.IDLE;
        this.currentActionDescription = "Goal cancelled by user.";
        this.remainingThinkingSteps = 0;
    }

    public boolean hasActiveGoal() {
        return goal != null && !goal.isBlank();
    }

    public void clearGoal() {
        this.goal = null;
        this.currentTask = null;
        this.currentActionDescription = "Goal completed.";
    }

    public void startMovement(Integer targetX, Integer targetY, String description) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.status = AgentStatus.MOVING;
        this.currentActionDescription = description;
    }

    public void completeMovement(Integer x, Integer y) {
        this.x = x;
        this.y = y;
        this.targetX = null;
        this.targetY = null;
        this.status = AgentStatus.IDLE;
        this.currentActionDescription = "Arrived at destination (" + x + ", " + y + ")";
        
        if (remainingThinkingSteps != null && remainingThinkingSteps <= 0) {
            this.goal = null;
        }
    }

    public void teleport(Location location, Integer x, Integer y) {
        this.currentLocation = location;
        this.x = x;
        this.y = y;
        this.targetX = null;
        this.targetY = null;
        this.status = AgentStatus.IDLE;
        this.currentActionDescription = "Teleported to " + location.getName();
    }

    public void updateStatus(AgentStatus newStatus, String description) {
        this.status = newStatus;
        this.currentActionDescription = description;
    }

    public void syncWithWorldState(AgentWorldState worldState) {
        this.x = worldState.getX();
        this.y = worldState.getY();
        if (worldState.getStatus() != null) {
            this.status = worldState.getStatus();
        }
    }

    public Perception preparePerception(List<String> nearbyObjects) {
        return Perception.builder()
                .name(this.name)
                .x(this.x)
                .y(this.y)
                .mapWidth(currentLocation != null && currentLocation.getWidth() != null ? currentLocation.getWidth() : 0)
                .mapHeight(currentLocation != null && currentLocation.getHeight() != null ? currentLocation.getHeight() : 0)
                .locationName(currentLocation != null ? currentLocation.getName() : "Unknown")
                .locationDescription(currentLocation != null ? currentLocation.getDescription() : "")
                .currentGoal(this.goal)
                .lastActionDescription(this.currentActionDescription)
                .nearbyObjects(nearbyObjects)
                .build();
    }

    public void applyThought(Thought thought) {
        this.currentActionDescription = thought.actionSummary();
        this.currentTask = thought.nextGoal();
        this.targetX = thought.targetX();
        this.targetY = thought.targetY();

        if (thought.status() != null) {
            try {
                this.status = AgentStatus.valueOf(thought.status().toUpperCase());
            } catch (IllegalArgumentException e) {
                this.status = AgentStatus.IDLE;
            }
        }

        // Domain rule: If coordinates were changed by AI, forcefully set MOVING status
        // to engage Game Engine ticks.
        if (this.targetX != null && this.targetY != null &&
                (!this.targetX.equals(this.x) || !this.targetY.equals(this.y))) {
            this.status = AgentStatus.MOVING;
        }

        consumeThinkingStep();
    }

    private void consumeThinkingStep() {
        if (remainingThinkingSteps != null) {
            remainingThinkingSteps--;
            if (remainingThinkingSteps <= 0 && status == AgentStatus.IDLE) {
                this.goal = null;
                this.currentActionDescription = "Task finished (Manual mode).";
            }
        }
    }

    // --- Rich Domain Methods for RPG ---

    public void takeDamage(int amount) {
        this.stats = this.stats.takeDamage(amount);
        if (!this.stats.isAlive()) {
            this.currentActionDescription = "Agent has fallen in battle.";
            // Future: set status to DEAD or similar
        }
    }

    public void heal(int amount) {
        this.stats = this.stats.heal(amount);
    }

    public void gainExperience(int amount) {
        AgentStats oldStats = this.stats;
        this.stats = this.stats.addExperience(amount);
        
        if (this.stats.getLevel() > oldStats.getLevel()) {
            this.currentActionDescription = "LEVEL UP! Reached level " + this.stats.getLevel();
        }
    }
}
