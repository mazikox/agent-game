import React from 'react';
import { View, Text, Image, StyleSheet } from 'react-native';
import { theme } from '../../theme/theme';
import { StatBox } from '../../components/common/StatBox';
import { Shield, Sword, MapPin, Compass } from 'lucide-react-native';

export const AgentProfile = ({ name, level, hp, maxHp, experience, expThreshold, atk, def, x, y, locationName, goal }) => {
  const hpProgress = (hp / maxHp) * 100;
  const expProgress = expThreshold > 0 ? Math.floor((experience / expThreshold) * 100) : 0;

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <View style={styles.avatarBorder}>
          <Image 
            source={require('../../../assets/knight_avatar.png')} 
            style={styles.avatar}
          />
        </View>
        <View style={styles.info}>
          <View style={styles.nameRow}>
            <Text style={styles.name}>{name}</Text>
            <View style={styles.levelBadge}>
              <Text style={styles.levelText}>LVL {level}</Text>
            </View>
          </View>
          <View style={styles.locationBadge}>
            <MapPin size={10} color={theme.colors.accent} />
            <Text style={styles.locationText}>{locationName || 'Wczytywanie...'} [ {x}, {y} ]</Text>
          </View>
          <View style={styles.hpBarContainer}>
            <View style={[styles.hpBar, { width: `${hpProgress}%` }]} />
            <Text style={styles.hpText}>{hp} / {maxHp}</Text>
          </View>
        </View>
      </View>
      
      {goal && (
        <View style={styles.goalContainer}>
          <Compass size={12} color={theme.colors.primary} />
          <Text style={styles.goalText} numberOfLines={1}>CEL: {goal}</Text>
        </View>
      )}

      <View style={styles.statsRow}>
        <StatBox label="ATK" value={atk || 10} color={theme.colors.danger} />
        <StatBox label="DEF" value={def || 5} color={theme.colors.primary} />
        <StatBox label="EXP" value={`${expProgress}%`} color={theme.colors.success} />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: theme.colors.surface,
    borderRadius: theme.borderRadius.lg,
    padding: theme.spacing.md,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
    marginHorizontal: theme.spacing.md,
    marginTop: 20,
  },
  header: {
    flexDirection: 'row',
    marginBottom: theme.spacing.sm,
  },
  avatarBorder: {
    padding: 2,
    borderRadius: theme.borderRadius.lg,
    borderWidth: 2,
    borderColor: theme.colors.accent,
  },
  avatar: {
    width: 64,
    height: 64,
    borderRadius: theme.borderRadius.lg - 2,
  },
  info: {
    flex: 1,
    marginLeft: theme.spacing.md,
    justifyContent: 'center',
  },
  name: {
    color: theme.colors.text.primary,
    fontSize: 18,
    fontWeight: 'bold',
  },
  locationBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 4,
  },
  locationText: {
    color: theme.colors.text.muted,
    fontSize: 10,
    fontWeight: 'bold',
    marginLeft: 4,
  },
  hpBarContainer: {
    height: 12,
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    borderRadius: 6,
    overflow: 'hidden',
    justifyContent: 'center',
  },
  hpBar: {
    height: '100%',
    backgroundColor: theme.colors.danger,
    position: 'absolute',
  },
  hpText: {
    color: 'white',
    fontSize: 8,
    fontWeight: 'bold',
    textAlign: 'center',
  },
  goalContainer: {
    flexDirection: 'row',
    backgroundColor: 'rgba(99, 102, 241, 0.1)',
    padding: 6,
    borderRadius: theme.borderRadius.sm,
    marginBottom: theme.spacing.md,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'rgba(99, 102, 241, 0.2)',
  },
  goalText: {
    color: theme.colors.primary,
    fontSize: 10,
    fontWeight: 'bold',
    marginLeft: 6,
    textTransform: 'uppercase',
  },
  statsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: theme.spacing.sm,
  },
  nameRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: theme.spacing.sm,
    marginBottom: 4,
  },
  levelBadge: {
    backgroundColor: theme.colors.accent,
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  levelText: {
    color: theme.colors.background,
    fontSize: 10,
    fontWeight: 'bold',
  },
});
