package com.agentgierka.mmo.ai.integration;

import com.agentgierka.mmo.agent.model.Agent;
import com.agentgierka.mmo.agent.model.AgentStats;
import com.agentgierka.mmo.agent.repository.AgentRepository;
import com.agentgierka.mmo.ai.adapter.GoalConditionFactory;
import com.agentgierka.mmo.ai.behaviortree.BehaviorContext;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;
import com.agentgierka.mmo.ai.behaviortree.condition.GoalCondition;
import com.agentgierka.mmo.ai.behaviortree.condition.GoalProgress;
import com.agentgierka.mmo.ai.behaviortree.condition.GoalProgressRegistry;
import org.springframework.test.util.ReflectionTestUtils;
import com.agentgierka.mmo.ai.behaviortree.decorator.RepeatUntilNode;
import com.agentgierka.mmo.ai.behaviortree.leaf.IdleAction;
import com.agentgierka.mmo.ai.listener.GoalProgressListener;
import com.agentgierka.mmo.creature.event.CreatureKilledEvent;
import com.agentgierka.mmo.creature.model.CreatureRank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Goal System Integration Test")
class GoalSystemIntegrationTest {

    @Autowired
    private GoalProgressRegistry registry;

    @Autowired
    private GoalConditionFactory conditionFactory;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("Full Flow: Kill count goal should be satisfied after 2 kills")
    void testKillCountFlow() {
        Agent agent = Agent.create("TestAgent", null, null, 0, 0, 1);
        agent = agentRepository.save(agent);
        UUID agentId = agent.getId();
        
        // 1. Create a goal: Kill 2 mobs
        GoalCondition condition = conditionFactory.parse("killCount >= 2");
        RepeatUntilNode root = new RepeatUntilNode(condition, new IdleAction());
        
        // 2. Initialize progress
        GoalProgress progress = registry.getOrCreate(agentId);
        BehaviorContext context = new BehaviorContext(agent, null, null, null, eventPublisher, progress);

        // 3. First tick: should be RUNNING (0/2 kills)
        assertEquals(NodeStatus.RUNNING, root.tick(context));

        // 4. Simulate first kill via event
        eventPublisher.publishEvent(new CreatureKilledEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 
                agentId, List.of(), 100, CreatureRank.NORMAL));
        
        // 5. Second tick: should be RUNNING (1/2 kills)
        assertEquals(NodeStatus.RUNNING, root.tick(context));

        // 6. Simulate second kill
        eventPublisher.publishEvent(new CreatureKilledEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 
                agentId, List.of(), 100, CreatureRank.NORMAL));

        // 7. Third tick: should be SUCCESS (2/2 kills)
        assertEquals(NodeStatus.SUCCESS, root.tick(context));
        assertEquals(2, progress.getKillCount());
        
        registry.remove(agentId);
    }
}
