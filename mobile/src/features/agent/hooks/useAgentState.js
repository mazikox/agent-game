import { useState, useEffect, useCallback } from 'react';
import { agentApi } from '../../../api/agentApi';
import { useSocket } from '../../../api/SocketContext';

/**
 * Custom hook to manage agent state, location sync, and console logs.
 * Encapsulates all WebSocket and API orchestration logic.
 */
export function useAgentState() {
  const { connected, connect, subscribeToAgent, subscribeToLocationCreatures } = useSocket();
  const [agent, setAgent] = useState(null);
  const [location, setLocation] = useState(null);
  const [creatures, setCreatures] = useState([]);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

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

    const subscription = subscribeToAgent(agent.id, (updatedState) => {
      setAgent(prev => {
        const nextLocId = updatedState.currentLocationId || prev?.currentLocationId;
        return {
          ...prev,
          x: updatedState.x,
          y: updatedState.y,
          status: updatedState.status,
          currentLocationId: nextLocId,
          currentActionDescription: updatedState.currentActionDescription
        };
      });

      if (updatedState.currentActionDescription) {
        addLog(updatedState.currentActionDescription);
      }
    });

    return () => subscription.unsubscribe();
  }, [agent?.id, connected, subscribeToAgent, addLog]);

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

  return {
    agent,
    location,
    logs,
    loading,
    error,
    connected,
    handleCommand,
    creatures
  };
}
