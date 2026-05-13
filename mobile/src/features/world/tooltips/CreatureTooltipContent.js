import React from 'react';
import { View, StyleSheet, Text, Image } from 'react-native';
import { theme } from '../../../theme/theme';

const RANK_COLORS = {
  NORMAL: '#a0aec0',
  ELITE: '#f6ad55',
  BOSS: '#fc8181',
};

const RANK_LABELS = {
  NORMAL: 'Normal',
  ELITE: '★ Elite',
  BOSS: '★★★ Boss',
};

export const CreatureTooltipContent = ({ data }) => {
  const rankColor = RANK_COLORS[data.rank] || RANK_COLORS.NORMAL;
  const rankLabel = RANK_LABELS[data.rank] || 'Unknown';
  const hpPercent = data.maxHp > 0 ? (data.currentHp / data.maxHp) * 100 : 100;

  const isDecorative = data.name?.includes('Tree') || data.name?.includes('Rock');

  return (
    <View>
      {/* Header: Name + Rank */}
      <Text style={[styles.name, { color: rankColor }]}>{data.name}</Text>
      <View style={styles.metaRow}>
        <Text style={[styles.rank, { color: rankColor }]}>{rankLabel}</Text>
        <Text style={styles.level}>Lv. {data.level || 1}</Text>
      </View>

      {/* HP Bar */}
      {!isDecorative && (
        <View style={styles.hpSection}>
          <View style={styles.hpBarBg}>
            <View style={[styles.hpBarFill, { width: `${hpPercent}%` }]} />
          </View>
          <Text style={styles.hpText}>
            {data.currentHp} / {data.maxHp}
          </Text>
        </View>
      )}

      {/* Coordinates */}
      <Text style={styles.coords}>
        Position: ({data.x}, {data.y})
      </Text>
    </View>
  );
};

const styles = StyleSheet.create({
  name: {
    fontSize: 13,
    fontWeight: '900',
    fontFamily: theme.typography.fantasy,
    letterSpacing: 0.5,
  },
  metaRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 2,
    marginBottom: 6,
  },
  rank: {
    fontSize: 10,
    fontWeight: 'bold',
  },
  level: {
    fontSize: 10,
    color: '#cbd5e0',
    fontWeight: 'bold',
  },
  hpSection: {
    marginBottom: 4,
  },
  hpBarBg: {
    height: 6,
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    borderRadius: 3,
    overflow: 'hidden',
  },
  hpBarFill: {
    height: '100%',
    backgroundColor: '#48bb78',
    borderRadius: 3,
  },
  hpText: {
    fontSize: 9,
    color: '#a0aec0',
    textAlign: 'right',
    marginTop: 2,
  },
  coords: {
    fontSize: 9,
    color: '#4a5568',
    marginTop: 4,
  },
});
