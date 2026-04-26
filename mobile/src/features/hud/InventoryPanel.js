import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, ActivityIndicator, TouchableOpacity, ScrollView, Image } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { theme } from '../../theme/theme';
import { agentApi } from '../../api/agentApi';
import { getItemImageUrl, ITEM_PLACEHOLDER } from '../../utils/itemUtils';

export const InventoryPanel = ({ agentId }) => {
  const [inventory, setInventory] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchInventory = async () => {
      try {
        setLoading(true);
        if (agentId) {
          const data = await agentApi.getInventory(agentId);
          setInventory(data);
        }
      } catch (err) {
        console.error('Failed to fetch inventory:', err);
        setError('Failed to load inventory');
      } finally {
        setLoading(false);
      }
    };

    fetchInventory();
  }, [agentId]);

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color={theme.colors.accent} />
      </View>
    );
  }

  if (error || !inventory) {
    return (
      <View style={styles.center}>
        <Text style={styles.errorText}>{error || 'No inventory data'}</Text>
      </View>
    );
  }

  const { width, height, items } = inventory;
  const slotSize = 60; // Size of one grid slot

  return (
    <LinearGradient
      colors={['rgba(20, 20, 25, 0.95)', 'rgba(10, 10, 15, 0.98)']}
      style={styles.container}
    >
      <Text style={styles.title}>INVENTORY</Text>
      
      <ScrollView horizontal contentContainerStyle={styles.scrollContent}>
        <ScrollView contentContainerStyle={styles.scrollContent}>
          <View style={[styles.gridContainer, { width: width * slotSize, height: height * slotSize }]}>
            {/* Render background grid slots */}
            {Array.from({ length: width * height }).map((_, index) => (
              <View 
                key={`slot-${index}`} 
                style={[
                  styles.slot, 
                  { 
                    width: slotSize, 
                    height: slotSize,
                    left: (index % width) * slotSize,
                    top: Math.floor(index / width) * slotSize
                  }
                ]} 
              />
            ))}

            {/* Render items */}
            {items.map((item) => {
              const itemLeft = (item.gridIndex % width) * slotSize;
              const itemTop = Math.floor(item.gridIndex / width) * slotSize;
              const itemWidth = item.width * slotSize;
              const itemHeight = item.height * slotSize;

              return (
                <TouchableOpacity
                  key={item.id}
                  style={[
                    styles.itemWrapper,
                    {
                      left: itemLeft,
                      top: itemTop,
                      width: itemWidth,
                      height: itemHeight,
                    }
                  ]}
                  activeOpacity={0.8}
                >
                  <LinearGradient
                    colors={['rgba(246, 173, 85, 0.2)', 'rgba(246, 173, 85, 0.05)']}
                    style={styles.itemInner}
                  >
                    <Image
                      source={getItemImageUrl(item.iconUrl)
                        ? { uri: getItemImageUrl(item.iconUrl) }
                        : ITEM_PLACEHOLDER}
                      style={styles.itemImage}
                      resizeMode="contain"
                    />
                    {item.quantity > 1 && (
                      <Text style={styles.itemQuantity}>
                        x{item.quantity}
                      </Text>
                    )}
                  </LinearGradient>
                </TouchableOpacity>
              );
            })}
          </View>
        </ScrollView>
      </ScrollView>
    </LinearGradient>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 15,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: 'rgba(246, 173, 85, 0.3)',
    alignItems: 'center',
    justifyContent: 'center',
    minWidth: 300,
    minHeight: 300,
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  title: {
    color: theme.colors.accent,
    fontSize: 18,
    fontFamily: theme.typography.fantasy,
    letterSpacing: 2,
    marginBottom: 15,
    textShadowColor: '#000',
    textShadowOffset: { width: 1, height: 1 },
    textShadowRadius: 3,
  },
  scrollContent: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  gridContainer: {
    position: 'relative',
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
  },
  slot: {
    position: 'absolute',
    borderWidth: 0.5,
    borderColor: 'rgba(255, 255, 255, 0.1)',
    backgroundColor: 'rgba(255, 255, 255, 0.02)',
  },
  itemWrapper: {
    position: 'absolute',
    padding: 2,
  },
  itemInner: {
    flex: 1,
    borderWidth: 1,
    borderColor: 'rgba(246, 173, 85, 0.5)',
    borderRadius: 4,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 4,
  },
  itemImage: {
    flex: 1,
    width: '90%',
    height: '90%',
  },
  itemName: {
    color: '#e2e8f0',
    fontSize: 11,
    textAlign: 'center',
    fontFamily: theme.typography.bold,
  },
  itemQuantity: {
    position: 'absolute',
    bottom: 2,
    right: 4,
    color: '#ecc94b',
    fontSize: 10,
    fontWeight: 'bold',
  },
  errorText: {
    color: theme.colors.danger,
    fontFamily: theme.typography.bold,
  }
});
