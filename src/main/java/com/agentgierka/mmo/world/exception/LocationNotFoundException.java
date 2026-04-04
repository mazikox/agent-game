package com.agentgierka.mmo.world.exception;

import com.agentgierka.mmo.exception.GameBaseException;

/**
 * Thrown when a location is not found in the persistence layer.
 */
public class LocationNotFoundException extends GameBaseException {
    public LocationNotFoundException(String id) {
        super("Location with ID " + id + " not found.", "LOCATION_NOT_FOUND");
    }
}
