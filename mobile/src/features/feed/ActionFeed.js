import React from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { theme } from '../../theme/theme';
import { Zap, Swords, User } from 'lucide-react-native';

const FeedItem = ({ type, title, message, time }) => {
  const isFight = type === 'FIGHT';
  const Icon = isFight ? Swords : User;
  
  return (
    <View style={styles.item}>
      <View style={[styles.iconContainer, { backgroundColor: isFight ? 'rgba(239, 68, 68, 0.1)' : 'rgba(99, 102, 241, 0.1)' }]}>
        <Icon size={14} color={isFight ? theme.colors.danger : theme.colors.primary} />
      </View>
      <View style={styles.itemContent}>
        <View style={styles.itemHeader}>
          <Text style={styles.itemTitle}>{title}</Text>
          <Text style={styles.itemTime}>{time}</Text>
        </View>
        <Text style={styles.itemMessage}>{message}</Text>
      </View>
    </View>
  );
};

export const ActionFeed = () => {
  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Zap size={14} color={theme.colors.accent} />
        <Text style={styles.headerTitle}>Ostatnia Aktywność</Text>
      </View>
      <ScrollView showsVerticalScrollIndicator={false} style={styles.scroll}>
        <FeedItem 
          type="FIGHT" 
          title="Twoja Walka" 
          message="Zalewasz Szkielet serią ciosów! Zadajesz 12 pkt obrażeń." 
          time="teraz"
        />
        <FeedItem 
          type="SOCIAL" 
          title="Inny Gracz" 
          message="MistrzGier przeszedł obok Ciebie zmierzając do Portalu." 
          time="2 min"
        />
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.colors.surface,
    borderTopLeftRadius: theme.borderRadius.xl,
    borderTopRightRadius: theme.borderRadius.xl,
    padding: theme.spacing.lg,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: theme.spacing.md,
  },
  headerTitle: {
    color: theme.colors.text.primary,
    fontSize: 14,
    fontWeight: 'bold',
    marginLeft: theme.spacing.sm,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  scroll: {
    flex: 1,
  },
  item: {
    flexDirection: 'row',
    marginBottom: theme.spacing.md,
    backgroundColor: 'rgba(255, 255, 255, 0.02)',
    padding: theme.spacing.sm,
    borderRadius: theme.borderRadius.md,
  },
  iconContainer: {
    width: 32,
    height: 32,
    borderRadius: theme.borderRadius.sm,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: theme.spacing.md,
  },
  itemContent: {
    flex: 1,
  },
  itemHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 2,
  },
  itemTitle: {
    color: theme.colors.text.primary,
    fontSize: 12,
    fontWeight: 'bold',
  },
  itemTime: {
    color: theme.colors.text.muted,
    fontSize: 10,
  },
  itemMessage: {
    color: theme.colors.text.secondary,
    fontSize: 12,
    lineHeight: 16,
  },
});
