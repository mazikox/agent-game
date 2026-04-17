import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { theme } from '../../theme/theme';
import { LinearGradient } from 'expo-linear-gradient';

/**
 * MapWindowFrame
 * 
 * A textured, ornamental frame for the map viewport.
 * Uses stone and metallic elements to match the Dark Fantasy theme.
 */
export const MapWindowFrame = ({ title, children, style }) => {
  return (
    <View style={[styles.container, style]}>
      {/* HEADER SECTION (PREMIUM GRADIENT TITLE BAR) */}
      <View style={styles.header}>
        <LinearGradient
          colors={['#1a1a1c', '#2d2d30', '#1a1a1c']}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 0 }}
          style={styles.headerGradient}
        >
          <View style={styles.headerOverlay}>
            <View style={styles.headerLine} />
            <View style={styles.titleWrapper}>
              <Text style={styles.titleText}>{title.toUpperCase()}</Text>
            </View>
            <View style={styles.headerLine} />
          </View>
        </LinearGradient>
      </View>

      {/* INNER CONTENT (MAP) */}
      <View style={styles.content}>
        {children}
      </View>

      {/* ORNAMENTAL CORNERS (METALLIC) */}
      <View style={[styles.corner, styles.topLeft]} />
      <View style={[styles.corner, styles.bottomLeft]} />
      
      {/* Right corners hidden for glued look */}
      <View style={[styles.corner, styles.topRight, { display: 'none' }]} />
      <View style={[styles.corner, styles.bottomRight, { display: 'none' }]} />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    position: 'relative',
    backgroundColor: '#0a0a0b',
    borderTopLeftRadius: 8,
    borderBottomLeftRadius: 8,
    padding: 3, 
    paddingRight: 0, // Glue to sidebar
    overflow: 'visible',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 12 },
    shadowOpacity: 0.7,
    shadowRadius: 16,
    elevation: 20,
  },
  header: {
    height: 38,
    borderTopLeftRadius: 8,
    overflow: 'hidden',
    borderBottomWidth: 2,
    borderBottomColor: '#1a1a1a',
    backgroundColor: '#1a1a1c',
  },
  headerGradient: {
    flex: 1,
  },
  headerOverlay: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 15,
    backgroundColor: 'rgba(0,0,0,0.2)', // Subtle darkening
  },
  headerLine: {
    flex: 1,
    height: 1,
    backgroundColor: 'rgba(236, 201, 75, 0.2)', // Gold hint
  },
  titleWrapper: {
    paddingHorizontal: 15,
  },
  titleText: {
    color: '#ecc94b', // GOLD
    fontSize: 14,
    fontFamily: theme.typography.fantasy,
    letterSpacing: 2,
    textShadowColor: 'rgba(0,0,0,1)',
    textShadowOffset: { width: 1, height: 1 },
    textShadowRadius: 3,
  },
  content: {
    flex: 1,
    position: 'relative',
    overflow: 'hidden',
    borderBottomLeftRadius: 6,
    backgroundColor: '#000',
  },
  borderOverlay: {
    ...StyleSheet.absoluteFillObject,
    pointerEvents: 'none',
    opacity: 0.6,
  },
  corner: {
    position: 'absolute',
    width: 10,
    height: 10,
    backgroundColor: '#718096', // Metallic grey
    transform: [{ rotate: '45deg' }],
    zIndex: 20,
    borderWidth: 1,
    borderColor: '#ecc94b', // Gold edging
    shadowColor: '#ecc94b',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.4,
    shadowRadius: 2,
  },
  topLeft: { top: -2, left: -2 },
  bottomLeft: { bottom: -2, left: -2 },
  topRight: { top: -2, right: -2 },
  bottomRight: { bottom: -2, right: -2 },
});
