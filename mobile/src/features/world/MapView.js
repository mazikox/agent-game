import React from 'react';
import { View, StyleSheet, Dimensions, Text } from 'react-native';
import { theme } from '../../theme/theme';
import { User, Atom } from 'lucide-react-native';

const GRID_SIZE = 10; // Number of cells to show

export const MapView = ({ agentX, agentY, mapWidth, mapHeight, portals = [] }) => {
  // Normalize positions to a 10x10 preview grid
  // In a real MMO, this would be a scrollable larger map
  const renderGrid = () => {
    const cells = [];
    for (let i = 0; i < 100; i++) {
      const x = i % 10;
      const y = Math.floor(i / 10);
      
      // Calculate real-world coordinates for this cell 
      // Mapping a 100x100 world to a 10x10 UI grid
      const worldXMin = (x / 10) * mapWidth;
      const worldXMax = ((x + 1) / 10) * mapWidth;
      const worldYMin = (y / 10) * mapHeight;
      const worldYMax = ((y + 1) / 10) * mapHeight;

      const isAgentInCell = agentX >= worldXMin && agentX < worldXMax && 
                            agentY >= worldYMin && agentY < worldYMax;

      const portalInCell = portals.find(p => 
        p.sourceX >= worldXMin && p.sourceX < worldXMax && 
        p.sourceY >= worldYMin && p.sourceY < worldYMax
      );

      cells.push(
        <View key={i} style={styles.cell}>
          {portalInCell && (
            <View style={styles.portalContainer}>
              <Atom size={12} color={theme.colors.primary} />
            </View>
          )}
          {isAgentInCell && (
            <View style={styles.agentMarker}>
              <View style={styles.agentPulse} />
              <User size={14} color={theme.colors.accent} strokeWidth={3} />
            </View>
          )}
        </View>
      );
    }
    return cells;
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>PODGLĄD MAPY ({mapWidth}x{mapHeight})</Text>
      </View>
      <View style={styles.grid}>
        {renderGrid()}
      </View>
      <View style={styles.legend}>
        <View style={styles.legendItem}>
          <View style={[styles.dot, { backgroundColor: theme.colors.accent }]} />
          <Text style={styles.legendText}>TY [ {agentX}, {agentY} ]</Text>
        </View>
        <View style={styles.legendItem}>
          <View style={[styles.dot, { backgroundColor: theme.colors.primary }]} />
          <Text style={styles.legendText}>PORTALE</Text>
        </View>
      </View>
    </View>
  );
};

const { width } = Dimensions.get('window');
const CELL_SIZE = 16; // Stały, mały wymiar komórki (16px * 10 = 160px cała mapa)

const styles = StyleSheet.create({
  container: {
    backgroundColor: theme.colors.surface,
    padding: theme.spacing.md,
    borderRadius: theme.borderRadius.lg,
    marginHorizontal: theme.spacing.md,
    marginBottom: theme.spacing.md,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  header: {
    marginBottom: theme.spacing.sm,
    borderLeftWidth: 3,
    borderLeftColor: theme.colors.accent,
    paddingLeft: 8,
  },
  title: {
    color: theme.colors.text.secondary,
    fontSize: 10,
    fontWeight: 'bold',
    letterSpacing: 1,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    width: CELL_SIZE * 10,
    height: CELL_SIZE * 10,
    backgroundColor: 'rgba(0, 0, 0, 0.2)',
    borderRadius: theme.borderRadius.sm,
    overflow: 'hidden',
    alignSelf: 'center', // Wyśrodkowanie siatki
  },
  cell: {
    width: CELL_SIZE,
    height: CELL_SIZE,
    borderWidth: 0.2,
    borderColor: 'rgba(255, 255, 255, 0.03)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  agentMarker: {
    zIndex: 10,
  },
  agentPulse: {
    position: 'absolute',
    width: 14,
    height: 14,
    borderRadius: 7,
    backgroundColor: 'rgba(245, 158, 11, 0.3)',
    transform: [{ scale: 1.8 }],
  },
  portalContainer: {
    opacity: 0.8,
  },
  legend: {
    flexDirection: 'row',
    marginTop: theme.spacing.md,
    justifyContent: 'space-around',
  },
  legendItem: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: 6,
  },
  legendText: {
    color: theme.colors.text.muted,
    fontSize: 10,
    fontWeight: 'bold',
  },
});
