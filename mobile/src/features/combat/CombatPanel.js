import React from 'react';
import { View, Text, Image, StyleSheet, TouchableOpacity } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { theme } from '../../theme/theme';

export const CombatPanel = ({ 
  agentName = "Shadow-01", 
  enemyName = "Fierce Wolf",
  agentHp = 100,
  agentMaxHp = 100,
  enemyHp = 100,
  enemyMaxHp = 100,
  onCombatAction 
}) => {
  const agentHpPercent = Math.min(100, Math.max(0, (agentHp / agentMaxHp) * 100));
  const enemyHpPercent = Math.min(100, Math.max(0, (enemyHp / enemyMaxHp) * 100));

  return (
    <LinearGradient
      colors={['rgba(30, 10, 10, 0.95)', 'rgba(15, 15, 20, 0.98)']}
      style={styles.combatPanel}
    >
      <View style={styles.avatarsRow}>
        {/* AGENT */}
        <View style={styles.avatarWrapper}>
          <View style={[styles.avatarCircle, { borderColor: '#3182ce' }]}>
            <Image 
              source={require('../../../assets/knight_avatar.png')} 
              style={styles.combatAvatarImg} 
            />
          </View>
          <Text style={styles.combatAvatarName}>{agentName.toUpperCase()}</Text>
          <View style={styles.hpBarContainer}>
            <LinearGradient
              colors={['#8b0000', '#ff0000']}
              start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }}
              style={[styles.hpBarFill, { width: `${agentHpPercent}%` }]}
            />
            <Text style={styles.hpBarText}>{agentHp}/{agentMaxHp}</Text>
          </View>
        </View>

        {/* VS */}
        <View style={styles.vsWrapper}>
          <Text style={styles.vsText}>VS</Text>
        </View>

        {/* ENEMY */}
        <View style={styles.avatarWrapper}>
          <View style={[styles.avatarCircle, { borderColor: '#e53e3e' }]}>
            <Image 
              source={require('../../../assets/monster_avatar.png')} 
              style={styles.combatAvatarImg} 
            />
          </View>
          <Text style={styles.combatAvatarName}>{enemyName.toUpperCase()}</Text>
          <View style={styles.hpBarContainer}>
            <LinearGradient
              colors={['#8b0000', '#ff0000']}
              start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }}
              style={[styles.hpBarFill, { width: `${enemyHpPercent}%` }]}
            />
            <Text style={styles.hpBarText}>{enemyHp}/{enemyMaxHp}</Text>
          </View>
        </View>
      </View>

      {/* ACTIONS */}
      <View style={styles.combatActionsRow}>
        <TouchableOpacity 
          style={[styles.actionButton, { backgroundColor: theme.colors.fantasy?.blood || '#8b0000' }]} 
          onPress={() => onCombatAction('ATTACK')}
        >
          <Text style={styles.actionButtonText}>ATTACK</Text>
        </TouchableOpacity>
        
        <TouchableOpacity 
          style={[styles.actionButton, { backgroundColor: theme.colors.primary }]} 
          onPress={() => onCombatAction('POTION')}
        >
          <Text style={styles.actionButtonText}>POTION</Text>
        </TouchableOpacity>
        
        <TouchableOpacity 
          style={[styles.actionButton, { backgroundColor: theme.colors.secondary }]} 
          onPress={() => onCombatAction('FLEE')}
        >
          <Text style={styles.actionButtonText}>FLEE</Text>
        </TouchableOpacity>
      </View>
    </LinearGradient>
  );
};

const styles = StyleSheet.create({
  combatPanel: {
    width: 320,
    padding: 20,
    borderRadius: 16,
    borderWidth: 1.5,
    borderColor: 'rgba(214, 158, 46, 0.4)', // Gold accent
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.6,
    shadowRadius: 8,
    marginTop: 15,
  },
  avatarsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-around',
    width: '100%',
    marginBottom: 20,
  },
  avatarWrapper: {
    alignItems: 'center',
  },
  avatarCircle: {
    width: 70,
    height: 70,
    borderRadius: 35,
    borderWidth: 2,
    overflow: 'hidden',
    backgroundColor: '#000',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.8,
    shadowRadius: 4,
  },
  combatAvatarImg: {
    width: '100%',
    height: '100%',
    resizeMode: 'cover',
  },
  combatAvatarName: {
    color: '#e2e8f0',
    fontSize: 11,
    fontFamily: theme.typography.bold,
    marginTop: 6,
    letterSpacing: 1,
  },
  hpBarContainer: {
    width: 90,
    height: 14,
    backgroundColor: 'rgba(0, 0, 0, 0.6)',
    borderRadius: 7,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.2)',
    marginTop: 6,
    overflow: 'hidden',
    justifyContent: 'center',
  },
  hpBarFill: {
    height: '100%',
    borderRadius: 6,
  },
  hpBarText: {
    position: 'absolute',
    width: '100%',
    textAlign: 'center',
    color: '#fff',
    fontSize: 9,
    fontFamily: theme.typography.bold,
    textShadowColor: '#000',
    textShadowOffset: { width: 1, height: 1 },
    textShadowRadius: 1,
  },
  vsWrapper: {
    paddingHorizontal: 10,
  },
  vsText: {
    color: '#ecc94b', // Gold
    fontSize: 26,
    fontFamily: theme.typography.fantasy || 'serif',
    fontWeight: '900',
    fontStyle: 'italic',
    textShadowColor: 'rgba(255, 0, 0, 0.8)',
    textShadowOffset: { width: 0, height: 0 },
    textShadowRadius: 10,
  },
  combatActionsRow: {
    flexDirection: 'row',
    gap: 10,
    justifyContent: 'center',
    width: '100%',
  },
  actionButton: {
    backgroundColor: 'rgba(26, 54, 93, 0.9)',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(79, 209, 237, 0.5)',
    minWidth: 85,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.4,
    shadowRadius: 2,
  },
  actionButtonText: {
    color: '#fff',
    fontSize: 11,
    fontFamily: theme.typography.bold,
    letterSpacing: 1.5,
  }
});
