import { useState, useEffect, useCallback, useRef } from 'react';
import { agentApi } from '../../../api/agentApi';
import { combatApi } from '../../../api/combatApi';
import { useSocket } from '../../../api/SocketContext';
import { useInteractionPanel } from '../../world/hooks/useInteractionPanel';

/**
 * Custom hook to manage agent state, location sync, and console logs.
 * Encapsulates all WebSocket and API orchestration logic.
 */
export function useAgentState() {
  const { connected, connect, subscribeToSubTopic, subscribeToLocationCreatures, subscribeToCombatLogs } = useSocket();
  const [agent, setAgent] = useState(null);
  const [location, setLocation] = useState(null);
  const [creatures, setCreatures] = useState([]);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const versions = useRef({
    position: -1,
    status: -1,
    health: -1,
    target: -1
  });

  useEffect(() => {
    versions.current = {
      position: -1,
      status: -1,
      health: -1,
      target: -1
    };
  }, [agent?.id]);

  const {
    panel,
    loading: panelLoading,
    error: panelError,
    openPanel,
    closePanel,
    executeAction
  } = useInteractionPanel(agent?.id);

  // 1. Log Management
  const addLog = useCallback((message) => {
    const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    setLogs(prev => {
      // Prevent duplicate logs if identical to last entry
      if (prev.length > 0 && prev[prev.length - 1].message === message) return prev;
      return [...prev, { time, message }].slice(-50);
    });
  }, []);

  // 2. Initial Data Fetching
  const fetchInitialData = useCallback(async () => {
    try {
      const agents = await agentApi.getAllAgents();
      if (agents && agents.length > 0) {
        const detailedAgent = await agentApi.getAgentDetails(agents[0].id);
        setAgent(detailedAgent);
        
        if (detailedAgent.currentActionDescription) {
          addLog(detailedAgent.currentActionDescription);
        }

        const locDetails = await agentApi.getLocationDetails(detailedAgent.currentLocationId);
        setLocation(locDetails);
        
        try {
            const initialCreatures = await agentApi.getCreaturesInLocation(detailedAgent.currentLocationId);
            setCreatures(initialCreatures || []);
        } catch(cErr) {
            console.error("Failed to load initial creatures:", cErr);
        }
        
        setError(null);
      }
    } catch (err) {
      console.error("[useAgentState] API Error:", err);
      setError("Błąd połączenia z serwerem");
    } finally {
      setLoading(false);
    }
  }, [addLog]);

  // 3. Lifecycle: Initialization
  useEffect(() => {
    fetchInitialData();
    connect();
  }, [fetchInitialData, connect]);

  // 4. Lifecycle: Real-time Socket Subscriptions
  useEffect(() => {
    if (!agent?.id || !connected) return;

    const positionSub = subscribeToSubTopic(agent.id, 'position', (data) => {
      if (data.version <= versions.current.position) return;
      versions.current.position = data.version;

      setAgent(prev => {
        if (!prev) return prev;
        return {
          ...prev,
          x: data.x,
          y: data.y,
          currentLocationId: data.locationId || prev.currentLocationId
        };
      });
    });

    const statusSub = subscribeToSubTopic(agent.id, 'status', (data) => {
      if (data.version <= versions.current.status) return;
      versions.current.status = data.version;

      if (data.status === 'MOVING') {
        versions.current.position = 0;
      }

      setAgent(prev => {
        if (!prev) return prev;
        return {
          ...prev,
          status: data.status,
          currentActionDescription: data.actionDescription
        };
      });

      if (data.actionDescription) {
        addLog(data.actionDescription);
      }
    });

    const healthSub = subscribeToSubTopic(agent.id, 'health', (data) => {
      if (data.version <= versions.current.health) return;
      versions.current.health = data.version;

      setAgent(prev => {
        if (!prev) return prev;
        return {
          ...prev,
          hp: data.hp,
          maxHp: data.maxHp
        };
      });
    });

    const targetSub = subscribeToSubTopic(agent.id, 'target', (data) => {
      if (data.version <= versions.current.target) return;
      versions.current.target = data.version;

      setAgent(prev => {
        if (!prev) return prev;
        return {
          ...prev,
          targetId: data.targetId,
          targetName: data.targetName,
          targetHp: data.targetHp,
          targetMaxHp: data.targetMaxHp
        };
      });
    });

    const combatSubscription = subscribeToCombatLogs(agent.id, (log) => {
      addLog(log);
    });

    return () => {
      combatSubscription.unsubscribe();
      if (positionSub) positionSub.unsubscribe();
      if (statusSub) statusSub.unsubscribe();
      if (healthSub) healthSub.unsubscribe();
      if (targetSub) targetSub.unsubscribe();
    };
  }, [agent?.id, connected, subscribeToSubTopic, subscribeToCombatLogs, addLog]);

  // 5. Lifecycle: Map / Location details sync and Creatures
  useEffect(() => {
    if (!agent?.currentLocationId) return;

    let isCancelled = false;
    let locSubscription = null;
    const locId = agent.currentLocationId.toLowerCase();
    const currentLocId = location?.id?.toLowerCase();
    
    // Fetch if location changed or missing
    if (!location || currentLocId !== locId) {
      const updateLocation = async () => {
        try {
          const locDetails = await agentApi.getLocationDetails(locId);
          const initialCreatures = await agentApi.getCreaturesInLocation(locId);
          
          if (!isCancelled) {
            setLocation(locDetails);
            setCreatures(initialCreatures || []);
            addLog("Wkroczono do: " + locDetails.name);
          }
        } catch (err) {
          console.error("[useAgentState] Failed to sync location layout or creatures:", err);
        }
      };
      updateLocation();
    }

    // Subscribe to location creatures if connected
    if (connected) {
      locSubscription = subscribeToLocationCreatures(
        locId,
        (newCreature) => {
          setCreatures(prev => {
            // Unikanie duplikatów przy ponownym połączeniu
            if (prev.some(c => c.instanceId === newCreature.instanceId)) return prev;
            return [...prev, newCreature];
          });
        },
        (killedId) => {
          setCreatures(prev => prev.filter(c => c.instanceId !== killedId));
        }
      );
    }

    return () => { 
      isCancelled = true; 
      if (locSubscription) {
        locSubscription.unsubscribe();
      }
    };
  }, [agent?.currentLocationId, location?.id, connected, addLog, subscribeToLocationCreatures]);

  // 6. Actions
  const handleCommand = async (goal) => {
    if (agent) {
      addLog("> USER CMD: " + goal);
      try {
        await agentApi.sendGoal(agent.id, goal);
      } catch (err) {
        addLog("Error: Nie udało się wysłać celu.");
      }
    }
  };

  const attackNearest = async () => {
    if (!agent || !creatures) return;

    // Filter only living enemies and calculate distances
    const targets = creatures
      .filter(c => c.state === 'ALIVE' || c.currentHp > 0)
      .map(c => ({
        ...c,
        distance: Math.sqrt(Math.pow(c.x - agent.x, 2) + Math.pow(c.y - agent.y, 2))
      }))
      .sort((a, b) => a.distance - b.distance);

    if (targets.length === 0) {
      addLog("No living enemies nearby to attack.");
      return;
    }

    const target = targets[0];
    try {
      addLog(`Initiating combat with ${target.name} (dist: ${Math.round(target.distance)})...`);
      await combatApi.initiate(agent.id, target.instanceId);
    } catch (err) {
      addLog("Combat failed: " + (err.response?.data?.message || err.message));
    }
  };

  const performCombatAction = async (type) => {
    if (!agent) return;
    try {
      await combatApi.executeAction(agent.id, type);
    } catch (err) {
      addLog("Action failed: " + (err.response?.data?.message || err.message));
    }
  };

  const approachNearest = async () => {
    if (!agent || !creatures) return;

    const targets = creatures
      .filter(c => c.state === 'ALIVE' || c.currentHp > 0)
      .map(c => ({
        ...c,
        distance: Math.sqrt(Math.pow(c.x - agent.x, 2) + Math.pow(c.y - agent.y, 2))
      }))
      .sort((a, b) => a.distance - b.distance);

    if (targets.length === 0) {
      addLog("No living enemies nearby to approach.");
      return;
    }

    const target = targets[0];
    try {
      addLog(`Approaching ${target.name}...`);
      await agentApi.move(agent.id, target.x, target.y);
    } catch (err) {
      addLog("Approach failed: " + (err.response?.data?.message || err.message));
    }
  };

  const stopAgent = async () => {
    if (!agent) return;
    try {
      addLog("Stopping agent...");
      await agentApi.interrupt(agent.id);
    } catch (err) {
      addLog("Stop failed: " + (err.response?.data?.message || err.message));
    }
  };

  const handleCreatureClick = useCallback((creature) => {
    if (!agent?.id) return;
    openPanel(creature.instanceId, 'CREATURE', creature.name, creature.x, creature.y);
  }, [agent?.id, openPanel]);

  const handleActionPress = useCallback(async (action) => {
    if (!agent?.id) return;
    
    // Specjalna obsługa akcji, które wymagają dodatkowych parametrów (np. podejście)
    if (action.actionId === 'approach') {
      const target = creatures.find(c => c.instanceId === panel?.targetId);
      if (target) {
        try {
          addLog(`Approaching ${target.name}...`);
          await agentApi.move(agent.id, target.x, target.y);
          closePanel();
        } catch (err) {
          addLog("Approach failed: " + (err.response?.data?.message || err.message));
        }
      }
      return;
    }

    // Standardowe akcje przez endpoint z deskryptora
    try {
      if (action.actionId === 'attack') {
        addLog(`Initiating combat...`);
      }
      await executeAction(action, { agentId: agent.id, creatureId: panel?.targetId });
    } catch (err) {
      addLog("Action failed: " + (err.response?.data?.message || err.message));
    }
  }, [agent?.id, panel?.targetId, creatures, executeAction, closePanel, addLog]);

  return {
    agent,
    location,
    logs,
    loading,
    error,
    connected,
    handleCommand,
    attackNearest,
    approachNearest,
    stopAgent,
    performCombatAction,
    creatures: (creatures || []).filter(c => c.state === 'ALIVE' || c.state === 'IN_COMBAT'),
    panel,
    panelLoading,
    panelError,
    closePanel,
    handleCreatureClick,
    handleActionPress,
  };
}
