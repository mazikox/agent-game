import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { theme } from '../../theme/theme';

export const StatBox = ({ label, value, color }) => (
  <View style={styles.container}>
    <Text style={styles.label}>{label}</Text>
    <Text style={[styles.value, { color: color || theme.colors.text.primary }]}>{value}</Text>
  </View>
);

const styles = StyleSheet.create({
  container: {
    backgroundColor: 'rgba(255, 255, 255, 0.03)',
    borderRadius: theme.borderRadius.md,
    padding: theme.spacing.sm,
    minWidth: 80,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  label: {
    color: theme.colors.text.muted,
    fontSize: 10,
    textTransform: 'uppercase',
    fontWeight: '700',
    marginBottom: 2,
  },
  value: {
    fontSize: 16,
    fontWeight: 'bold',
  },
});
