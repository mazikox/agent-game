import { useState } from 'react';
import { StyleSheet, View, SafeAreaView, StatusBar, Text, ActivityIndicator, ImageBackground, useWindowDimensions } from 'react-native';
import hudConfig from './src/theme/hudConfig.json';
import { theme } from './src/theme/theme';
import { AgentProfile } from './src/features/agent/AgentProfile';
import { MapView } from './src/features/world/MapView';
import { AgentConsole } from './src/features/hud/AgentConsole';
import { SideMenu } from './src/features/hud/SideMenu';
import { HUDElement } from './src/features/hud/HUDElement';
import { LoginScreen } from './src/features/auth/LoginScreen';
import { SocketProvider } from './src/api/SocketContext';
import { InventoryPanel } from './src/features/hud/InventoryPanel';

// FANTASY FONTS
import { useFonts, Cinzel_700Bold } from '@expo-google-fonts/cinzel';
import { Lora_400Regular, Lora_700Bold } from '@expo-google-fonts/lora';

import { useAgentState } from './src/features/agent/hooks/useAgentState';

/**
 * Main Content component that uses the SocketContext.
 */
function GameContent() {
  const { 
    agent, 
    location, 
    creatures,
    logs, 
    loading, 
    error, 
    connected, 
    handleCommand,
    attackNearest,
    performCombatAction 
  } = useAgentState();

  const [activeTab, setActiveTab] = useState('map');

  const { width } = useWindowDimensions();
  const isCompact = width < 768; // Tablet threshold
  const currentHudConfig = isCompact ? hudConfig.COMPACT : hudConfig.DEFAULT;

  if (loading && !agent) {
    return (
      <View style={[styles.container, styles.center]}>
        <ActivityIndicator size="large" color={theme.colors.gold} />
        <Text style={styles.loadingText}>Łączenie z krainą...</Text>
      </View>
    );
  }

  const renderMainContent = () => {
    switch (activeTab) {
      case 'map':
        return (
          <MapView 
            agentX={agent?.x || 0}
            agentY={agent?.y || 0}
            mapWidth={location ? location.width : 100}
            mapHeight={location ? location.height : 100}
            portals={location ? location.portals : []}
            creatures={creatures || []}
            locationName={location ? location.name : 'Unknown Realm'}
            agentName={agent?.name || 'Shadow-01'}
          />
        );
      case 'inventory':
        return <InventoryPanel agentId={agent?.id} />;
      default:
        return (
          <View style={[styles.container, styles.center, { padding: 20 }]}>
            <Text style={{ color: theme.colors.accent, fontSize: 20, fontFamily: theme.typography.fantasy }}>
              {activeTab.toUpperCase()}
            </Text>
            <Text style={{ color: '#cbd5e0', marginTop: 10, textAlign: 'center' }}>
              Ta sekcja krainy jest jeszcze niezbadana...
            </Text>
          </View>
        );
    }
  };

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.fantasy.stone }]}>
      <StatusBar hidden />
      
      {/* MAIN INTEGRATED LAYOUT */}
      <View style={styles.mainRow}>
          {/* CENTER: MAP AREA */}
          <View style={styles.mapArea}>
            {renderMainContent()}
          </View>
      </View>

      {/* RIGHT: SIDEBAR (Absolute positioned to prevent overlapping by overlay) */}
      <View style={styles.sidebarArea}>
        <SideMenu activeTab={activeTab} onTabChange={setActiveTab} />
      </View>

        {/* HUD OVERLAY (ABSOLUTE ELEMENTS) */}
        <View style={[styles.hudOverlay, { pointerEvents: 'box-none' }]}>
          <SafeAreaView style={{ flex: 1, pointerEvents: 'box-none' }}>
            
            <HUDElement id="AGENT_PROFILE">
              <AgentProfile 
                name={agent?.name || 'Shadow-01'}
                level={agent?.level || 1}
                hp={agent?.hp || 100}
                maxHp={agent?.maxHp || 100}
                status={agent?.status}
                x={agent?.x}
                y={agent?.y}
                targetId={agent?.targetId}
                targetName={agent?.targetName}
                targetHp={agent?.targetHp}
                targetMaxHp={agent?.targetMaxHp}
                creatures={creatures}
                currentTask={agent?.currentTask}
                currentAction={agent?.currentActionDescription}
                hudConfig={currentHudConfig}
                onAttackNearest={attackNearest}
                onCombatAction={performCombatAction}
              />
            </HUDElement>

            <HUDElement id="AGENT_CONSOLE">
              <AgentConsole 
                agentId={agent?.id} 
                logs={logs}
                onCommandSent={handleCommand} 
              />
            </HUDElement>

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
  },
  fullScreen: {
    width: '100%',
    height: '100%',
  },
  mainRow: {
    flex: 1,
    flexDirection: 'row',
  },
  mapArea: {
    flex: 1,
    padding: 15,
    marginRight: 85, // Reserve space for absolute sidebar
  },
  sidebarArea: {
    position: 'absolute',
    right: 0,
    top: 0,
    bottom: 0,
    width: 85,
    zIndex: 200,
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
    zIndex: 100, // Ensure tactical interface is always on top
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
