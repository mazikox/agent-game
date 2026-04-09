import React, { useState, useEffect, useCallback } from 'react';
import { StyleSheet, View, SafeAreaView, StatusBar, Text, ActivityIndicator } from 'react-native';
import { theme } from './src/theme/theme';
import { AgentProfile } from './src/features/agent/AgentProfile';
import { MapView } from './src/features/world/MapView';
import { AgentConsole } from './src/features/hud/AgentConsole';
import { TopBar } from './src/features/hud/TopBar';
import { SideMenu } from './src/features/hud/SideMenu';
import { agentApi } from './src/api/agentApi';
import { LoginScreen } from './src/features/auth/LoginScreen';
import { SocketProvider, useSocket } from './src/api/SocketContext';

/**
 * Main Content component that uses the SocketContext.
 */
function GameContent({ handleLogout }) {
  const { connected, connect, subscribeToAgent } = useSocket();
  const [agent, setAgent] = useState(null);
  const [location, setLocation] = useState(null);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const addLog = useCallback((message) => {
    const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    setLogs(prev => {
      if (prev.length > 0 && prev[prev.length - 1].message === message) return prev;
      return [...prev, { time, message }].slice(-50);
    });
  }, []);

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
        setError(null);
      }
    } catch (err) {
      console.error("API Error:", err);
      setError("Błąd połączenia");
    } finally {
      setLoading(false);
    }
  }, [addLog]);

  // Initial data fetch and socket connection
  useEffect(() => {
    fetchInitialData();
    connect();
  }, [fetchInitialData, connect]);

  // WebSocket Subscription
  useEffect(() => {
    if (!agent?.id || !connected) return;

    console.log("Setting up WebSocket subscription for agent:", agent.id);
    const subscription = subscribeToAgent(agent.id, (updatedState) => {
      // Update agent state in real-time
      setAgent(prev => ({
        ...prev,
        x: updatedState.x,
        y: updatedState.y,
        status: updatedState.status,
        currentLocationId: updatedState.currentLocationId || prev?.currentLocationId,
        currentActionDescription: updatedState.currentActionDescription
      }));

      if (updatedState.currentActionDescription) {
        addLog(updatedState.currentActionDescription);
      }
    });

    };
  }, [agent?.id, connected, subscribeToAgent, addLog]);

  // Effect to handle map/location change
  useEffect(() => {
    if (!agent?.currentLocationId) return;

    const shouldFetch = !location || location.id !== agent.currentLocationId;
    
    if (shouldFetch) {
      const updateLocation = async () => {
        try {
          console.log("Location changed! Fetching details for:", agent.currentLocationId);
          const locDetails = await agentApi.getLocationDetails(agent.currentLocationId);
          setLocation(locDetails);
          addLog("Wkroczono do: " + locDetails.name);
        } catch (err) {
          console.error("Failed to fetch new location details:", err);
        }
      };
      updateLocation();
    }
  }, [agent?.currentLocationId, location?.id, addLog]);

  const handleCommand = async (goal) => {
    if (agent) {
      addLog("> USER CMD: " + goal);
      await agentApi.sendGoal(agent.id, goal);
      // No need to fetch data again, WebSocket will push the update
    }
  };

  if (loading && !agent) {
    return (
      <View style={[styles.container, styles.center]}>
        <ActivityIndicator size="large" color={theme.colors.gold} />
        <Text style={styles.loadingText}>Łączenie z krainą...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <StatusBar hidden />
      
      <MapView 
        agentX={agent?.x || 0}
        agentY={agent?.y || 0}
        mapWidth={location ? location.width : 100}
        mapHeight={location ? location.height : 100}
        portals={location ? location.portals : []}
        locationName={location ? location.name : 'Unknown Realm'}
      />

      <View style={[styles.hudOverlay, { pointerEvents: 'box-none' }]}>
        <SafeAreaView style={{ flex: 1, pointerEvents: 'box-none' }}>
          <TopBar 
            gold={1575} 
            gems={28} 
            locationName={location ? location.name : 'Unknown Realm'} 
          />

          <View style={[styles.contentContainer, { pointerEvents: 'box-none' }]}>
            <AgentProfile 
              name={agent?.name || 'Shadow-01'}
              level={agent?.level || 1}
              hp={agent?.hp || 100}
              maxHp={agent?.maxHp || 100}
              status={agent?.status}
              x={agent?.x}
              y={agent?.y}
              mapWidth={agent?.mapWidth}
              mapHeight={agent?.mapHeight}
              currentTask={agent?.currentTask}
              currentAction={agent?.currentActionDescription}
            />

            <SideMenu />

            <View style={[styles.bottomLeftContainer, { pointerEvents: 'box-none' }]}>
              <AgentConsole 
                agentId={agent?.id} 
                logs={logs}
                onCommandSent={handleCommand} 
              />
            </View>
          </View>
        </SafeAreaView>
      </View>

      {!connected && (
        <View style={styles.onlineBadge}>
          <Text style={styles.onlineText}>Reconnecting...</Text>
        </View>
      )}
    </View>
  );
}

export default function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  const handleLoginSuccess = () => {
    setIsAuthenticated(true);
  };

  if (!isAuthenticated) {
    return <LoginScreen onLoginSuccess={handleLoginSuccess} />;
  }

  return (
    <SocketProvider>
       <GameContent />
    </SocketProvider>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  center: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  hudOverlay: {
    ...StyleSheet.absoluteFillObject,
  },
  contentContainer: {
    flex: 1,
    padding: theme.spacing.hud,
  },
  bottomLeftContainer: {
    position: 'absolute',
    bottom: 20,
    left: 20,
  },
  loadingText: {
    color: theme.colors.text.muted,
    marginTop: theme.spacing.md,
  },
  onlineBadge: {
    position: 'absolute',
    top: 60,
    right: 20,
    backgroundColor: 'rgba(239, 68, 68, 0.2)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
  },
  onlineText: {
    color: theme.colors.danger,
    fontSize: 10,
    fontWeight: 'bold',
  }
});
