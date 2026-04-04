import React, { useState, useEffect, useCallback } from 'react';
import { StyleSheet, View, SafeAreaView, StatusBar, Text, ScrollView, ActivityIndicator } from 'react-native';
import { theme } from './src/theme/theme';
import { AgentProfile } from './src/features/agent/AgentProfile';
import { MapView } from './src/features/world/MapView';
import { ActionFeed } from './src/features/feed/ActionFeed';
import { CommandPanel } from './src/features/controls/CommandPanel';
import { agentApi } from './src/api/agentApi';
import { LinearGradient } from 'expo-linear-gradient';

export default function App() {
  const [agent, setAgent] = useState(null);
  const [location, setLocation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchAgentData = useCallback(async () => {
    try {
      const agents = await agentApi.getAllAgents();
      if (agents && agents.length > 0) {
        // 1. Fetch live agent position (Redis-backed)
        const detailedAgent = await agentApi.getAgentDetails(agents[0].id);
        setAgent(detailedAgent);
        
        // 2. If location changed, fetch map context (Postgres-backed)
        if (!location || location.id !== detailedAgent.currentLocationId) {
           const locDetails = await agentApi.getLocationDetails(detailedAgent.currentLocationId);
           setLocation(locDetails);
        }
        setError(null);
      }
    } catch (err) {
      console.error("API Error:", err);
      setError("Błąd połączenia z serwerem MMO");
    } finally {
      setLoading(false);
    }
  }, [location]);

  useEffect(() => {
    fetchAgentData();
    const interval = setInterval(fetchAgentData, 3000);
    return () => clearInterval(interval);
  }, [fetchAgentData]);

  const handleCommand = async (goal) => {
    if (agent) {
      await agentApi.sendGoal(agent.id, goal);
      fetchAgentData();
    }
  };

  if (loading && !agent) {
    return (
      <View style={[styles.container, styles.center]}>
        <ActivityIndicator size="large" color={theme.colors.accent} />
        <Text style={styles.loadingText}>Łączenie z krainą...</Text>
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="light-content" />
      <LinearGradient
        colors={[theme.colors.background, '#1a1a1c']}
        style={styles.gradient}
      >
        <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false}>
          <View style={styles.header}>
            <Text style={styles.gameTitle}>BOT KINGDOMS</Text>
            <View style={styles.onlineBadge}>
              <View style={styles.onlineDot} />
              <Text style={styles.onlineText}>{error ? 'Offline' : 'Live'}</Text>
            </View>
          </View>

          {agent ? (
            <>
              <AgentProfile 
                name={agent.name}
                level={1}
                hp={100}
                maxHp={100}
                atk={15}
                def={10}
                x={agent.x}
                y={agent.y}
                locationName={location ? location.name : 'Unknown'}
                goal={agent.goal}
              />

              <MapView 
                agentX={agent.x}
                agentY={agent.y}
                mapWidth={location ? location.width : 100}
                mapHeight={location ? location.height : 100}
                portals={location ? location.portals : []}
              />
            </>
          ) : (
            <View style={styles.errorContainer}>
              <Text style={styles.errorText}>{error || "Brak aktywnych agentów w bazie."}</Text>
            </View>
          )}

          <ActionFeed />
        </ScrollView>

        {agent && (
          <CommandPanel 
            agentId={agent.id} 
            onCommandSent={handleCommand} 
          />
        )}
      </LinearGradient>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.colors.background,
  },
  center: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  gradient: {
    flex: 1,
  },
  scroll: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: theme.spacing.lg,
    paddingTop: theme.spacing.md,
  },
  gameTitle: {
    color: theme.colors.text.primary,
    fontSize: 14,
    fontWeight: '900',
    letterSpacing: 2,
  },
  onlineBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(16, 185, 129, 0.1)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
  },
  onlineDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: theme.colors.success,
    marginRight: 6,
  },
  onlineText: {
    color: theme.colors.success,
    fontSize: 10,
    fontWeight: 'bold',
  },
  loadingText: {
    color: theme.colors.text.muted,
    marginTop: theme.spacing.md,
  },
  errorContainer: {
    padding: theme.spacing.xl,
    alignItems: 'center',
  },
  errorText: {
    color: theme.colors.danger,
    textAlign: 'center',
  }
});
