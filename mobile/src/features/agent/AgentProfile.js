import React from 'react';
import { View, Text, Image, StyleSheet } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { theme } from '../../theme/theme';
import { GothicBlendedImage } from '../../components/ui/GothicBlendedImage';

/**
 * AGENT PROFILE - DATA-DRIVEN SHELL
 * 
 * Uses 'agent-szablon.png' as the high-fidelity skeleton.
 * Positioning is driven by hudConfig.json.
 */
export const AgentProfile = ({
  name = "Shadow-01", 
  level = 1, 
  hp = 100, 
  maxHp = 100,
  stamina = 100, 
  maxStamina = 100, 
  status = "IDLE", 
  x = 0, 
  y = 0,
  currentAction,
  hudConfig
}) => {
  // Safe defaults
  const dimensions = hudConfig || { 
    WIDTH: 460, 
    ASPECT_RATIO: 2.044,
    SKELETON: {
      AVATAR: { left: '6.8%', top: '15.3%', width: '30.6%', height: '62.5%' },
      LEVEL:  { left: '7.35%', top: '63.2%', width: '9.1%', height: '18.5%' },
      HP:     { left: '44.2%', top: '46.4%', width: '42.8%', height: '10.3%' },
      MANA:   { left: '43.7%', top: '63.3%', width: '40.1%', height: '9.6%' },
      STATUS: { left: '68.6%', top: '19.6%', width: '23%', height: '18.8%' },
      NAME:   { left: '15%', top: '-20%' }
    }
  };

  const { SKELETON } = dimensions;

  // Calculate percentages for fills
  const hpPercent = Math.min(100, Math.max(0, (hp / maxHp) * 100));
  const staminaPercent = Math.min(100, Math.max(0, (stamina / maxStamina) * 100));

  return (
    <View style={styles.rootContainer}>
      {/* AGENT NAME & COORDS */}
      <View style={[styles.nameHeader, SKELETON.NAME || { left: '15%', top: '-10%' }]}>
        <Text style={styles.nameText}>{name.toUpperCase()}</Text>
        <Text style={styles.coordText}>{x}, {y}</Text>
      </View>

      {/* MAIN HUD SHELL */}
      <View style={[styles.hudShell, { width: dimensions.WIDTH, aspectRatio: dimensions.ASPECT_RATIO }]}>
        
        {/* LAYER 1: BACKGROUNDS */}
        
        {/* AVATAR (must be under the frame) */}
        <View style={[styles.absolute, SKELETON.AVATAR, { backgroundColor: '#000', overflow: 'hidden', borderRadius: 999 }]}>
          <Image 
            source={require('../../../assets/knight_avatar.png')} 
            style={styles.fullSize} 
            resizeMode="cover"
          />
        </View>

        {/* LAYER 2: DYNAMIC FILLS (Sandwiched for realism) */}
        
        {/* HP BAR (Vibrant & Glowing) */}
        <View style={[styles.absolute, SKELETON.HP, styles.barBackdrop]}>
          <LinearGradient
            colors={['#8b0000', '#ff0000', '#ff4500']} // Ultra vibrant red-glow
            start={{ x: 0, y: 0.5 }} end={{ x: 1, y: 0.5 }}
            style={[styles.fill, { width: `${hpPercent}%` }]}
          />
        </View>

        {/* MANA BAR (Vibrant & Glowing) */}
        <View style={[styles.absolute, SKELETON.MANA, styles.barBackdrop]}>
          <LinearGradient
            colors={['#00008b', '#0000ff', '#00bfff']} // Ultra vibrant blue-glow
            start={{ x: 0, y: 0.5 }} end={{ x: 1, y: 0.5 }}
            style={[styles.fill, { width: `${staminaPercent}%` }]}
          />
        </View>

        {/* LAYER 3: THE SHELL (Robust and opaque) */}
        <GothicBlendedImage 
          source={require('../../../assets/agent-szablon.png')}
          style={StyleSheet.absoluteFill}
          isOpaque={true}
        />

        {/* LAYER 4: OVERLAYS (Text) */}
        <View style={[styles.absolute, SKELETON.LEVEL, styles.centerAll]}>
          <Text style={styles.levelText}>{level}</Text>
        </View>

        <View style={[styles.absolute, SKELETON.STATUS, styles.centerAll]}>
          <Text style={styles.statusText}>{status.toUpperCase()}</Text>
        </View>

      </View>

      {/* CURRENT ACTION */}
      {currentAction && (
        <Text style={styles.actionText}>{currentAction}</Text>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  rootContainer: {
    alignItems: 'center',
    backgroundColor: 'transparent',
    maxWidth: '100%',
  },
  absolute: { position: 'absolute' },
  fullSize: { width: '100%', height: '100%' },
  centerAll: { justifyContent: 'center', alignItems: 'center' },
  barBackdrop: { 
    backgroundColor: 'rgba(0,0,0,0.8)', 
    overflow: 'hidden', 
    borderRadius: 4,
    // Add inner shadow effect for liquid look
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.5,
    shadowRadius: 3,
  },
  
  nameHeader: {
    flexDirection: 'row',
    alignItems: 'baseline',
    alignSelf: 'flex-start',
    zIndex: 10,
  },
  nameText: {
    color: '#ecc94b',
    fontSize: 20,
    fontFamily: theme.typography.fantasy,
    letterSpacing: 2,
    textShadowColor: 'rgba(0,0,0,1)',
    textShadowOffset: { width: 2, height: 2 },
    textShadowRadius: 4,
  },
  coordText: {
    color: '#718096',
    fontSize: 12,
    marginLeft: 10,
    fontFamily: theme.typography.bold,
  },
  hudShell: {
    position: 'relative',
  },
  fill: { 
    height: '100%',
    // High-fidelity glow using opacity
    opacity: 0.9,
  },
  levelText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '900',
    fontFamily: theme.typography.fantasy,
    textShadowColor: '#000',
    textShadowOffset: { width: 1, height: 1 },
    textShadowRadius: 3,
  },
  statusText: {
    color: '#cbd5e0',
    fontSize: 14,
    fontFamily: theme.typography.bold,
    letterSpacing: 1,
    textShadowColor: '#000',
    textShadowOffset: { width: 1, height: 1 },
    textShadowRadius: 2,
  },
  actionText: {
    color: '#4fd1ed',
    fontSize: 12,
    fontStyle: 'italic',
    marginTop: -5,
  }
});
