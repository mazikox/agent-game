package com.agentgierka.mmo.exception;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Base abstract exception for all game-related errors.
 */
@Getter
public abstract class GameBaseException extends RuntimeException {
    private final String errorCode;
    private final LocalDateTime timestamp;

    protected GameBaseException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
    }
}
