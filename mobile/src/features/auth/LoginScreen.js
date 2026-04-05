import React, { useState } from 'react';
import { StyleSheet, View, Text, TextInput, TouchableOpacity, ActivityIndicator, Alert, KeyboardAvoidingView, Platform } from 'react-native';
import { theme } from '../../theme/theme';
import { authService } from '../../api/authService';
import { LogIn, UserPlus } from 'lucide-react-native';
import { LinearGradient } from 'expo-linear-gradient';

export const LoginScreen = ({ onLoginSuccess }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [isRegisterMode, setIsRegisterMode] = useState(false);

  const handleAuth = async () => {
    if (!username || !password) {
      Alert.alert("Błąd", "Wprowadź login i hasło");
      return;
    }

    setLoading(true);
    try {
      if (isRegisterMode) {
        await authService.register(username, password);
        Alert.alert("Sukces", "Konto zostało utworzone. Teraz możesz się zalogować.");
        setIsRegisterMode(false);
      } else {
        const token = await authService.login(username, password);
        onLoginSuccess(token);
      }
    } catch (error) {
      console.error("Auth Error:", error);
      Alert.alert("Błąd autoryzacji", error.response?.status === 401 
        ? "Niepoprawny login lub hasło" 
        : error.response?.status === 409 
          ? "Użytkownik o tej nazwie już istnieje"
          : "Wystąpił problem z połączeniem");
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView 
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      style={styles.container}
    >
      <LinearGradient
        colors={[theme.colors.background, '#1a1a2e']}
        style={styles.gradient}
      >
        <View style={styles.card}>
          <Text style={styles.title}>BOT KINGDOMS</Text>
          <Text style={styles.subtitle}>{isRegisterMode ? 'Stwórz nową postać' : 'Wkrocz do krainy'}</Text>

          <View style={styles.inputContainer}>
            <Text style={styles.label}>LOGIN</Text>
            <TextInput
              style={styles.input}
              value={username}
              onChangeText={setUsername}
              placeholder="Twój nick..."
              placeholderTextColor={theme.colors.text.muted}
              autoCapitalize="none"
            />
          </View>

          <View style={styles.inputContainer}>
            <Text style={styles.label}>HASŁO</Text>
            <TextInput
              style={styles.input}
              value={password}
              onChangeText={setPassword}
              placeholder="Twoje tajne hasło..."
              placeholderTextColor={theme.colors.text.muted}
              secureTextEntry
            />
          </View>

          <TouchableOpacity 
            style={styles.button} 
            onPress={handleAuth}
            disabled={loading}
          >
            {loading ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <>
                {isRegisterMode ? <UserPlus size={20} color="#fff" /> : <LogIn size={20} color="#fff" />}
                <Text style={styles.buttonText}>
                  {isRegisterMode ? 'ZAREJESTRUJ' : 'ZALOGUJ'}
                </Text>
              </>
            )}
          </TouchableOpacity>

          <TouchableOpacity 
            style={styles.switchButton}
            onPress={() => setIsRegisterMode(!isRegisterMode)}
          >
            <Text style={styles.switchText}>
              {isRegisterMode ? 'Masz już konto? Zaloguj się' : 'Nie masz konta? Zarejestruj się'}
            </Text>
          </TouchableOpacity>
        </View>
      </LinearGradient>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  gradient: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: theme.spacing.xl,
  },
  card: {
    width: '100%',
    maxWidth: 400,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    borderRadius: 16,
    padding: theme.spacing.xl,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
  },
  title: {
    color: theme.colors.text.primary,
    fontSize: 24,
    fontWeight: '900',
    textAlign: 'center',
    letterSpacing: 4,
    marginBottom: 4,
  },
  subtitle: {
    color: theme.colors.accent,
    fontSize: 14,
    textAlign: 'center',
    marginBottom: theme.spacing.xl,
    fontWeight: 'bold',
  },
  inputContainer: {
    marginBottom: theme.spacing.lg,
  },
  label: {
    color: theme.colors.text.muted,
    fontSize: 10,
    fontWeight: 'bold',
    marginBottom: 8,
    letterSpacing: 1,
  },
  input: {
    backgroundColor: 'rgba(0, 0, 0, 0.3)',
    borderRadius: 8,
    padding: 12,
    color: theme.colors.text.primary,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
  },
  button: {
    backgroundColor: theme.colors.accent,
    borderRadius: 8,
    padding: 14,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: theme.spacing.md,
  },
  buttonText: {
    color: '#fff',
    fontWeight: 'bold',
    marginLeft: 10,
    letterSpacing: 1,
  },
  switchButton: {
    marginTop: theme.spacing.xl,
    alignItems: 'center',
  },
  switchText: {
    color: theme.colors.text.muted,
    fontSize: 12,
  }
});
