import React, { useEffect, useRef, useState } from 'react';
import { View, StyleSheet, Text, ImageBackground, Animated } from 'react-native';
import { theme } from '../../theme/theme';
import { MapPin, Atom } from 'lucide-react-native';

const Corner = ({ position }) => {
  const styles = StyleSheet.create({
    corner: {
      position: 'absolute',
      width: 15,
      height: 15,
      borderColor: theme.colors.gold,
      zIndex: 5,
    },
    topLeft: { top: -2, left: -2, borderTopWidth: 3, borderLeftWidth: 3 },
    topRight: { top: -2, right: -2, borderTopWidth: 3, borderRightWidth: 3 },
    bottomLeft: { bottom: -2, left: -2, borderBottomWidth: 3, borderLeftWidth: 3 },
    bottomRight: { bottom: -2, right: -2, borderBottomWidth: 3, borderRightWidth: 3 },
  });
  return <View style={[styles.corner, styles[position]]} />;
};

const PulseIcon = ({ children }) => {
  const scale = useRef(new Animated.Value(1)).current;
  const opacity = useRef(new Animated.Value(0.8)).current;

  useEffect(() => {
    Animated.loop(
      Animated.parallel([
        Animated.sequence([
          Animated.timing(scale, { toValue: 1.2, duration: 1000, useNativeDriver: true }),
          Animated.timing(scale, { toValue: 1, duration: 1000, useNativeDriver: true }),
        ]),
        Animated.sequence([
          Animated.timing(opacity, { toValue: 0.4, duration: 1000, useNativeDriver: true }),
          Animated.timing(opacity, { toValue: 0.8, duration: 1000, useNativeDriver: true }),
        ])
      ])
    ).start();
  }, [scale, opacity]);

  return (
    <Animated.View style={{ transform: [{ scale }], opacity }}>
      {children}
    </Animated.View>
  );
};

// Map asset mapping
const MAP_ASSETS = {
  'Forest of Beginnings': require('../../../assets/maps/forest_of_beginnings.png'),
  'Azure Meadow': require('../../../assets/maps/azure_meadow.png'),
  'default': require('../../../assets/rpg_map.png'),
};

export const MapView = ({ agentX, agentY, mapWidth, mapHeight, portals = [], locationName = "" }) => {
  const [currentMap, setCurrentMap] = useState(MAP_ASSETS['default']);
  const fadeAnim = useRef(new Animated.Value(0)).current;
  
  // Position animation for the agent
  const agentPos = useRef(new Animated.ValueXY({ 
      x: (agentX / mapWidth) * 100, 
      y: (agentY / mapHeight) * 100 
  })).current;

  // Track map changes for fade effect
  useEffect(() => {
    const nextMap = MAP_ASSETS[locationName] || MAP_ASSETS['default'];
    if (nextMap === currentMap) return;

    Animated.timing(fadeAnim, { toValue: 0, duration: 300, useNativeDriver: true }).start(() => {
      setCurrentMap(nextMap);
      Animated.timing(fadeAnim, { toValue: 1, duration: 600, useNativeDriver: true }).start();
    });
  }, [locationName, currentMap, fadeAnim]);

  // Handle smooth agent movement (interpolation over 1 second)
  useEffect(() => {
    Animated.timing(agentPos, {
      toValue: { 
          x: (agentX / mapWidth) * 100, 
          y: (agentY / mapHeight) * 100 
      },
      duration: 1000, 
      useNativeDriver: false, // Layout animations (left/top) don't support native driver
    }).start();
  }, [agentX, agentY, mapWidth, mapHeight, agentPos]);

  return (
    <View style={styles.container}>
      <Animated.View style={[styles.boardWrapper, { opacity: fadeAnim }]}>
        <Corner position="topLeft" />
        <Corner position="topRight" />
        <Corner position="bottomLeft" />
        <Corner position="bottomRight" />
        
        <ImageBackground 
            source={currentMap}
            style={styles.boardContent}
            resizeMode="stretch" // Critical for coordinate alignment
        >
            <View style={styles.worldOverlay}>
                {/* Portals */}
                {portals.map((portal, index) => (
                    <View 
                        key={`portal-${index}`} 
                        style={[
                            styles.portalContainer, 
                            { 
                                left: `${(portal.sourceX / mapWidth) * 100}%`, 
                                top: `${(portal.sourceY / mapHeight) * 100}%` 
                            }
                        ]}
                    >
                        <PulseIcon>
                          <Atom size={20} color={theme.colors.primary} strokeWidth={2.5} />
                        </PulseIcon>
                        <Text style={styles.portalLabel}>
                          {portal.targetLocationName || 'AZURE MEADOW'}
                        </Text>
                    </View>
                ))}

                {/* Agent Marker (Animated) */}
                <Animated.View style={[
                    styles.agentMarker,
                    {
                        left: agentPos.x.interpolate({
                            inputRange: [0, 100],
                            outputRange: ['0%', '100%']
                        }),
                        top: agentPos.y.interpolate({
                            inputRange: [0, 100],
                            outputRange: ['0%', '100%']
                        })
                    }
                ]}>
                    <View style={styles.pulseRing} />
                    <View style={styles.agentTag}>
                        <Text style={styles.agentTagText}>Shadow-01</Text>
                        <Text style={styles.agentCoords}>({agentX}, {agentY})</Text>
                    </View>
                    <MapPin size={28} color={theme.colors.accent} strokeWidth={3} />
                </Animated.View>
            </View>
        </ImageBackground>
      </Animated.View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: '#000',
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: -1,
  },
  boardWrapper: {
    width: '90%', // Slightly larger for better view
    aspectRatio: 1, // Enforce square if map is 100x100
    borderWidth: 2,
    borderColor: 'rgba(79, 209, 237, 0.4)',
    backgroundColor: '#000',
    position: 'relative',
    shadowColor: theme.colors.primary,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.5,
    shadowRadius: 10,
    elevation: 10,
  },
  boardContent: {
    flex: 1,
    overflow: 'hidden',
  },
  worldOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0, 0, 0, 0.15)',
  },
  portalContainer: {
    position: 'absolute',
    alignItems: 'center',
    justifyContent: 'center',
    transform: [{ translateX: -10 }, { translateY: -10 }], // Center based on icon size
  },
  portalLabel: {
    color: '#fff',
    fontSize: 9,
    fontWeight: '900',
    marginTop: 2,
    textShadowColor: 'black',
    textShadowRadius: 4,
    textAlign: 'center',
  },
  agentMarker: {
    position: 'absolute',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 100,
    transform: [{ translateX: -14 }, { translateY: -24 }], // Offset pin point
  },
  pulseRing: {
    position: 'absolute',
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'rgba(246, 173, 85, 0.4)',
    borderWidth: 2,
    borderColor: theme.colors.accent,
  },
  agentTag: {
    position: 'absolute',
    bottom: 35,
    backgroundColor: 'rgba(0, 0, 0, 0.75)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
    borderWidth: 1,
    borderColor: theme.colors.accent,
    alignItems: 'center',
    minWidth: 70,
  },
  agentTagText: {
    color: '#fff',
    fontSize: 9,
    fontWeight: '900',
    textTransform: 'uppercase',
  },
  agentCoords: {
    color: theme.colors.accent,
    fontSize: 8,
    fontWeight: 'bold',
    marginTop: 1,
  },
});
