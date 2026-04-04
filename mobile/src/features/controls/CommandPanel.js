import React, { useState } from 'react';
import { View, TextInput, TouchableOpacity, Text, StyleSheet, ActivityIndicator } from 'react-native';
import { theme } from '../../theme/theme';
import { Send } from 'lucide-react-native';

export const CommandPanel = ({ agentId, onCommandSent }) => {
  const [command, setCommand] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSend = async () => {
    if (!command.trim()) return;
    
    setLoading(true);
    try {
      await onCommandSent(command);
      setCommand('');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.inputWrapper}>
        <TextInput
          style={styles.input}
          placeholder="Wpisz rozkaz (np. Idź do lasu)..."
          placeholderTextColor={theme.colors.text.muted}
          value={command}
          onChangeText={setCommand}
          multiline={false}
          onSubmitEditing={handleSend}
        />
        <TouchableOpacity 
          style={[styles.sendButton, !command.trim() && styles.disabled]} 
          onPress={handleSend}
          disabled={loading || !command.trim()}
        >
          {loading ? (
            <ActivityIndicator size="small" color="white" />
          ) : (
            <Send size={18} color="white" />
          )}
        </TouchableOpacity>
      </View>
      <Text style={styles.hint}>Twoje AI przeanalizuje ten rozkaz i podejmie kroki. 🧠</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: theme.spacing.lg,
    backgroundColor: 'rgba(0, 0, 0, 0.4)',
    borderTopWidth: 1,
    borderTopColor: 'rgba(255, 255, 255, 0.05)',
  },
  inputWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: theme.colors.surface,
    borderRadius: theme.borderRadius.full,
    paddingLeft: theme.spacing.lg,
    paddingRight: 4,
    height: 48,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
  },
  input: {
    flex: 1,
    color: theme.colors.text.primary,
    fontSize: 14,
    fontWeight: '500',
  },
  sendButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: theme.colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  disabled: {
    backgroundColor: theme.colors.secondary,
    opacity: 0.5,
  },
  hint: {
    color: theme.colors.text.muted,
    fontSize: 10,
    textAlign: 'center',
    marginTop: theme.spacing.sm,
    fontStyle: 'italic',
  },
});
