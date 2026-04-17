import { useState } from 'react';
import { StyleSheet, View, SafeAreaView, StatusBar, Text, ActivityIndicator, ImageBackground, useWindowDimensions } from 'react-native';
import hudConfig from './src/theme/hudConfig.json';
import { theme } from './src/theme/theme';
import { AgentProfile } from './src/features/agent/AgentProfile';
import { MapView } from './src/features/world/MapView';
import { AgentConsole } from './src/features/hud/AgentConsole';
import { TopBar } from './src/features/hud/TopBar';
import { SideMenu } from './src/features/hud/SideMenu';
import { HUDElement } from './src/features/hud/HUDElement';
import { LoginScreen } from './src/features/auth/LoginScreen';
import { SocketProvider } from './src/api/SocketContext';

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
    logs, 
    loading, 
    error, 
    connected, 
    handleCommand 
  } = useAgentState();

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

  return (
    <ImageBackground 
      source={require('./assets/background.png')} 
      style={[styles.container, styles.fullScreen]}
      imageStyle={styles.fullScreen}
      resizeMode="cover"
    >
      <View style={styles.container}>
        <StatusBar hidden />
        
        <MapView 
          agentX={agent?.x || 0}
          agentY={agent?.y || 0}
          mapWidth={location ? location.width : 100}
          mapHeight={location ? location.height : 100}
          portals={location ? location.portals : []}
          locationName={location ? location.name : 'Unknown Realm'}
          agentName={agent?.name || 'Shadow-01'}
        />

        <View style={[styles.hudOverlay, { pointerEvents: 'box-none' }]}>
          <SafeAreaView style={{ flex: 1, pointerEvents: 'box-none' }}>
            
            <HUDElement id="TOP_BAR">
              <TopBar 
                gold={agent?.gold || 0} 
                gems={agent?.gems || 0} 
                locationName={location ? location.name : 'Unknown Realm'} 
              />
            </HUDElement>

            <HUDElement id="AGENT_PROFILE">
              <AgentProfile 
                name={agent?.name || 'Shadow-01'}
                level={agent?.level || 1}
                hp={agent?.hp || 100}
                maxHp={agent?.maxHp || 100}
                status={agent?.status}
                x={agent?.x}
                y={agent?.y}
                currentTask={agent?.currentTask}
                currentAction={agent?.currentActionDescription}
                hudConfig={currentHudConfig}
              />
            </HUDElement>

            <HUDElement id="SIDE_MENU">
              <SideMenu />
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
    </ImageBackground>
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
