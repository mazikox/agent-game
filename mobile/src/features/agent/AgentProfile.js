import React from 'react';
import { View, Text, Image, StyleSheet } from 'react-native';
import { theme } from '../../theme/theme';
import { LinearGradient } from 'expo-linear-gradient';

export const AgentProfile = ({ 
  name = "Shadow-01", 
  level = 1, 
  hp = 100, 
  maxHp = 100, 
  stamina = 100, 
  maxStamina = 100,
  currentAction = "Waiting for orders...",
  status = "IDLE",
  x = 0,
  y = 0,
  mapWidth = 0,
  mapHeight = 0,
  currentTask = ""
}) => {
  return (
    <View style={styles.container}>
      
      {/* AVATAR CIRCLE - Overlapping */}
      <View style={styles.avatarWrapper}>
        <LinearGradient
          colors={[theme.colors.border.cyan, theme.colors.border.gold]}
          start={{x: 0, y: 0}}
          end={{x: 1, y: 1}}
          style={styles.avatarGlow}
        >
          <View style={styles.avatarInner}>
            <Image 
              source={require('../../../assets/knight_avatar.png')} 
              style={styles.avatar}
            />
          </View>
        </LinearGradient>
      </View>

      {/* STATS PANEL - To the right */}
      <View style={styles.statsPanel}>
        <View style={styles.headerRow}>
          <View>
            <Text style={styles.name}>{name}</Text>
            <Text style={styles.level}>Lv. {level} • {x},{y} ({mapWidth}x{mapHeight})</Text>
          </View>
          <View style={[styles.statusBadge, { backgroundColor: status === 'MOVING' ? '#4CAF50' : '#FFC107' }]}>
            <Text style={styles.statusText}>{status}</Text>
          </View>
        </View>

        {/* HP BAR */}
        <View style={styles.barRow}>
          <Text style={styles.barLabel}>HP</Text>
          <View style={styles.barContainer}>
            <LinearGradient
              colors={theme.gradients.hp}
              start={{x: 0, y: 0}}
              end={{x: 1, y: 0}}
              style={[styles.barFill, { width: `${(hp/maxHp) * 100}%` }]}
            />
            <Text style={styles.barValue}>{hp}/{maxHp}</Text>
          </View>
        </View>

        {/* CURRENT ACTION / THOUGHTS */}
        <View style={styles.actionBox}>
          <Text style={styles.actionLabel}>SYSTEM INTENT: {currentTask || 'EXPLORING'}</Text>
          <Text style={styles.actionText} numberOfLines={2}>
            {"> "}{currentAction}
          </Text>
        </View>
      </View>

    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    marginLeft: 0,
    marginTop: -25, // Overlap effect with header
  },
  avatarWrapper: {
    zIndex: 10,
  },
  avatarGlow: {
    width: 120,
    height: 120,
    borderRadius: 60,
    padding: 3,
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: theme.colors.primary,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.8,
    shadowRadius: 10,
    elevation: 15,
  },
  avatarInner: {
    width: 114,
    height: 114,
    borderRadius: 57,
    backgroundColor: '#000',
    overflow: 'hidden',
    borderWidth: 2,
    borderColor: 'rgba(0,0,0,0.8)',
  },
  avatar: {
    width: '100%',
    height: '100%',
  },
  statsPanel: {
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    paddingLeft: 65, // Room for overlapping avatar
    paddingRight: 20,
    paddingVertical: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.15)',
    marginLeft: -55, // Shift left to overlap
    minWidth: 260,
    marginTop: 20,
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 8,
  },
  statusBadge: {
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.2)',
  },
  statusText: {
    color: '#fff',
    fontSize: 8,
    fontWeight: '900',
  },
  name: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '900',
    textShadowColor: 'rgba(0,0,0,0.8)',
    textShadowRadius: 2,
    letterSpacing: 0.5,
  },
  level: {
    color: theme.colors.text.secondary,
    fontSize: 11,
    fontWeight: '900',
    textTransform: 'uppercase',
  },
  barRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 6,
    gap: 8,
  },
  barLabel: {
    color: '#fff',
    fontSize: 8,
    fontWeight: '900',
    width: 45,
  },
  barContainer: {
    flex: 1,
    height: 12,
    backgroundColor: '#000',
    borderRadius: 2,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
    overflow: 'hidden',
    justifyContent: 'center',
  },
  barFill: {
    height: '100%',
  },
  barValue: {
    position: 'absolute',
    width: '100%',
    textAlign: 'center',
    color: '#fff',
    fontSize: 8,
    fontWeight: '900',
    textShadowColor: '#000',
    textShadowRadius: 1,
  },
  actionBox: {
    marginTop: 10,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    borderRadius: 4,
    padding: 6,
    borderLeftWidth: 2,
    borderLeftColor: theme.colors.accent,
  },
  actionLabel: {
    color: theme.colors.accent,
    fontSize: 7,
    fontWeight: '900',
    marginBottom: 2,
  },
  actionText: {
    color: '#fff',
    fontSize: 10,
    fontWeight: '500',
    fontStyle: 'italic',
  },
});
