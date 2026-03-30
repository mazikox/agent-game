package com.agentgierka.mmo.ai.port;

import com.agentgierka.mmo.ai.model.Perception;
import com.agentgierka.mmo.ai.model.Thought;

public interface Brain {
    Thought think(Perception perception);
}
