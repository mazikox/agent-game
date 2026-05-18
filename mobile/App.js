import { useState, useEffect } from 'react';
import { StyleSheet, View, SafeAreaView, StatusBar, Text, ActivityIndicator, ImageBackground, useWindowDimensions, Platform, Image } from 'react-native';
import hudConfig from './src/theme/hudConfig.json';
import { theme } from './src/theme/theme';
import { AgentProfile } from './src/features/agent/AgentProfile';
import { MapView } from './src/features/world/MapView';
import { AgentConsole } from './src/features/hud/AgentConsole';
import { AgentActionsPanel } from './src/features/hud/AgentActionsPanel';
import { SideMenu } from './src/features/hud/SideMenu';
import { HUDElement } from './src/features/hud/HUDElement';
import { LoginScreen } from './src/features/auth/LoginScreen';
import { SocketProvider } from './src/api/SocketContext';
import { InventoryPanel } from './src/features/hud/InventoryPanel';
import { AdminPanel } from './src/features/admin/AdminPanel';

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
    approachNearest,
    stopAgent,
    performCombatAction,
    panel,
    panelLoading,
    panelError,
    closePanel,
    handleCreatureClick,
    handleActionPress
  } = useAgentState();

  const [activeTab, setActiveTab] = useState('map');
  const [adminCoords, setAdminCoords] = useState({ x: null, y: null });

  useEffect(() => {
    // Reset selected coords when leaving admin tab
    if (activeTab !== 'admin') {
      setAdminCoords({ x: null, y: null });
    }
  }, [activeTab]);

  // GLOBAL CURSOR INJECTION (WEB ONLY)
  useEffect(() => {
    if (Platform.OS === 'web') {
      try {
        const cursorBase64 = 'data:image/png;base64,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';
        
        console.log('[CursorSystem] Injecting Base64 cursor directly...');

        const styleId = 'custom-game-cursor';
        let styleTag = document.getElementById(styleId);
        
        if (!styleTag) {
          styleTag = document.createElement('style');
          styleTag.id = styleId;
          document.head.appendChild(styleTag);
        }

        styleTag.innerHTML = `
          * { 
            cursor: url("${cursorBase64}") 0 0, auto !important; 
          }
          button, a, [role="button"], [style*="cursor: pointer"] {
            cursor: url("${cursorBase64}") 0 0, pointer !important;
          }
        `;
        
        console.log('[CursorSystem] Style injected successfully.');
      } catch (err) {
        console.error('[CursorSystem] Failed to inject cursor:', err);
      }
    }
  }, []);

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
            onCreaturePress={handleCreatureClick}
            interactionPanel={panel}
            panelLoading={panelLoading}
            panelError={panelError}
            onAction={handleActionPress}
            onClose={closePanel}
          />
        );
      case 'inventory':
        return <InventoryPanel agentId={agent?.id} />;
      case 'admin':
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
            onPress={(x, y) => setAdminCoords({ x, y })}
            interactionPanel={panel}
            panelLoading={panelLoading}
            panelError={panelError}
            onAction={handleActionPress}
            onClose={closePanel}
          />
        );
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
      <View style={styles.hudOverlay} pointerEvents="box-none">
        <SafeAreaView style={{ flex: 1 }} pointerEvents="box-none">

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

          <HUDElement id="AGENT_ACTIONS">
            <AgentActionsPanel
              onApproachEnemy={approachNearest}
              onAttackEnemy={attackNearest}
              onStop={stopAgent}
            />
          </HUDElement>

          {activeTab === 'admin' && (
            <HUDElement id="ADMIN_PANEL">
              <AdminPanel
                location={location}
                selectedX={adminCoords.x}
                selectedY={adminCoords.y}
                onClose={() => setAdminCoords({ x: null, y: null })}
              />
            </HUDElement>
          )}

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
