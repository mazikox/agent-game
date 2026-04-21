import React, { useEffect, useRef, useState, useMemo } from 'react';
import { View, StyleSheet, Text, ImageBackground, Animated, Image } from 'react-native';
import { theme } from '../../theme/theme';
import { MapPin, Atom } from 'lucide-react-native';
import { MapWindowFrame } from './MapWindowFrame';


const PulseIcon = ({ children }) => {
  const scale = useRef(new Animated.Value(1)).current;
  const opacity = useRef(new Animated.Value(0.8)).current;

  useEffect(() => {
    const animation = Animated.loop(
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
    );
    animation.start();
    return () => animation.stop();
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
    ratio: 1.0,
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

export const MapView = ({ agentX, agentY, mapWidth, mapHeight, portals = [], creatures = [], locationName = "", agentName = "Shadow-01" }) => {
  const config = MAP_CONFIG[locationName] || MAP_CONFIG['default'];
  const [currentMap, setCurrentMap] = useState(config.uri);
  const [currentRatio, setCurrentRatio] = useState(config.ratio);
  const [currentImgW, setCurrentImgW] = useState(1024); // Startowa naturalna wartość przed odpaleniem onLoad
  const [currentImgH, setCurrentImgH] = useState(1024);
  const [containerLayout, setContainerLayout] = useState({ width: 0, height: 0 });
  const fadeAnim = useRef(new Animated.Value(1)).current;
  const [viewportLayout, setViewportLayout] = useState({ width: 0, height: 0 });
  
  // Jednolity rozmiar ramki - niezależnie od załadowanej mapy, utrzymujemy stały układ i wielkość
  const canvasSize = useMemo(() => {
    const { width: cw, height: ch } = containerLayout;
    if (cw === 0 || ch === 0) return { width: '100%', height: '100%' };

    const HEADER_OFFSET = 44; // 38px header + 6px total vertical padding
    const WIDTH_OFFSET = 3;   // 3px left padding (right is 0)

    // Wymuszamy stałe ratio okienka dla wszystkich map (1.0 = kwadrat, idealnie pod stałe wycięcie 800x800)
    // Dzięki temu rozmiar ramki w grze NIGDY się nie zmieni przy zmianie mapy.
    const FIXED_RATIO = 1.0; 
    
    let targetWidth = cw;
    let targetHeight = ((cw - WIDTH_OFFSET) / FIXED_RATIO) + HEADER_OFFSET;

    if (targetHeight > ch) {
      targetHeight = ch;
      targetWidth = ((ch - HEADER_OFFSET) * FIXED_RATIO) + WIDTH_OFFSET;
    }

    return { width: targetWidth, height: targetHeight };
  }, [containerLayout]);

  // Przesunięcie gracza w układzie procentowym (0-100%) względem wielkości mapy logicznej
  const agentPosPerc = useRef(new Animated.ValueXY({ 
      x: (agentX / Math.max(mapWidth, 1)) * 100, 
      y: (agentY / Math.max(mapHeight, 1)) * 100 
  })).current;

  // Wymiary oryginalnego zdjęcia pobrane statycznie z MAP_CONFIG (omija brak Image.resolveAssetSource na platforrmie Web)
  const imgW = currentImgW;
  const imgH = currentImgH;

  // Kamera: wewnątrz viewportu (ramki) wyświetlamy obszar zawsze równy 800 fizycznym pikselom oryginalnego zdjęcia.
  const CAMERA_PIXELS = 800;
  const scale = viewportLayout.width > 0 ? (viewportLayout.width / CAMERA_PIXELS) : 1;

  // Wymiary wirtualnej mapy zachowują proporcje zdjęcia ale ich skala jest narzucona przez ramkę
  const worldWidth = imgW * scale;
  const worldHeight = imgH * scale;

  // Kamera centrująca podąża za procentem wyliczonej, sztucznie powiększonej wirtualnej planszy
  const cameraX = agentPosPerc.x.interpolate({
      inputRange: [0, 100],
      outputRange: [viewportLayout.width / 2, viewportLayout.width / 2 - worldWidth]
  });

  const cameraY = agentPosPerc.y.interpolate({
      inputRange: [0, 100],
      outputRange: [viewportLayout.height / 2, viewportLayout.height / 2 - worldHeight]
  });

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
    Animated.timing(agentPosPerc, {
      toValue: { 
          x: (agentX / Math.max(mapWidth, 1)) * 100, 
          y: (agentY / Math.max(mapHeight, 1)) * 100 
      },
      duration: 1000, 
      useNativeDriver: false,
    }).start();
  }, [agentX, agentY, mapWidth, mapHeight, agentPosPerc]);

  const onViewportLayout = (event) => {
    const { width, height } = event.nativeEvent.layout;
    setContainerLayout({ width, height });
  };

  return (
    <View style={styles.container}>

      <Animated.View 
        style={[styles.mapViewport, { opacity: fadeAnim }]}
        onLayout={onViewportLayout}
      >
        <MapWindowFrame 
          title={locationName} 
          style={[styles.mapWindow, canvasSize]}
        >
          <View 
              style={[styles.canvas, { width: '100%', height: '100%' }]}
              onLayout={(e) => setViewportLayout(e.nativeEvent.layout)}
          >
              {/* Rzeczywista powiększona mapa z efektem zoom-in na wycięty fragment */}
              <Animated.View style={[
                  {
                      position: 'absolute',
                      left: 0, top: 0,
                      width: worldWidth > 0 ? worldWidth : '100%', 
                      height: worldHeight > 0 ? worldHeight : '100%',
                      transform: [
                          { translateX: cameraX },
                          { translateY: cameraY }
                      ]
                  }
              ]}>
                  <ImageBackground 
                      source={currentMap}
                      style={styles.imageLayer}
                      resizeMode="stretch" 
                      onLoad={(e) => {
                          // Na Webie React Native pakuje event z HTML, więc precyzyjnie szukamy naturalWidth DOM'u
                          const ev = e.nativeEvent || {};
                          const target = ev.target || {};
                          const source = ev.source || {};

                          const w = source.width || ev.width || target.naturalWidth || target.clientWidth;
                          const h = source.height || ev.height || target.naturalHeight || target.clientHeight;
                          
                          if (w && h && !isNaN(w) && !isNaN(h) && w > 0) {
                              setCurrentImgW(Number(w));
                              setCurrentImgH(Number(h));
                          }
                      }}
                  >
                      <View style={styles.worldOverlay}>
                          {portals.map((portal, index) => (
                              <View 
                                  key={`portal-${index}`} 
                                  style={[
                                      styles.portalContainer, 
                                      { 
                                          left: `${(portal.sourceX / Math.max(mapWidth, 1)) * 100}%`, 
                                          top: `${(portal.sourceY / Math.max(mapHeight, 1)) * 100}%` 
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

                          {creatures.map((creature) => (
                              <View
                                  key={creature.instanceId}
                                  style={[
                                      styles.creatureMarker,
                                      {
                                          left: `${(Number(creature.x) / Math.max(mapWidth, 1)) * 100}%`,
                                          top: `${(Number(creature.y) / Math.max(mapHeight, 1)) * 100}%`
                                      }
                                  ]}
                              />
                          ))}

                          <Animated.View style={[
                              styles.agentMarker,
                              {
                                  left: agentPosPerc.x.interpolate({
                                      inputRange: [0, 100],
                                      outputRange: ['0%', '100%']
                                  }),
                                  top: agentPosPerc.y.interpolate({
                                      inputRange: [0, 100],
                                      outputRange: ['0%', '100%']
                                  })
                              }
                          ]}>
                              <View style={styles.pulseRing} />
                              <View style={styles.agentTag}>
                                  <Text style={styles.agentTagText}>{agentName}</Text>
                                  <Text style={styles.agentCoords}>({agentX}, {agentY})</Text>
                              </View>
                              <View style={styles.pinWrapper}>
                                  <MapPin size={28} color={theme.colors.accent} strokeWidth={3} fill={theme.colors.accent + '20'} />
                              </View>
                          </Animated.View>
                      </View>
                  </ImageBackground>
              </Animated.View>
          </View>
        </MapWindowFrame>
      </Animated.View>

      {/* <View style={styles.vignette} pointerEvents="none" /> */}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'flex-end', // Glued to sidebar
    position: 'relative',
  },
  ambientBackground: {
    ...StyleSheet.absoluteFillObject,
    width: '100%',
    height: '100%',
  },
  mapViewport: {
    width: '100%',
    height: '100%',
    justifyContent: 'center',
    alignItems: 'flex-end', // Glued to sidebar
    position: 'relative',
  },
  mapWindow: {
    width: '100%',
    height: '100%',
    borderRadius: 8,
  },
  canvas: {
    overflow: 'hidden', 
    position: 'relative',
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
  creatureMarker: {
    position: 'absolute',
    width: 24,
    height: 24,
    marginLeft: -12,
    marginTop: -12,
    borderRadius: 12,
    backgroundColor: '#dc2626', // Red-600
    borderColor: '#fca5a5', // Lighter red border
    borderWidth: 2,
    shadowColor: '#dc2626',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 1.0,
    shadowRadius: 8,
    zIndex: 90,
    elevation: 5,
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
