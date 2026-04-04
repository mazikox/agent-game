package com.agentgierka.mmo.exception;

import com.agentgierka.mmo.agent.exception.AgentNotFoundException;
import com.agentgierka.mmo.agent.exception.InvalidMovementException;
import com.agentgierka.mmo.world.exception.LocationNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Centrally handles all exceptions thrown by the application.
 * Maps custom GameBaseExceptions to standardized JSON responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AgentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAgentNotFound(AgentNotFoundException ex) {
        log.warn("Agent not found: {}", ex.getMessage());
        return buildResponse(ex.getErrorCode(), ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(LocationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLocationNotFound(LocationNotFoundException ex) {
        log.warn("Location not found: {}", ex.getMessage());
        return buildResponse(ex.getErrorCode(), ex.getMessage(), HttpStatus.NOT_FOUND);
    }


    @ExceptionHandler(InvalidMovementException.class)
    public ResponseEntity<ErrorResponse> handleInvalidMovement(InvalidMovementException ex) {
        log.warn("Invalid movement request: {}", ex.getMessage());
        return buildResponse(ex.getErrorCode(), ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(GameBaseException.class)
    public ResponseEntity<ErrorResponse> handleGenericGameException(GameBaseException ex) {
        log.error("Internal game error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildResponse(ex.getErrorCode(), ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        log.error("Unexpected system error: ", ex);
        return buildResponse("INTERNAL_SERVER_ERROR", "An unexpected error occurred.", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> buildResponse(String code, String message, HttpStatus status) {
        ErrorResponse response = new ErrorResponse(code, message, LocalDateTime.now());
        return new ResponseEntity<>(response, status);
    }
}
