package com.agentgierka.mmo.engine;

import org.springframework.stereotype.Component;

/**
 * Lifecycle guard for the Game Engine.
 * 
 * Ensures the engine does not start processing ticks until 
 * initialization (e.g. Redis cleanup and state syncing) is fully complete.
 */
@Component
public class EngineControl {

    private volatile boolean ready = false;

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }
}
