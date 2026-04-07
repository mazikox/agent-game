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

export default function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [agent, setAgent] = useState(null);
  const [location, setLocation] = useState(null);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const addLog = (message) => {
    const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    setLogs(prev => {
      // Don't add duplicate consecutive logs
      if (prev.length > 0 && prev[prev.length - 1].message === message) return prev;
      return [...prev, { time, message }].slice(-50); // Keep last 50
    });
  };

  const handleLoginSuccess = () => {
    setIsAuthenticated(true);
  };

  const fetchAgentData = useCallback(async () => {
    try {
      const agents = await agentApi.getAllAgents();
      if (agents && agents.length > 0) {
        const detailedAgent = await agentApi.getAgentDetails(agents[0].id);
        setAgent(detailedAgent);
        
        // Update logs if action changed
        if (detailedAgent.currentActionDescription) {
          addLog(detailedAgent.currentActionDescription);
        }

        if (!location || location.id !== detailedAgent.currentLocationId) {
           const locDetails = await agentApi.getLocationDetails(detailedAgent.currentLocationId);
           setLocation(locDetails);
        }
        setError(null);
      }
    } catch (err) {
      console.error("API Error:", err);
      setError("Błąd połączenia");
    } finally {
      setLoading(false);
    }
  }, [location]);

  useEffect(() => {
    if (!isAuthenticated) return;
    fetchAgentData();
    const interval = setInterval(fetchAgentData, 1000);
    return () => clearInterval(interval);
  }, [fetchAgentData, isAuthenticated]);

  const handleCommand = async (goal) => {
    if (agent) {
      addLog("> USER CMD: " + goal);
      await agentApi.sendGoal(agent.id, goal);
      fetchAgentData();
    }
  };

  if (!isAuthenticated) {
    return <LoginScreen onLoginSuccess={handleLoginSuccess} />;
  }

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
      
      {/* BACKGROUND LAYER - Functional Map */}
      <MapView 
        agentX={agent?.x || 0}
        agentY={agent?.y || 0}
        mapWidth={location ? location.width : 100}
        mapHeight={location ? location.height : 100}
        portals={location ? location.portals : []}
        locationName={location ? location.name : 'Unknown Realm'}
      />

      {/* HUD LAYER */}
      <View style={[styles.hudOverlay, { pointerEvents: 'box-none' }]}>
        <SafeAreaView style={{ flex: 1, pointerEvents: 'box-none' }}>
          
          {/* HEADER LAYER */}
          <TopBar 
            gold={1575} 
            gems={28} 
            locationName={location ? location.name : 'Unknown Realm'} 
          />

          {/* OVERLAPPING COMPONENTS */}
          <View style={[styles.contentContainer, { pointerEvents: 'box-none' }]}>
            
            {/* TOP LEFT: PROFILE */}
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

            {/* MIDDLE RIGHT: 2-COLUMN MENU */}
            <SideMenu />

            {/* BOTTOM LEFT / CENTER: AGENT CONSOLE */}
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

      {error && (
        <View style={styles.onlineBadge}>
          <Text style={styles.onlineText}>Offline</Text>
        </View>
      )}
    </View>
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
