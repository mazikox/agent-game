package com.agentgierka.mmo.ai.behaviortree.condition;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class GoalConditionTest {

    @Test
    @DisplayName("KillCountCondition should work correctly")
    void testKillCount() {
        GoalProgress progress = new GoalProgress();
        GoalCondition condition = new KillCountCondition(2, ComparisonOperator.GREATER_THAN_OR_EQUAL);

        assertFalse(condition.isSatisfied(progress, null));
        
        progress.incrementKills();
        assertFalse(condition.isSatisfied(progress, null));
        
        progress.incrementKills();
        assertTrue(condition.isSatisfied(progress, null));
    }

    @Test
    @DisplayName("LevelCondition should check agent level")
    void testLevel() {
        Agent agent = Mockito.mock(Agent.class);
        AgentStats stats = AgentStats.builder().level(1).build();
        when(agent.getStats()).thenReturn(stats);

        GoalCondition condition = new LevelCondition(2, ComparisonOperator.GREATER_THAN_OR_EQUAL);
        assertFalse(condition.isSatisfied(null, agent));

        stats = stats.toBuilder().level(2).build();
        when(agent.getStats()).thenReturn(stats);
        assertTrue(condition.isSatisfied(null, agent));
    }

    @Test
    @DisplayName("HpPercentCondition should check health threshold")
    void testHpPercent() {
        Agent agent = Mockito.mock(Agent.class);
        AgentStats stats = AgentStats.builder().hp(100).maxHp(100).build();
        when(agent.getStats()).thenReturn(stats);

        GoalCondition condition = new HpPercentCondition(30, ComparisonOperator.LESS_THAN_OR_EQUAL);
        
        // 100/100 = 100% ( > 30%)
        assertFalse(condition.isSatisfied(null, agent));

        // 30/100 = 30% ( <= 30%)
        stats = stats.toBuilder().hp(30).build();
        when(agent.getStats()).thenReturn(stats);
        assertTrue(condition.isSatisfied(null, agent));
    }

    @Test
    @DisplayName("Should support compound AND condition")
    void testAndCondition() {
        GoalProgress progress = new GoalProgress();
        Agent agent = Mockito.mock(Agent.class);
        AgentStats stats = AgentStats.builder().level(1).build();
        when(agent.getStats()).thenReturn(stats);

        GoalCondition kills = new KillCountCondition(1, ComparisonOperator.GREATER_THAN_OR_EQUAL);
        GoalCondition level = new LevelCondition(2, ComparisonOperator.GREATER_THAN_OR_EQUAL);
        GoalCondition compound = kills.and(level);

        // Kills: 0, Lvl: 1 -> False
        assertFalse(compound.isSatisfied(progress, agent));

        // Kills: 1, Lvl: 1 -> False
        progress.incrementKills();
        assertFalse(compound.isSatisfied(progress, agent));

        // Kills: 1, Lvl: 2 -> True
        stats = stats.toBuilder().level(2).build();
        when(agent.getStats()).thenReturn(stats);
        assertTrue(compound.isSatisfied(progress, agent));
    }
}
