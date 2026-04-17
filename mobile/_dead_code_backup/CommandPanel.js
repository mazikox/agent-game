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
      <Text style={styles.label}>COMMAND AGENT</Text>
      <View style={styles.inputWrapper}>
        <Text style={styles.prompt}>&gt;</Text>
        <TextInput
          style={styles.input}
          placeholder="Type a command..."
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
            <View style={styles.buttonContent}>
              <Text style={styles.buttonText}>SEND</Text>
              <Send size={14} color="white" />
            </View>
          )}
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 10,
    backgroundColor: theme.colors.glass,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: theme.colors.borderLight,
    width: 380,
  },
  label: {
    color: theme.colors.accent,
    fontSize: 10,
    fontWeight: '900',
    letterSpacing: 1,
    marginBottom: 8,
  },
  inputWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.4)',
    borderRadius: 6,
    paddingLeft: 12,
    paddingRight: 4,
    height: 40,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
  },
  prompt: {
    color: theme.colors.text.muted,
    fontSize: 16,
    marginRight: 8,
    fontWeight: 'bold',
  },
  input: {
    flex: 1,
    color: theme.colors.text.primary,
    fontSize: 13,
    fontWeight: '500',
  },
  sendButton: {
    height: 32,
    paddingHorizontal: 12,
    borderRadius: 4,
    backgroundColor: theme.colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.2)',
  },
  buttonContent: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  buttonText: {
    color: 'white',
    fontSize: 10,
    fontWeight: '900',
  },
  disabled: {
    backgroundColor: theme.colors.secondary,
    opacity: 0.5,
  },
});
