import React from 'react';
import { View, Text, ImageBackground, StyleSheet } from 'react-native';
import { theme } from '../../theme/theme';
import { MapPin } from 'lucide-react-native';
import { LinearGradient } from 'expo-linear-gradient';

export const LocationHero = ({ locationName, regionName }) => {
  return (
    <View style={styles.container}>
      <ImageBackground
        source={require('../../../assets/dark_cave_bg.png')}
        style={styles.background}
        imageStyle={styles.imageStyle}
      >
        <LinearGradient
          colors={['transparent', 'rgba(10, 10, 11, 0.8)', theme.colors.background]}
          style={styles.gradient}
        >
          <View style={styles.content}>
            <View style={styles.badge}>
              <MapPin size={10} color={theme.colors.accent} />
              <Text style={styles.regionText}>{regionName}</Text>
            </View>
            <Text style={styles.locationTitle}>{locationName}</Text>
          </View>
        </LinearGradient>
      </ImageBackground>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    height: 240,
    marginVertical: theme.spacing.lg,
    overflow: 'hidden',
  },
  background: {
    flex: 1,
    justifyContent: 'flex-end',
  },
  imageStyle: {
    opacity: 0.7,
  },
  gradient: {
    padding: theme.spacing.md,
    height: '60%',
    justifyContent: 'flex-end',
  },
  content: {
    alignItems: 'center',
  },
  badge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(245, 158, 11, 0.1)',
    paddingHorizontal: theme.spacing.sm,
    paddingVertical: 2,
    borderRadius: theme.borderRadius.full,
    marginBottom: theme.spacing.xs,
    borderWidth: 1,
    borderColor: 'rgba(245, 158, 11, 0.3)',
  },
  regionText: {
    color: theme.colors.accent,
    fontSize: 10,
    fontWeight: 'bold',
    marginLeft: 4,
    textTransform: 'uppercase',
  },
  locationTitle: {
    color: theme.colors.text.primary,
    fontSize: 28,
    fontWeight: '900',
    letterSpacing: 1,
    textShadowColor: 'rgba(0, 0, 0, 0.75)',
    textShadowOffset: { width: 0, height: 2 },
    textShadowRadius: 4,
  },
});
