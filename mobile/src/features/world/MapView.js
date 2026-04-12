import React, { useEffect, useRef, useState, useMemo } from 'react';
import { View, StyleSheet, Text, ImageBackground, Animated, Image } from 'react-native';
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

const MAP_CONFIG = {
  'Forest of Beginnings': {
    uri: require('../../../assets/maps/forest_of_beginnings.png'),
    ratio: 1.0,
  },
  'Azure Meadow': {
    uri: require('../../../assets/maps/azure_meadow.png'),
    ratio: 1.5,
  },
  'Deep Iron Mine': {
    uri: require('../../../assets/maps/deep_iron_mine.png'),
    ratio: 1.77,
  },
  'default': {
    uri: require('../../../assets/rpg_map.png'),
    ratio: 1.33,
  },
};

export const MapView = ({ agentX, agentY, mapWidth, mapHeight, portals = [], locationName = "" }) => {
  const config = MAP_CONFIG[locationName] || MAP_CONFIG['default'];
  const [currentMap, setCurrentMap] = useState(config.uri);
  const [currentRatio, setCurrentRatio] = useState(config.ratio);
  const [containerLayout, setContainerLayout] = useState({ width: 0, height: 0 });
  const fadeAnim = useRef(new Animated.Value(1)).current;
  
  // Best Fit Algorithm: Ensure the map canvas respects the aspect ratio within BOTH width and height constraints
  const canvasSize = useMemo(() => {
    const { width: cw, height: ch } = containerLayout;
    if (cw === 0 || ch === 0) return { width: '100%', height: '100%' };

    const ratio = currentRatio;
    let targetWidth = cw;
    let targetHeight = cw / ratio;

    // If perfectly filling width makes it too tall for the viewport, scale by height instead
    if (targetHeight > ch) {
      targetHeight = ch;
      targetWidth = ch * ratio;
    }

    return { width: targetWidth, height: targetHeight };
  }, [containerLayout, currentRatio]);

  const agentPos = useRef(new Animated.ValueXY({ 
      x: (agentX / mapWidth) * 100, 
      y: (agentY / mapHeight) * 100 
  })).current;

  useEffect(() => {
    const nextConfig = MAP_CONFIG[locationName] || MAP_CONFIG['default'];
    if (nextConfig.uri === currentMap) return;

    Animated.timing(fadeAnim, { toValue: 0, duration: 250, useNativeDriver: true }).start(() => {
      setCurrentMap(nextConfig.uri);
      setCurrentRatio(nextConfig.ratio);
      Animated.timing(fadeAnim, { toValue: 1, duration: 500, useNativeDriver: true }).start();
    });
  }, [locationName, currentMap, fadeAnim]);

  useEffect(() => {
    Animated.timing(agentPos, {
      toValue: { 
          x: (agentX / mapWidth) * 100, 
          y: (agentY / mapHeight) * 100 
      },
      duration: 1000, 
      useNativeDriver: false,
    }).start();
  }, [agentX, agentY, mapWidth, mapHeight, agentPos]);

  const onViewportLayout = (event) => {
    const { width, height } = event.nativeEvent.layout;
    setContainerLayout({ width, height });
  };

  return (
    <View style={styles.container}>
      <Animated.Image 
        source={currentMap}
        style={[styles.ambientBackground, { 
            opacity: fadeAnim.interpolate({
                inputRange: [0, 1],
                outputRange: [0, 0.35]
            })
        }]}
        blurRadius={30}
        resizeMode="cover"
      />

      <Animated.View 
        style={[styles.mapViewport, { opacity: fadeAnim }]}
        onLayout={onViewportLayout}
      >
        {/* The Measured Canvas: Strictly follows calculated 'Best Fit' pixels */}
        <View style={[styles.canvas, canvasSize]}>
            <Corner position="topLeft" />
            <Corner position="topRight" />
            <Corner position="bottomLeft" />
            <Corner position="bottomRight" />

            <ImageBackground 
                source={currentMap}
                style={styles.imageLayer}
                resizeMode="stretch" 
            >
                <View style={styles.worldOverlay}>
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
                                <View style={styles.portalIconWrapper}>
                                    <Atom size={20} color={theme.colors.primary} strokeWidth={2.5} />
                                </View>
                            </PulseIcon>
                            <Text style={styles.portalLabel}>{portal.targetLocationName}</Text>
                        </View>
                    ))}

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
                        <View style={styles.pinWrapper}>
                            <MapPin size={28} color={theme.colors.accent} strokeWidth={3} fill={theme.colors.accent + '20'} />
                        </View>
                    </Animated.View>
                </View>
            </ImageBackground>
        </View>
      </Animated.View>

      <View style={styles.vignette} pointerEvents="none" />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: '#0a0a0b',
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: -1,
  },
  ambientBackground: {
    ...StyleSheet.absoluteFillObject,
    width: '100%',
    height: '100%',
  },
  mapViewport: {
    width: '92%',
    maxWidth: 1100,
    height: '82%', 
    justifyContent: 'center',
    alignItems: 'center',
    position: 'relative',
  },
  canvas: {
    backgroundColor: '#000',
    borderRadius: 8,
    overflow: 'hidden',
    position: 'relative',
    borderWidth: 1.5,
    borderColor: 'rgba(79, 209, 237, 0.3)',
    shadowColor: theme.colors.primary,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.8,
    shadowRadius: 15,
  },
  imageLayer: {
    flex: 1,
    width: '100%',
    height: '100%',
  },
  worldOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0, 0, 0, 0.08)',
  },
  portalContainer: {
    position: 'absolute',
    alignItems: 'center',
    justifyContent: 'center',
    width: 60,
    height: 60,
    marginLeft: -30,
    marginTop: -30,
  },
  portalIconWrapper: {
    backgroundColor: 'rgba(79, 209, 237, 0.2)',
    padding: 6,
    borderRadius: 22,
    borderWidth: 1,
    borderColor: 'rgba(79, 209, 237, 0.5)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  portalLabel: {
    color: '#fff',
    fontSize: 9,
    fontWeight: '900',
    marginTop: 4,
    textShadowColor: 'black',
    textShadowRadius: 3,
    backgroundColor: 'rgba(0, 0, 0, 0.65)',
    paddingHorizontal: 8,
    borderRadius: 8,
    textAlign: 'center',
  },
  agentMarker: {
    position: 'absolute',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 100,
    width: 60,
    height: 60,
    marginLeft: -30,
    marginTop: -45, 
  },
  pinWrapper: {
    shadowColor: theme.colors.accent,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.8,
    shadowRadius: 6,
  },
  pulseRing: {
    position: 'absolute',
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'rgba(246, 173, 85, 0.2)',
    borderWidth: 1.5,
    borderColor: theme.colors.accent,
  },
  agentTag: {
    position: 'absolute',
    bottom: 58,
    backgroundColor: 'rgba(10, 10, 11, 0.98)',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 6,
    borderWidth: 1.5,
    borderColor: theme.colors.accent,
    alignItems: 'center',
    minWidth: 90,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.6,
    shadowRadius: 4,
  },
  agentTagText: {
    color: '#fff',
    fontSize: 10,
    fontWeight: '900',
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  agentCoords: {
    color: theme.colors.accent,
    fontSize: 8,
    fontWeight: 'bold',
    marginTop: 0,
  },
  vignette: {
    ...StyleSheet.absoluteFillObject,
    borderWidth: 60,
    borderColor: 'rgba(0,0,0,0.45)',
  }
});
