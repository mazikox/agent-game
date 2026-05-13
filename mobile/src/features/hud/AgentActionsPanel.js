import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { theme } from '../../theme/theme';
import { Crosshair, Map, Pause, Search } from 'lucide-react-native';

const ActionButton = ({ icon: Icon, label, onPress }) => (
  <TouchableOpacity 
    style={styles.item} 
    activeOpacity={0.7}
    onPress={onPress}
  >
    <View style={styles.iconContainer}>
      <Icon size={22} color={theme.colors.text.secondary} />
    </View>
    <Text style={styles.label}>{label}</Text>
  </TouchableOpacity>
);

export const AgentActionsPanel = ({ onApproachEnemy, onAttackEnemy, onStop }) => {
  return (
    <View style={styles.container}>
      {/* Optional: we can remove the title or style it differently if we want to match side menu exactly */}
      
      <ActionButton 
        icon={Search} 
        label="Approach" 
        onPress={onApproachEnemy} 
      />
      <View style={styles.divider} />
      
      <ActionButton 
        icon={Crosshair} 
        label="Attack" 
        onPress={onAttackEnemy} 
      />
      <View style={styles.divider} />
      
      <ActionButton 
        icon={Pause} 
        label="Stop" 
        onPress={onStop} 
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    width: 80,
    backgroundColor: 'rgba(15, 15, 18, 0.9)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
    borderRadius: 8,
    alignItems: 'center',
    paddingVertical: 5,
    overflow: 'hidden',
  },
  item: {
    width: '100%',
    paddingVertical: 12,
    justifyContent: 'center',
    alignItems: 'center',
  },
  iconContainer: {
    marginBottom: 4,
  },
  label: {
    color: theme.colors.text.muted,
    fontSize: 9,
    fontWeight: '500',
    textAlign: 'center',
    textTransform: 'uppercase',
  },
  divider: {
    width: '60%',
    height: 1,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
  }
});
