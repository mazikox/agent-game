package com.agentgierka.mmo.ai.adapter;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStats;
import com.agentgierka.mmo.ai.behaviortree.condition.*;
import com.agentgierka.mmo.world.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class GoalConditionFactoryTest {

    private final GoalConditionFactory factory = new GoalConditionFactory();

    @Test
    @DisplayName("Should parse simple numeric conditions")
    void shouldParseSimpleNumeric() {
        GoalCondition killCond = factory.parse("killCount >= 5");
        assertInstanceOf(KillCountCondition.class, killCond);

        GoalCondition lvlCond = factory.parse("level >= 10");
        assertInstanceOf(LevelCondition.class, lvlCond);
        
        GoalCondition hpCond = factory.parse("hpPercent <= 30");
        assertInstanceOf(HpPercentCondition.class, hpCond);
    }

    @Test
    @DisplayName("Should parse location condition")
    void shouldParseLocation() {
        GoalCondition locCond = factory.parse("locationReached == 'Forest of Beginnings'");
        assertInstanceOf(LocationReachedCondition.class, locCond);

        Agent agent = Mockito.mock(Agent.class);
        Location loc = Location.builder().name("Forest of Beginnings").build();
        when(agent.getCurrentLocation()).thenReturn(loc);

        assertTrue(locCond.isSatisfied(null, agent));
    }

    @Test
    @DisplayName("Should parse compound AND condition")
    void shouldParseAnd() {
        GoalCondition compound = factory.parse("AND(killCount >= 5, level >= 3)");
        assertNotNull(compound);

        GoalProgress progress = new GoalProgress();
        Agent agent = Mockito.mock(Agent.class);
        when(agent.getStats()).thenReturn(AgentStats.builder().level(1).build());

        // Not satisfied (0 kills, lvl 1)
        assertFalse(compound.isSatisfied(progress, agent));

        // Still not satisfied (5 kills, lvl 1)
        for(int i=0; i<5; i++) progress.incrementKills();
        assertFalse(compound.isSatisfied(progress, agent));

        // Satisfied (5 kills, lvl 3)
        when(agent.getStats()).thenReturn(AgentStats.builder().level(3).build());
        assertTrue(compound.isSatisfied(progress, agent));
    }

    @Test
    @DisplayName("Should return AlwaysFalseCondition for unknown metrics")
    void shouldHandleUnknown() {
        GoalCondition cond = factory.parse("bananaCount >= 42");
        assertInstanceOf(AlwaysFalseCondition.class, cond);
    }
}
