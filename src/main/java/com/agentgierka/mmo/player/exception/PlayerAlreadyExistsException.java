package com.agentgierka.mmo.player.exception;

import com.agentgierka.mmo.exception.GameBaseException;

public class PlayerAlreadyExistsException extends GameBaseException {
    public PlayerAlreadyExistsException(String username) {
        super("Player with username '" + username + "' already exists.", "PLAYER_ALREADY_EXISTS");
    }
}
