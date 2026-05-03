package com.agentgierka.mmo.ai.behaviortree.condition;

import lombok.RequiredArgsConstructor;

import java.util.function.BiPredicate;

/**
 * Enumeration of supported comparison operators for goal conditions.
 */
@RequiredArgsConstructor
public enum ComparisonOperator {
    GREATER_THAN_OR_EQUAL(">=", (actual, expected) -> actual >= expected),
    LESS_THAN_OR_EQUAL("<=", (actual, expected) -> actual <= expected),
    EQUAL("==", (actual, expected) -> actual == expected),
    GREATER_THAN(">", (actual, expected) -> actual > expected),
    LESS_THAN("<", (actual, expected) -> actual < expected);

    private final String symbol;
    private final BiPredicate<Integer, Integer> predicate;

    public boolean compare(int actual, int expected) {
        return predicate.test(actual, expected);
    }

    public static ComparisonOperator fromSymbol(String symbol) {
        for (ComparisonOperator op : values()) {
            if (op.symbol.equals(symbol)) {
                return op;
            }
        }
        return GREATER_THAN_OR_EQUAL; // Default to >= if not found
    }
}
