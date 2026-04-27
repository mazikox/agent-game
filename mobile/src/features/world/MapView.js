import React, { useEffect, useRef, useState } from 'react';
import { View, StyleSheet, Text, ImageBackground, Animated, Image, Platform } from 'react-native';
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

const MONSTER_ICONS = {
  '/creatures/wolf.png': require('../../../assets/monster_avatar.png'),
  '/creatures/spider.png': require('../../../assets/monster_avatar.png'),
  '/creatures/dragon.png': require('../../../assets/monster_avatar.png'),
};

const getMonsterIcon = (iconUrl) => {
  return MONSTER_ICONS[iconUrl] || require('../../../assets/monster_avatar.png');
};

export const MapView = ({ agentX, agentY, mapWidth, mapHeight, portals = [], creatures = [], locationName = "", agentName = "Shadow-01", onPress }) => {
  const config = MAP_CONFIG[locationName] || MAP_CONFIG['default'];

  const [currentMap, setCurrentMap] = useState(config.uri);
  const [currentImgW, setCurrentImgW] = useState(1024);
  const [currentImgH, setCurrentImgH] = useState(1024);
  const fadeAnim = useRef(new Animated.Value(1)).current;

  const agentPosPerc = useRef(new Animated.ValueXY({
    x: (agentX / Math.max(mapWidth, 1)) * 100,
    y: (agentY / Math.max(mapHeight, 1)) * 100,
  })).current;

  const cameraPos = useRef(new Animated.ValueXY({ x: 0, y: 0 })).current;

  // Plain-number mirror of cameraPos for click math
  const cameraPosRef = useRef({ x: 0, y: 0 });
  useEffect(() => {
    const idX = cameraPos.x.addListener(({ value }) => { cameraPosRef.current.x = value; });
    const idY = cameraPos.y.addListener(({ value }) => { cameraPosRef.current.y = value; });
    return () => {
      cameraPos.x.removeListener(idX);
      cameraPos.y.removeListener(idY);
    };
  }, [cameraPos]);



  const VIEWPORT = 800;
  const imgW = currentImgW;
  const imgH = currentImgH;

  useEffect(() => {
    const nextConfig = MAP_CONFIG[locationName] || MAP_CONFIG['default'];
    if (nextConfig.uri === currentMap) return;
    Animated.timing(fadeAnim, { toValue: 0, duration: 250, useNativeDriver: true }).start(() => {
      setCurrentMap(nextConfig.uri);
      Animated.timing(fadeAnim, { toValue: 1, duration: 500, useNativeDriver: true }).start();
    });
  }, [locationName]);

  useEffect(() => {
    if (!currentMap) return;
    const uri = typeof currentMap === 'string' ? currentMap : currentMap.uri || currentMap;
    Image.getSize(
      uri,
      (w, h) => { setCurrentImgW(w); setCurrentImgH(h); },
      (err) => console.error('Image.getSize error:', err)
    );
  }, [currentMap]);

  useEffect(() => {
    const safeMapW = Math.max(mapWidth, 1);
    const safeMapH = Math.max(mapHeight, 1);

    const agentPixelX = (agentX / safeMapW) * currentImgW;
    const agentPixelY = (agentY / safeMapH) * currentImgH;

    const offsetX = VIEWPORT / 2 - agentPixelX;
    const offsetY = VIEWPORT / 2 - agentPixelY;

    const targetX = Math.min(0, Math.max(VIEWPORT - currentImgW, offsetX));
    const targetY = Math.min(0, Math.max(VIEWPORT - currentImgH, offsetY));

    Animated.parallel([
      Animated.timing(agentPosPerc, {
        toValue: { x: (agentX / safeMapW) * 100, y: (agentY / safeMapH) * 100 },
        duration: 1000,
        useNativeDriver: false,
      }),
      Animated.timing(cameraPos, {
        toValue: { x: targetX, y: targetY },
        duration: 1000,
        useNativeDriver: false,
      }),
    ]).start();
  }, [agentX, agentY, mapWidth, mapHeight, currentImgW, currentImgH]);

  return (
    <View style={styles.container}>
      <Animated.View style={[styles.mapViewport, { opacity: fadeAnim }]}>
        <MapWindowFrame title={locationName} style={styles.mapWindow}>
          {/* canvasRef lets us attach a native DOM listener on Web */}
          <View
            style={[
              styles.canvas,
              onPress && Platform.OS === 'web' && { cursor: 'crosshair' },
            ]}
            onClick={onPress && Platform.OS === 'web' ? (e) => {
              const rect = e.currentTarget.getBoundingClientRect();
              const canvasX = e.clientX - rect.left;
              const canvasY = e.clientY - rect.top;

              // Subtract camera offset to get pixel position inside the map image
              const mapPixelX = canvasX - cameraPosRef.current.x;
              const mapPixelY = canvasY - cameraPosRef.current.y;

              const imgW = Math.max(currentImgW, 1);
              const imgH = Math.max(currentImgH, 1);
              const mW = Math.max(mapWidth, 1);
              const mH = Math.max(mapHeight, 1);

              const gameX = Math.round((mapPixelX / imgW) * mW);
              const gameY = Math.round((mapPixelY / imgH) * mH);

              console.log('[AdminMap] canvas click → canvasXY:', canvasX, canvasY,
                '| cameraOffset:', cameraPosRef.current.x, cameraPosRef.current.y,
                '| mapPixel:', mapPixelX, mapPixelY,
                '| gameCoords:', gameX, gameY);

              onPress(gameX, gameY);
            } : undefined}
          >
            <Animated.View style={{
              position: 'absolute',
              left: cameraPos.x,
              top: cameraPos.y,
              width: imgW,
              height: imgH,
            }}>
              <ImageBackground
                source={currentMap}
                style={{ width: '100%', height: '100%' }}
                resizeMode="stretch"
              >
                <View style={styles.worldOverlay}>
                  {portals.map((portal, index) => (
                    <View
                      key={`portal-${index}`}
                      style={[
                        styles.portalContainer,
                        {
                          left: `${(portal.sourceX / Math.max(mapWidth, 1)) * 100}%`,
                          top: `${(portal.sourceY / Math.max(mapHeight, 1)) * 100}%`,
                        },
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
                          top: `${(Number(creature.y) / Math.max(mapHeight, 1)) * 100}%`,
                        },
                      ]}
                    >
                      <Image 
                        source={getMonsterIcon(creature.iconUrl)} 
                        style={styles.creatureIcon}
                        resizeMode="cover"
                      />
                    </View>
                  ))}

                  <Animated.View style={[
                    styles.agentMarker,
                    {
                      left: agentPosPerc.x.interpolate({ inputRange: [0, 100], outputRange: ['0%', '100%'] }),
                      top: agentPosPerc.y.interpolate({ inputRange: [0, 100], outputRange: ['0%', '100%'] }),
                    },
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
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'flex-end',
    position: 'relative',
  },
  mapViewport: {
    width: 803,
    height: 844,
    justifyContent: 'center',
    alignItems: 'center',
    position: 'relative',
  },
  mapWindow: {
    width: 803,
    height: 844,
    borderRadius: 8,
  },
  canvas: {
    width: 800,
    height: 800,
    overflow: 'hidden',
    position: 'relative',
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
    width: 40,
    height: 40,
    marginLeft: -20,
    marginTop: -20,
    borderRadius: 20,
    backgroundColor: '#dc2626',
    borderColor: '#fca5a5',
    borderWidth: 2,
    overflow: 'hidden',
    shadowColor: '#dc2626',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 1.0,
    shadowRadius: 8,
    zIndex: 90,
    elevation: 5,
  },
  creatureIcon: {
    width: '100%',
    height: '100%',
    borderRadius: 12,
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
});
