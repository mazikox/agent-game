import React, { useState } from 'react';
import { View, Text, StyleSheet, ScrollView, TextInput, TouchableOpacity, ActivityIndicator } from 'react-native';
import { theme } from '../../theme/theme';
import { ChevronRight, Send } from 'lucide-react-native';

const LogItem = ({ message, time }) => {
  return (
    <View style={styles.logItem}>
      <Text style={styles.logTime}>[{time}]</Text>
      <Text style={styles.logMessage}>
        {(message || '').split(' ').map((word, i) => {
          const isHighlight = word.includes('Dragon') || word.includes('Peak') || word.includes('Scale') || word.includes('Shadow');
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

export const AgentConsole = ({ agentId, logs = [], onCommandSent }) => {
  const [command, setCommand] = useState('');
  const [loading, setLoading] = useState(false);
  const scrollRef = React.useRef();

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
      {/* AGENT LOG SECTION */}
      <View style={styles.section}>
        <View style={styles.header}>
          <Text style={styles.headerTitle}>AGENT LOG</Text>
          <ChevronRight size={12} color={theme.colors.text.muted} />
        </View>
        <ScrollView 
          ref={scrollRef}
          style={styles.logScroll} 
          showsVerticalScrollIndicator={true}
          onContentSizeChange={() => scrollRef.current?.scrollToEnd({ animated: true })}
        >
          {logs.length > 0 ? (
            logs.map((log, idx) => (
              <LogItem key={idx} time={log.time} message={log.message} />
            ))
          ) : (
            <Text style={styles.emptyLog}>Waiting for agent reports...</Text>
          )}
        </ScrollView>
      </View>

      {/* COMMAND AGENT SECTION */}
      <View style={styles.commandSection}>
        <Text style={styles.headerTitle}>COMMAND AGENT</Text>
        <View style={styles.inputWrapper}>
          <Text style={styles.prompt}>&gt;</Text>
          <TextInput
            style={styles.input}
            placeholder={`Type a command...`}
            placeholderTextColor={theme.colors.text.muted}
            value={command}
            onChangeText={setCommand}
            onSubmitEditing={handleSend}
          />
          <TouchableOpacity 
            style={[styles.sendButton, !command.trim() && styles.disabled]} 
            onPress={handleSend}
            disabled={loading || !command.trim()}
          >
            {loading ? (
              <ActivityIndicator size="small" color="#fff" />
            ) : (
              <View style={styles.sendContent}>
                <Text style={styles.sendText}>SEND</Text>
                <Send size={12} color="#fff" />
              </View>
            )}
          </TouchableOpacity>
        </View>
      </View>

      {/* DECORATIVE CORNERS */}
      <View style={[styles.corner, styles.topLeft]} />
      <View style={[styles.corner, styles.topRight]} />
      <View style={[styles.corner, styles.bottomLeft]} />
      <View style={[styles.corner, styles.bottomRight]} />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    width: '100%',
    maxWidth: 440,
    backgroundColor: 'rgba(26, 54, 93, 0.4)',
    borderRadius: 4,
    borderWidth: 1,
    borderColor: 'rgba(79, 209, 237, 0.2)',
    padding: 12,
    position: 'relative',
  },
  section: {
    height: 120,
    marginBottom: 10,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 5,
  },
  headerTitle: {
    color: theme.colors.text.gold,
    fontSize: 10,
    fontWeight: '900',
    letterSpacing: 1,
    marginBottom: 5,
  },
  logScroll: {
    flex: 1,
  },
  logItem: {
    flexDirection: 'row',
    marginBottom: 3,
  },
  logTime: {
    color: theme.colors.text.muted,
    fontSize: 11,
    marginRight: 6,
  },
  logMessage: {
    color: '#fff',
    fontSize: 11,
    flex: 1,
    lineHeight: 14,
  },
  emptyLog: {
    color: theme.colors.text.muted,
    fontSize: 10,
    fontStyle: 'italic',
    textAlign: 'center',
    marginTop: 20,
  },
  highlight: {
    color: theme.colors.text.gold,
    fontWeight: 'bold',
  },
  commandSection: {
    borderTopWidth: 1,
    borderTopColor: 'rgba(255, 255, 255, 0.05)',
    paddingTop: 8,
  },
  inputWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(0,0,0,0.4)',
    borderRadius: 4,
    paddingHorizontal: 10,
    height: 36,
    borderWidth: 1,
    borderColor: 'rgba(79, 209, 237, 0.2)',
  },
  prompt: {
    color: theme.colors.primary,
    fontSize: 14,
    marginRight: 8,
  },
  input: {
    flex: 1,
    color: '#fff',
    fontSize: 12,
  },
  sendButton: {
    backgroundColor: 'rgba(26, 54, 93, 0.8)',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 4,
    borderWidth: 1,
    borderColor: 'rgba(79, 209, 237, 0.4)',
  },
  sendContent: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  sendText: {
    color: '#fff',
    fontSize: 10,
    fontWeight: '900',
  },
  disabled: {
    opacity: 0.5,
  },
  corner: {
    position: 'absolute',
    width: 10,
    height: 10,
    borderColor: theme.colors.primary,
  },
  topLeft: { top: -1, left: -1, borderTopWidth: 2, borderLeftWidth: 2, borderTopLeftRadius: 4 },
  topRight: { top: -1, right: -1, borderTopWidth: 2, borderRightWidth: 2, borderTopRightRadius: 4 },
  bottomLeft: { bottom: -1, left: -1, borderBottomWidth: 2, borderLeftWidth: 2, borderBottomLeftRadius: 4 },
  bottomRight: { bottom: -1, right: -1, borderBottomWidth: 2, borderRightWidth: 2, borderBottomRightRadius: 4 },
});
