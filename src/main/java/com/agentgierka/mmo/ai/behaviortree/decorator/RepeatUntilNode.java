package com.agentgierka.mmo.ai.behaviortree.decorator;

import com.agentgierka.mmo.ai.behaviortree.BehaviorContext;
import com.agentgierka.mmo.ai.behaviortree.BehaviorNode;
import com.agentgierka.mmo.ai.behaviortree.NodeStatus;
import com.agentgierka.mmo.ai.behaviortree.condition.GoalCondition;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RepeatUntilNode implements BehaviorNode {
    private final GoalCondition condition;
    private final BehaviorNode child;
    private int consecutiveFailures = 0;
    private static final int MAX_CONSECUTIVE_FAILURES = 5;

    public RepeatUntilNode(GoalCondition condition, BehaviorNode child) {
        this.condition = condition;
        this.child = child;
    }

    @Override
    public NodeStatus tick(BehaviorContext context) {
        if (condition.isSatisfied(context.goalProgress(), context.agent())) {
            log.info("Goal condition satisfied for agent {}. Terminating loop.", context.agent().getName());
            return NodeStatus.SUCCESS;
        }

        NodeStatus status = child.tick(context);

        if (status == NodeStatus.SUCCESS) {
            handleIterationSuccess(context.agent().getName());
            return NodeStatus.RUNNING;
        }

        if (status == NodeStatus.FAILURE) {
            return handleIterationFailure(context.agent().getName());
        }

        consecutiveFailures = 0;
        return NodeStatus.RUNNING;
    }

    private void handleIterationSuccess(String agentName) {
        log.info("Loop iteration completed for agent {}. Resetting child for next iteration.", agentName);
        consecutiveFailures = 0;
        child.reset();
    }

    private NodeStatus handleIterationFailure(String agentName) {
        consecutiveFailures++;
        log.warn("Child node failed for agent {} (Attempt {}/{}). Retrying...",
                agentName, consecutiveFailures, MAX_CONSECUTIVE_FAILURES);

        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            log.error("Max consecutive failures reached for agent {}. Aborting goal.", agentName);
            return NodeStatus.FAILURE;
        }

        child.reset();
        return NodeStatus.RUNNING;
    }

    @Override
    public void reset() {
        consecutiveFailures = 0;
        child.reset();
    }

    @Override
    public String describe() {
        return "RepeatUntil(" + child.describe() + ")";
    }
}
