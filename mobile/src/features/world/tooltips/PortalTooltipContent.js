import React from 'react';
import { View, StyleSheet, Text } from 'react-native';
import { theme } from '../../../theme/theme';

export const PortalTooltipContent = ({ data }) => {
  return (
    <View>
      <Text style={styles.title}>⬡ Portal</Text>
      <Text style={styles.destination}>{data.targetLocationName}</Text>
      <View style={styles.divider} />
      <Text style={styles.coords}>
        Exit: ({data.targetX ?? '?'}, {data.targetY ?? '?'})
      </Text>
    </View>
  );
};

const styles = StyleSheet.create({
  title: {
    fontSize: 11,
    fontWeight: '900',
    color: theme.colors.primary || '#4fd1ed',
    letterSpacing: 1,
    textTransform: 'uppercase',
  },
  destination: {
    fontSize: 13,
    fontWeight: 'bold',
    color: '#fff',
    fontFamily: theme.typography.fantasy,
    marginTop: 2,
  },
  divider: {
    height: 1,
    backgroundColor: 'rgba(79, 209, 237, 0.2)',
    marginVertical: 6,
  },
  coords: {
    fontSize: 9,
    color: '#4a5568',
  },
});
