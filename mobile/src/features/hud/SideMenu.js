import React from 'react';
import { View, StyleSheet, Text, TouchableOpacity } from 'react-native';
import { theme } from '../../theme/theme';
import { 
  Briefcase, 
  Zap, 
  Map as MapIcon, 
  ShoppingBag, 
  Sword, 
  Globe, 
  MessageSquare, 
  Mic 
} from 'lucide-react-native';

const GridItem = ({ icon: Icon, label, color = theme.colors.primary }) => (
  <TouchableOpacity style={styles.gridItem} activeOpacity={0.7}>
    <View style={styles.iconWrapper}>
      <Icon size={20} color={color} />
    </View>
    <Text style={styles.label}>{label}</Text>
  </TouchableOpacity>
);

export const SideMenu = () => {
  return (
    <View style={styles.container}>
      {/* 2-COLUMN GRID */}
      <View style={styles.grid}>
        <GridItem icon={Briefcase} label="INVENTORY" />
        <GridItem icon={Zap} label="SKILLS" color={theme.colors.gem} />
        <GridItem icon={Sword} label="COMBAT" />
        <GridItem icon={Globe} label="MAP" color={theme.colors.gem} />
        <GridItem icon={ShoppingBag} label="SHOP" color={theme.colors.accent} />
        <GridItem icon={MapIcon} label="QUESTS" />
      </View>

      {/* BOTTOM ACTIONS */}
      <View style={styles.bottomActions}>
         <TouchableOpacity style={styles.actionBtn}>
            <MessageSquare size={18} color={theme.colors.text.secondary} />
         </TouchableOpacity>
         <TouchableOpacity style={styles.actionBtn}>
            <Mic size={18} color={theme.colors.text.secondary} />
         </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    width: 160,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: 8,
  },
  gridItem: {
    width: 70,
    height: 70,
    backgroundColor: 'rgba(26, 54, 93, 0.4)',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(79, 209, 237, 0.3)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 5,
  },
  iconWrapper: {
    marginBottom: 4,
  },
  label: {
    color: theme.colors.text.secondary,
    fontSize: 7,
    fontWeight: '900',
    textAlign: 'center',
    letterSpacing: 0.5,
  },
  bottomActions: {
    marginTop: 20,
    flexDirection: 'column',
    gap: 10,
  },
  actionBtn: {
    width: 48,
    height: 48,
    borderRadius: 8,
    backgroundColor: 'rgba(0, 0, 0, 0.6)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
    justifyContent: 'center',
    alignItems: 'center',
  }
});
