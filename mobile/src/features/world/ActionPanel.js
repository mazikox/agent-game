import React from 'react';
import { View, Text, TouchableOpacity, ActivityIndicator, StyleSheet, Platform } from 'react-native';
import { theme } from '../../theme/theme';

const ICON_MAP = {
  sword:        '⚔️',
  eye:          '🔍',
  move:         '👣',
  run:          '💨',
  'door-enter': '🚪',
};

const LABEL_MAP = {
  examine: 'Zbadaj',
  approach: 'Podejdź',
  attack: 'Zaatakuj',
  enter_portal: 'Wejdź do portalu',
};

const translateDisabledReason = (disabledReason) => {
  if (!disabledReason) return null;
  const { code, params } = disabledReason;
  switch (code) {
    case 'AGENT_IN_COMBAT':
      return 'Agent jest w walce';
    case 'TARGET_DEAD':
      return 'Cel jest martwy';
    case 'ALREADY_CLOSE':
      return 'Już jesteś obok';
    case 'CREATURE_IN_COMBAT':
      return 'Cel jest już w walce';
    case 'TOO_FAR':
      if (params && params.current !== undefined && params.max !== undefined) {
        return `Za daleko (${Math.round(params.current)} / ${params.max})`;
      }
      return 'Za daleko';
    default:
      return code;
  }
};

const ActionButton = ({ action, onPress }) => {
  const icon = ICON_MAP[action.icon] || '●';
  const label = LABEL_MAP[action.actionId] || action.actionId;
  const reason = translateDisabledReason(action.disabledReason);

  return (
    <TouchableOpacity
      style={[styles.button, !action.enabled && styles.buttonDisabled]}
      onPress={() => action.enabled && onPress(action)}
      disabled={!action.enabled}
      activeOpacity={action.enabled ? 0.75 : 1}
    >
      <Text style={styles.buttonIcon}>{icon}</Text>
      <Text style={[styles.buttonLabel, !action.enabled && styles.buttonLabelDisabled]}>
        {label}
      </Text>
      {!action.enabled && reason && (
        <Text style={styles.disabledReason}>{reason}</Text>
      )}
    </TouchableOpacity>
  );
};

export const ActionPanel = ({ panel, loading, error, onAction, onClose, style }) => {
  if (!panel) return null;

  return (
    <View style={[styles.overlay, style]} pointerEvents="box-none">
      <View style={styles.container}>
        {/* Nagłówek */}
        <View style={styles.header}>
          <Text style={styles.title}>{panel.targetName || '...'}</Text>
          <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
            <Text style={styles.closeBtnText}>✕</Text>
          </TouchableOpacity>
        </View>

        {/* Zawartość */}
        {loading && !panel.actions && (
          <ActivityIndicator color={theme.colors.primary} style={{ marginVertical: 16 }} />
        )}

        {error && (
          <Text style={styles.errorText}>{error}</Text>
        )}

        {panel.actions && (
          <View style={styles.actionsContainer}>
            {panel.actions.length === 0 ? (
              <Text style={styles.emptyText}>Brak dostępnych akcji</Text>
            ) : (
              panel.actions.map((action) => (
                <ActionButton key={action.actionId} action={action} onPress={onAction} />
              ))
            )}
          </View>
        )}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  overlay: {
    position: 'absolute',
    bottom: 20,
    right: 20,
    zIndex: 600,
  },
  container: {
    backgroundColor: theme.colors.glass,
    borderRadius: theme.borderRadius.lg,
    borderWidth: 1,
    borderColor: theme.colors.border.light,
    minWidth: 200,
    maxWidth: 260,
    padding: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.7,
    shadowRadius: 16,
    ...(Platform.OS === 'web' ? { backdropFilter: 'blur(8px)' } : {}),
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
    paddingBottom: 8,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255,255,255,0.08)',
  },
  title: {
    color: '#ecc94b',
    fontSize: 13,
    fontWeight: '900',
    fontFamily: theme.typography.fantasy,
    flex: 1,
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  closeBtn: {
    padding: 4,
  },
  closeBtnText: {
    color: '#718096',
    fontSize: 12,
  },
  actionsContainer: {
    gap: 6,
  },
  button: {
    backgroundColor: 'rgba(255,255,255,0.06)',
    borderRadius: theme.borderRadius.md,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.1)',
    paddingHorizontal: 12,
    paddingVertical: 9,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    flexWrap: 'wrap',
  },
  buttonDisabled: {
    opacity: 0.45,
    borderColor: 'rgba(255,255,255,0.05)',
  },
  buttonIcon: {
    fontSize: 14,
  },
  buttonLabel: {
    color: '#e2e8f0',
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  buttonLabelDisabled: {
    color: '#718096',
  },
  disabledReason: {
    color: '#a0aec0',
    fontSize: 10,
    width: '100%',
    marginTop: 2,
  },
  errorText: {
    color: '#fc8181',
    fontSize: 11,
    textAlign: 'center',
    marginVertical: 8,
  },
  emptyText: {
    color: '#718096',
    fontSize: 11,
    textAlign: 'center',
    marginVertical: 8,
  },
});
