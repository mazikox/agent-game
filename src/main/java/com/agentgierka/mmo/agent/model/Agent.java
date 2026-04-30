package com.agentgierka.mmo.agent.model;

import com.agentgierka.mmo.ai.model.Perception;
import com.agentgierka.mmo.ai.model.Direction;
import com.agentgierka.mmo.player.Player;
import com.agentgierka.mmo.world.Location;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

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
    private Integer speed = 5;

    public void changeSpeed(Integer newSpeed) {
        this.speed = newSpeed;
    }

    @Enumerated(EnumType.STRING)
    private AgentStatus status;

    private String goal;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "agent_memory_log", joinColumns = @JoinColumn(name = "agent_id"))
    @Builder.Default
    private List<String> memoryLog = new ArrayList<>();

    private String currentTask;

    private String currentActionDescription;

    private LocalDateTime lastTeleportAt;

    @Column(name = "target_id")
    private UUID targetId;

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

    public void assignGoal(String newGoal) {
        if (this.memoryLog == null) {
            this.memoryLog = new ArrayList<>();
        }
        this.memoryLog.add(0, "[CEL] Gracz: " + newGoal);
        while (this.memoryLog.size() > 10) {
            this.memoryLog.remove(this.memoryLog.size() - 1);
        }
        this.goal = newGoal;
        this.currentTask = "Waiting for goal analysis...";
        this.targetX = null;
        this.targetY = null;
        this.status = AgentStatus.IDLE;
    }

    public void cancelCurrentGoal() {
        this.goal = null;
        this.targetX = null;
        this.targetY = null;
        this.status = AgentStatus.IDLE;
        this.currentActionDescription = "Goal cancelled by user.";
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
    }

    public void teleport(Location location, Integer x, Integer y) {
        this.currentLocation = location;
        this.x = x;
        this.y = y;
        this.targetX = null;
        this.targetY = null;
        this.status = AgentStatus.IDLE;
        this.currentActionDescription = "Teleported to " + location.getName();
        this.lastTeleportAt = LocalDateTime.now();
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

    public Perception preparePerception(List<String> nearbyObjects, List<String> visibleCreatures) {
        return Perception.builder()
                .name(this.name)
                .x(this.x)
                .y(this.y)
                .level(this.stats != null ? this.stats.getLevel() : 1)
                .mapWidth(currentLocation != null && currentLocation.getWidth() != null ? currentLocation.getWidth() : 0)
                .mapHeight(currentLocation != null && currentLocation.getHeight() != null ? currentLocation.getHeight() : 0)
                .locationName(currentLocation != null ? currentLocation.getName() : "Unknown")
                .locationDescription(currentLocation != null ? currentLocation.getDescription() : "")
                .currentGoal(this.goal)
                .memoryLog(new ArrayList<>(this.memoryLog))
                .lastActionDescription(this.currentActionDescription)
                .nearbyObjects(nearbyObjects)
                .visibleCreatures(visibleCreatures)
                .build();
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

    public void targetEntity(UUID entityId) {
        this.targetId = entityId;
    }

    public void clearTarget() {
        this.targetId = null;
    }

    public record Point(int x, int y) {}

    public Point calculateTarget(Direction direction, Integer steps) {
        int newX = this.x != null ? this.x : 0;
        int newY = this.y != null ? this.y : 0;
        int moveSteps = steps != null ? steps : 0;
        
        if (direction != null) {
            switch (direction) {
                case UP -> newY -= moveSteps;
                case DOWN -> newY += moveSteps;
                case LEFT -> newX -= moveSteps;
                case RIGHT -> newX += moveSteps;
            }
        }
        
        int maxWidth = currentLocation != null && currentLocation.getWidth() != null ? currentLocation.getWidth() : Integer.MAX_VALUE;
        int maxHeight = currentLocation != null && currentLocation.getHeight() != null ? currentLocation.getHeight() : Integer.MAX_VALUE;
        
        newX = Math.max(0, Math.min(newX, maxWidth));
        newY = Math.max(0, Math.min(newY, maxHeight));
        
        return new Point(newX, newY);
    }
}
