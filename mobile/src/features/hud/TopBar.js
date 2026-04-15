import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { theme } from '../../theme/theme';
import { Settings, Coins, Diamond, Menu } from 'lucide-react-native';
import { LinearGradient } from 'expo-linear-gradient';

export const TopBar = ({ gold, gems, locationName = "World" }) => {
  return (
    <View style={styles.safeArea}>
      <View style={styles.container}>
        
        {/* LEFT: MENU BUTTON */}
        <TouchableOpacity style={styles.menuButton}>
          <Menu size={18} color={theme.colors.primary} />
          <Text style={styles.menuText}>MENU</Text>
        </TouchableOpacity>

        {/* CENTER: TITLE PLATE */}
        <View style={styles.titlePlateContainer}>
          <LinearGradient
            colors={theme.gradients.header}
            style={styles.titlePlate}
          >
            <Text style={styles.title}>LOCATION: {locationName.toUpperCase()}</Text>
          </LinearGradient>
          <View style={styles.plateBorder} />
        </View>

        {/* RIGHT: CURRENCY & SETTINGS */}
        <View style={styles.rightSection}>
          <View style={styles.currencyBlock}>
            <View style={styles.currencyItem}>
              <Coins size={14} color={theme.colors.gold} />
              <Text style={styles.currencyValue}>{gold}</Text>
            </View>
            <View style={styles.currencyItem}>
              <Diamond size={14} color={theme.colors.gem} />
              <Text style={styles.currencyValue}>{gems}</Text>
            </View>
          </View>
          <TouchableOpacity style={styles.settingsBtn}>
            <Settings size={18} color={theme.colors.text.secondary} />
          </TouchableOpacity>
        </View>

      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  safeArea: {
    width: '100%',
    paddingHorizontal: 10,
    paddingTop: 5,
  },
  container: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    height: 50,
  },
  menuButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(26, 54, 93, 0.8)',
    paddingHorizontal: 15,
    paddingVertical: 8,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: theme.colors.border.cyan,
    gap: 8,
  },
  menuText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '900',
    letterSpacing: 1,
  },
  titlePlateContainer: {
    position: 'absolute',
    left: '30%',
    right: '30%',
    alignItems: 'center',
  },
  titlePlate: {
    paddingHorizontal: 40,
    paddingVertical: 8,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: theme.colors.border.gold,
    minWidth: 200,
    alignItems: 'center',
  },
  title: {
    color: theme.colors.text.gold,
    fontSize: 16,
    fontWeight: '900',
    letterSpacing: 2,
    textShadowColor: 'rgba(0,0,0,0.5)',
    textShadowRadius: 3,
  },
  rightSection: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  currencyBlock: {
    flexDirection: 'row',
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
    gap: 15,
  },
  currencyItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  currencyValue: {
    color: '#fff',
    fontSize: 13,
    fontWeight: 'bold',
  },
  settingsBtn: {
    backgroundColor: 'rgba(26, 54, 93, 0.8)',
    padding: 8,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.2)',
  },
});
