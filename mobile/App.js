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

// FANTASY FONTS
import { useFonts, Cinzel_700Bold } from '@expo-google-fonts/cinzel';
import { Lora_400Regular, Lora_700Bold } from '@expo-google-fonts/lora';

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

  useEffect(() => {
    fetchInitialData();
    connect();
  }, [fetchInitialData, connect]);

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

  useEffect(() => {
    if (!agent?.currentLocationId) return;

    let isCancelled = false;
    const locId = agent.currentLocationId.toLowerCase();
    const currentLocId = location?.id?.toLowerCase();
    
    if (!location || currentLocId !== locId) {
      const updateLocation = async () => {
        try {
          const locDetails = await agentApi.getLocationDetails(locId);
          if (!isCancelled) {
            setLocation(locDetails);
            addLog("Wkroczono do: " + locDetails.name);
          }
        } catch (err) {
          console.error("[MapSync] Failed to fetch layout details:", err);
        }
      };
      updateLocation();
    }
    return () => { isCancelled = true; };
  }, [agent?.currentLocationId, location?.id, addLog]);

  const handleCommand = async (goal) => {
    if (agent) {
      addLog("> USER CMD: " + goal);
      await agentApi.sendGoal(agent.id, goal);
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
  
  // FONT LOADING
  const [fontsLoaded] = useFonts({
    Cinzel_700Bold,
    Lora_400Regular,
    Lora_700Bold,
  });

  const handleLoginSuccess = () => {
    setIsAuthenticated(true);
  };

  if (!fontsLoaded) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#ecc94b" />
        <Text style={styles.loadingText}>Wczytywanie magii...</Text>
      </View>
    );
  }

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
  loadingContainer: {
    flex: 1,
    backgroundColor: '#0a0a0b',
    justifyContent: 'center',
    alignItems: 'center',
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
    color: '#ecc94b',
    marginTop: theme.spacing.md,
    fontFamily: 'serif',
    fontWeight: 'bold',
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
