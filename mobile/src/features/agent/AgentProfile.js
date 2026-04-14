import React from 'react';
import { View, Text, Image, StyleSheet, Platform } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { theme } from '../../theme/theme';

/**
 * HUD LAYOUT CONFIGURATION
 * Centralized constants for pixel-perfect adjustments.
 */
const HUD_CONFIG = {
  PORTRAIT: {
    SIZE: 115,
    BORDER: 5,
    OVERLAP_X: -25, // How much the portrait overlaps the bars
  },
  BARS: {
    HEIGHT: 26,
    SPACING: 10,
    ICON_SCALE: 2.2, // Growth factor relative to bar height
    ICON_OFFSET: -15, // Icon "plug" depth into the bar
    SOCKET_PADDING: 32, // Where liquid starts in the frame
  },
  TEXT_SHADOW: {
    textShadowColor: 'rgba(0, 0, 0, 0.9)',
    textShadowOffset: { width: 2, height: 2 },
    textShadowRadius: 6,
  }
};

/**
 * 1. WebBlendedImage Component
 * FIX: Solves the 'Black Square' issue on Web.
 * Standard React Native <Image /> on web wraps <img> in a <div>, 
 * which breaks 'mixBlendMode' for some browser engines.
 * This component uses native CSS backgrounds for perfect blending.
 */
const WebBlendedImage = ({ source, style, resizeMode = 'contain' }) => {
  // Extract URI from require() or URI object
  const imageUri = typeof source === 'number' ? Image.resolveAssetSource(source).uri : source.uri;

  if (Platform.OS === 'web') {
    return (
      <View 
        style={[
          style,
          {
            backgroundImage: `url(${imageUri})`,
            backgroundSize: resizeMode === 'contain' ? 'contain' : 'cover',
            backgroundRepeat: 'no-repeat',
            backgroundPosition: 'center',
            // @ts-ignore
            mixBlendMode: 'screen', // Force screen blending on the image layer
          }
        ]}
      />
    );
  }

  // Fallback for native mobile
  return <Image source={source} style={[style, { mixBlendMode: 'screen' }]} resizeMode={resizeMode} />;
};

/**
 * 2. Portrait Component
 * Encapsulates the circular stone frame and level badge.
 */
const AgentPortrait = ({ level, avatarSource }) => (
  <View style={styles.portraitContainer}>
    <View style={styles.stoneFrame}>
      <View style={styles.avatarMask}>
        <Image source={avatarSource} style={styles.avatarImage} />
      </View>
    </View>
    <View style={styles.levelBadge}>
      <Text style={styles.levelBadgeText}>{level}</Text>
    </View>
  </View>
);

/**
 * 3. GothicStatBar Component
 * Reusable progress bar with deep-overlap icon and liquid glass effect.
 */
const GothicStatBar = ({ label, value, max, colorBase, colorGlow, iconSource }) => {
  const percentage = Math.min(100, Math.max(0, (value / max) * 100));
  const iconSize = HUD_CONFIG.BARS.HEIGHT * HUD_CONFIG.BARS.ICON_SCALE;

  return (
    <View style={[styles.barRow, { height: iconSize }]}>
      
      {/* BACKGROUND FRAME LAYER */}
      <View style={styles.barFrameWrapper}>
        <WebBlendedImage 
          source={require('../../../assets/ui/frame_v2.png')}
          style={styles.fullAbsolute}
          resizeMode="stretch"
        />
        
        {/* LIQUID CONTENT */}
        <View style={styles.liquidTubeContainer}>
          <View style={[styles.liquidFill, { width: `${percentage}%` }]}>
            <LinearGradient
              colors={['#000', colorBase, colorGlow]}
              start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }}
              style={styles.fullAbsolute}
            >
              {/* Texture Layer */}
              <WebBlendedImage 
                source={require('../../../assets/ui/texture_liquid.png')}
                style={[styles.fullAbsolute, { opacity: 0.3 }]}
                resizeMode="cover"
              />
            </LinearGradient>
          </View>
          
          {/* Glass Gloss Overlay */}
          <View style={styles.glassShine} />
          
          <Text style={[styles.barValueText, HUD_CONFIG.TEXT_SHADOW]}>
            {value} / {max}
          </Text>
        </View>
      </View>

      {/* SOCKETED ICON - Deep Overlap */}
      <View style={[styles.iconHarness, { width: iconSize, height: iconSize }]}>
        {/* Local dark spot to ground the blend */}
        <View style={styles.iconBacklight} />
        <WebBlendedImage source={iconSource} style={styles.iconImage} />
      </View>

    </View>
  );
};

/**
 * MAIN: AgentProfile (The HUD Orchestrator)
 */
