package com.agentgierka.mmo.ai.adapter;

import com.agentgierka.mmo.ai.behaviortree.condition.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Factory responsible for parsing condition strings from AI into GoalCondition objects.
 */
@Component
@Slf4j
public class GoalConditionFactory {

    private static final Pattern SIMPLE_CONDITION_PATTERN = Pattern.compile("(\\w+)\\s*(>=|<=|==|>|<)\\s*[\"']?([^\"']+)[\"']?");

    public GoalCondition parse(String conditionString) {
        if (conditionString == null || conditionString.isBlank()) {
            return new AlwaysFalseCondition();
        }

        String trimmed = conditionString.trim();

        // Handle compound AND/OR (simplified for now, can be expanded)
        if (trimmed.startsWith("AND(") && trimmed.endsWith(")")) {
            return parseCompound(trimmed.substring(4, trimmed.length() - 1), true);
        }
        if (trimmed.startsWith("OR(") && trimmed.endsWith(")")) {
            return parseCompound(trimmed.substring(3, trimmed.length() - 1), false);
        }

        return parseSingle(trimmed);
    }

    private GoalCondition parseSingle(String cond) {
        Matcher matcher = SIMPLE_CONDITION_PATTERN.matcher(cond);
        if (!matcher.matches()) {
            log.warn("Unknown condition format: '{}'. Falling back to AlwaysFalseCondition.", cond);
            return new AlwaysFalseCondition();
        }

        String metric = matcher.group(1).toLowerCase();
        String opSymbol = matcher.group(2);
        ComparisonOperator operator = ComparisonOperator.fromSymbol(opSymbol);
        String valueStr = matcher.group(3);

        return switch (metric) {
            case "killcount", "mobcount" -> new KillCountCondition(Integer.parseInt(valueStr), operator);
            case "level" -> new LevelCondition(Integer.parseInt(valueStr), operator);
            case "hppercent" -> new HpPercentCondition(Integer.parseInt(valueStr), operator);
            case "expgained" -> new ExpGainedCondition(Integer.parseInt(valueStr), operator);
            case "locationreached" -> new LocationReachedCondition(valueStr);
            default -> {
                log.warn("Unsupported metric in condition: '{}'", metric);
                yield new AlwaysFalseCondition();
            }
        };
    }

    private GoalCondition parseCompound(String inner, boolean isAnd) {
        String[] parts = inner.split(",\\s*(?![^()]*\\))"); // Split by comma not inside parentheses
        GoalCondition result = null;

        for (String part : parts) {
            GoalCondition current = parse(part);
            if (result == null) {
                result = current;
            } else {
                result = isAnd ? result.and(current) : result.or(current);
            }
        }

        return result != null ? result : new AlwaysFalseCondition();
    }
}
