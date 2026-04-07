import React from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { theme } from '../../theme/theme';
import { ChevronRight } from 'lucide-react-native';

const LogItem = ({ message, time }) => {
  return (
    <View style={styles.item}>
      <Text style={styles.itemTime}>[{time}]</Text>
      <Text style={styles.itemMessage}>
        {message.split(' ').map((word, i) => {
          // Decorative coloring for certain words
          const isHighlight = word.includes('Dragon') || word.includes('Peak') || word.includes('Lair');
          return (
            <Text key={i} style={isHighlight ? styles.highlight : null}>
              {word}{' '}
            </Text>
          );
        })}
      </Text>
    </View>
  );
};

export const ActionFeed = () => {
  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>AGENT LOG</Text>
        <ChevronRight size={12} color={theme.colors.text.muted} />
      </View>
      <ScrollView showsVerticalScrollIndicator={true} style={styles.scroll}>
        <LogItem 
          time="15:01" 
          message="Lyra navigated to Dragon's Peak." 
        />
        <LogItem 
          time="15:05" 
          message="Sighted a young dragon near the lair." 
        />
        <LogItem 
          time="15:10" 
          message="Initiating stealth approach." 
        />
        <LogItem 
          time="15:12" 
          message="Lyra collected 2 Dragon Scale Fragments." 
        />
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    height: 140,
    width: 380,
    backgroundColor: theme.colors.glass,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: theme.colors.borderLight,
    padding: 10,
    marginBottom: 8,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255, 255, 255, 0.05)',
    paddingBottom: 4,
    marginBottom: 6,
  },
  headerTitle: {
    color: theme.colors.accent,
    fontSize: 10,
    fontWeight: '900',
    letterSpacing: 1,
  },
  scroll: {
    flex: 1,
  },
  item: {
    flexDirection: 'row',
    marginBottom: 4,
    alignItems: 'flex-start',
  },
  itemTime: {
    color: theme.colors.text.muted,
    fontSize: 11,
    fontFamily: 'System', // Use mono if available
    marginRight: 8,
  },
  itemMessage: {
    color: theme.colors.text.primary,
    fontSize: 11,
    flex: 1,
    lineHeight: 15,
  },
  highlight: {
    color: theme.colors.accent,
    fontWeight: 'bold',
  },
});
