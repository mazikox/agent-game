package com.agentgierka.mmo.exception;

import com.agentgierka.mmo.agent.exception.AgentNotFoundException;
import com.agentgierka.mmo.agent.exception.AgentStateException;
import com.agentgierka.mmo.agent.exception.InvalidMovementException;
import com.agentgierka.mmo.combat.exception.CombatException;
import com.agentgierka.mmo.creature.exception.CreatureNotFoundException;
import com.agentgierka.mmo.world.exception.LocationNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import com.agentgierka.mmo.inventory.application.InventoryNotFoundException;
import com.agentgierka.mmo.inventory.exception.InventoryException;
import com.agentgierka.mmo.player.exception.PlayerAlreadyExistsException;

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

    @ExceptionHandler(PlayerAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handlePlayerAlreadyExists(PlayerAlreadyExistsException ex) {
        log.warn("Player already exists: {}", ex.getMessage());
        return buildResponse(ex.getErrorCode(), ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CombatException.class)
    public ResponseEntity<ErrorResponse> handleCombatException(CombatException ex) {
        log.warn("Combat rule violation: {}", ex.getMessage());
        return buildResponse(ex.getErrorCode(), ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CreatureNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCreatureNotFound(CreatureNotFoundException ex) {
        log.warn("Creature not found: {}", ex.getMessage());
        return buildResponse(ex.getErrorCode(), ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInventoryNotFound(InventoryNotFoundException ex) {
        log.warn("Inventory not found: {}", ex.getMessage());
        return buildResponse(ex.getErrorCode(), ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AgentStateException.class)
    public ResponseEntity<ErrorResponse> handleAgentStateConflict(AgentStateException ex) {
        log.warn("Agent state conflict: {}", ex.getMessage());
        return buildResponse(ex.getErrorCode(), ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InventoryException.class)
    public ResponseEntity<ErrorResponse> handleInventoryException(InventoryException ex) {
        log.warn("Inventory rule violation: {}", ex.getMessage());
        return buildResponse(ex.getErrorCode(), ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(GameBaseException.class)
    public ResponseEntity<ErrorResponse> handleGenericGameException(GameBaseException ex) {
        log.error("Internal game error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildResponse(ex.getErrorCode(), ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildResponse("FORBIDDEN", "Access denied.", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse(ex.getMessage());
        log.warn("Validation failed: {}", details);
        return buildResponse("VALIDATION_ERROR", details, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJson(HttpMessageNotReadableException ex) {
        log.warn("Invalid request format: {}", ex.getMessage());
        return buildResponse("INVALID_REQUEST_FORMAT", "Invalid request format or data values.", HttpStatus.BAD_REQUEST);
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
