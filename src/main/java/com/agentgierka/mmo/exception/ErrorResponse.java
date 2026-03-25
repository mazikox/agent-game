package com.agentgierka.mmo.exception;

import java.time.LocalDateTime;

/**
 * Standardized error response DTO.
 */
public record ErrorResponse(
    String errorCode,
    String message,
    LocalDateTime timestamp
) {}