export const AgentProfile = ({ 
  name = "Shadow-01", level = 1, hp = 100, maxHp = 100, 
  stamina = 100, maxStamina = 100, status = "IDLE", x = 0, y = 0 
}) => {
  return (
    <View style={styles.rootContainer}>
      
      {/* 1. AGENT PORTRAIT */}
      <AgentPortrait 
        level={level} 
        avatarSource={require('../../../assets/knight_avatar.png')} 
      />

      {/* 2. FLOATING INFO GROUP */}
      <View style={styles.infoWrapper}>
        
        {/* STRUCTURAL FILIGREE (Visually anchors the bars) */}
        <WebBlendedImage 
          source={require('../../../assets/ui/ornament_v2.png')}
          style={styles.ornamentBackground}
          resizeMode="cover"
        />

        {/* HEADER: Name & Status */}
        <View style={styles.header}>
          <Text style={[styles.nameText, HUD_CONFIG.TEXT_SHADOW]}>{name.toUpperCase()}</Text>
          <View style={styles.statusRow}>
            <Text style={[styles.statusText, HUD_CONFIG.TEXT_SHADOW]}>{status}</Text>
            <View style={styles.dotSeparator} />
            <Text style={[styles.coordText, HUD_CONFIG.TEXT_SHADOW]}>{x}, {y}</Text>
          </View>
        </View>

        {/* STAT BARS */}
        <View style={styles.statsContainer}>
          <GothicStatBar 
            iconSource={require('../../../assets/ui/heart_v2.png')}
            colorBase="#c53030" colorGlow="#f56565"
            value={hp} max={maxHp}
          />
          <GothicStatBar 
            iconSource={require('../../../assets/ui/mana_v2.png')}
            colorBase="#2b6cb0" colorGlow="#4299e1"
            value={stamina} max={maxStamina}
          />
        </View>
      </View>

    </View>
  );
};

const styles = StyleSheet.create({
  // UTILS
  fullAbsolute: { ...StyleSheet.absoluteFillObject },

  rootContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 20,
    backgroundColor: 'transparent', // FIX: Ensure no gray squares
  },

  // PORTRAIT STYLES
  portraitContainer: {
    width: HUD_CONFIG.PORTRAIT.SIZE,
    height: HUD_CONFIG.PORTRAIT.SIZE,
    zIndex: 1000,
  },
  stoneFrame: {
    width: '100%',
    height: '100%',
    borderRadius: 999,
    backgroundColor: '#121214',
    borderWidth: HUD_CONFIG.PORTRAIT.BORDER,
    borderColor: '#3a3a3d',
    borderTopColor: '#6a6a6e',
    borderBottomColor: '#000',
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.9,
    shadowRadius: 15,
  },
  avatarMask: {
    width: '88%',
    height: '88%',
    borderRadius: 999,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: 'rgba(236, 201, 75, 0.4)',
  },
  avatarImage: {
    width: '100%',
    height: '100%',
  },
  levelBadge: {
    position: 'absolute',
    bottom: -2,
    right: -2,
    backgroundColor: '#000',
    width: 36,
    height: 36,
    borderRadius: 18,
    borderWidth: 2,
    borderColor: '#ecc94b',
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#000',
    shadowRadius: 10,
    shadowOpacity: 1,
  },
  levelBadgeText: {
    color: '#ecc94b',
    fontSize: 16,
    fontWeight: '900',
    fontFamily: theme.typography.fantasy,
  },

  // INFO WRAPPER
  infoWrapper: {
    marginLeft: HUD_CONFIG.PORTRAIT.OVERLAP_X,
    paddingLeft: 45,
    justifyContent: 'center',
    position: 'relative',
    minWidth: 300,
  },
  ornamentBackground: {
    position: 'absolute',
    left: 20,
    top: -10,
    width: '100%',
    height: '120%',
    opacity: 0.5,
    tintColor: '#cbd5e0',
    zIndex: -1,
  },
  header: {
    marginBottom: 10,
  },
  nameText: {
    color: '#ecc94b',
    fontSize: 26,
    fontFamily: theme.typography.fantasy,
    letterSpacing: 2,
  },
  statusRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: -4,
  },
  statusText: {
    color: '#a0aec0',
    fontSize: 12,
    fontFamily: theme.typography.bold,
  },
  coordText: {
    color: '#718096',
    fontSize: 12,
    fontFamily: theme.typography.bold,
  },
  dotSeparator: {
    width: 4,
    height: 4,
    borderRadius: 2,
    backgroundColor: '#4a5568',
    marginHorizontal: 12,
  },

  // BAR STYLES
  statsContainer: {
    gap: HUD_CONFIG.BARS.SPACING,
  },
  barRow: {
    flexDirection: 'row', 
    alignItems: 'center',
    width: '100%',
  },
  barFrameWrapper: {
    flex: 1,
    height: HUD_CONFIG.BARS.HEIGHT,
    position: 'relative',
    zIndex: 100,
  },
  liquidTubeContainer: {
    ...StyleSheet.absoluteFillObject,
    marginLeft: HUD_CONFIG.BARS.SOCKET_PADDING,
    marginRight: 8,
    marginTop: 4,
    marginBottom: 4,
    backgroundColor: 'rgba(0,0,0,0.6)',
    borderRadius: 10,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.05)',
  },
  liquidFill: {
    height: '100%',
    position: 'relative',
  },
  glassShine: {
    position: 'absolute',
    top: 0,
    left: 0,
    width: '100%',
    height: '40%',
    backgroundColor: 'rgba(255,255,255,0.12)',
  },
  barValueText: {
    position: 'absolute',
    width: '100%',
    textAlign: 'center',
    color: '#fff',
    fontSize: 11,
    fontWeight: 'bold',
    top: 1,
  },

  // ICON STYLES
  iconHarness: {
    position: 'absolute',
    left: HUD_CONFIG.BARS.ICON_OFFSET,
    zIndex: 500,
    justifyContent: 'center',
    alignItems: 'center',
  },
  iconBacklight: {
    position: 'absolute',
    width: '70%',
    height: '70%',
    backgroundColor: '#000',
    borderRadius: 999,
  },
  iconImage: {
    width: '100%',
    height: '100%',
  }
});
