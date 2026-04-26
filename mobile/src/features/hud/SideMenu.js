import React from 'react';
import { View, StyleSheet, Text, TouchableOpacity, ScrollView } from 'react-native';
import { theme } from '../../theme/theme';
import { 
  ChevronLeft,
  MessageSquare, 
  Mic 
} from 'lucide-react-native';
import { NAV_ITEMS } from './navConfig';

const NavItem = ({ icon: Icon, label, color, active, onPress }) => (
  <TouchableOpacity 
    style={[styles.item, active && styles.activeItem]} 
    activeOpacity={0.7}
    onPress={onPress}
  >
    {active && <View style={styles.activeIndicator} />}
    <View style={styles.iconContainer}>
      <Icon size={24} color={active ? theme.colors.accent : color} />
    </View>
    <Text style={[styles.label, active && styles.activeLabel]}>{label}</Text>
  </TouchableOpacity>
);

export const SideMenu = ({ activeTab, onTabChange }) => {
  return (
    <View style={styles.sidebar}>
      {/* TOP ACTION: BACK */}
      <TouchableOpacity style={styles.topBtn}>
        <ChevronLeft size={28} color={theme.colors.text.secondary} />
      </TouchableOpacity>

      <View style={styles.divider} />

      {/* MIDDLE: MAIN NAV */}
      <ScrollView contentContainerStyle={styles.navContainer} showsVerticalScrollIndicator={false}>
        {NAV_ITEMS.map((item) => (
          <React.Fragment key={item.id}>
             <NavItem 
               {...item} 
               active={activeTab === item.id}
               onPress={() => onTabChange && onTabChange(item.id)}
             />
             <View style={styles.divider} />
          </React.Fragment>
        ))}
      </ScrollView>

      {/* BOTTOM ACTIONS */}
      <View style={styles.bottomSection}>
        <TouchableOpacity style={styles.actionBtn}>
          <MessageSquare size={22} color={theme.colors.text.secondary} />
        </TouchableOpacity>
        <TouchableOpacity style={styles.actionBtn}>
          <Mic size={22} color={theme.colors.text.secondary} />
          <View style={styles.sparkle} /> 
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  sidebar: {
    width: 85,
    backgroundColor: 'rgba(15, 15, 18, 0.9)',
    borderLeftWidth: 1,
    borderLeftColor: 'rgba(255, 255, 255, 0.1)',
    height: '100%',
    alignItems: 'center',
    paddingVertical: 10,
  },
  topBtn: {
    padding: 10,
    marginBottom: 5,
  },
  navContainer: {
    alignItems: 'center',
    paddingVertical: 5,
  },
  item: {
    width: 85,
    height: 80,
    justifyContent: 'center',
    alignItems: 'center',
    position: 'relative',
    paddingVertical: 10,
  },
  activeItem: {
    backgroundColor: 'rgba(246, 173, 85, 0.05)',
  },
  activeIndicator: {
    position: 'absolute',
    left: 0,
    top: '20%',
    bottom: '20%',
    width: 3,
    backgroundColor: theme.colors.accent,
    borderTopRightRadius: 2,
    borderBottomRightRadius: 2,
    shadowColor: theme.colors.accent,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.8,
    shadowRadius: 4,
  },
  iconContainer: {
    marginBottom: 4,
  },
  label: {
    color: theme.colors.text.muted,
    fontSize: 10,
    fontWeight: '500',
    textAlign: 'center',
  },
  activeLabel: {
    color: theme.colors.accent,
    fontWeight: 'bold',
  },
  divider: {
    width: '60%',
    height: 1,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
  },
  bottomSection: {
    marginTop: 'auto',
    width: '100%',
    alignItems: 'center',
    gap: 15,
    paddingTop: 15,
  },
  actionBtn: {
    padding: 10,
    position: 'relative',
  },
  sparkle: {
    position: 'absolute',
    top: -5,
    left: -5,
    // Note: In real app we might use an image or custom vector for that star/sparkle
  }
});
