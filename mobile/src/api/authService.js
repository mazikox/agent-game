import axios from 'axios';

const API_BASE_URL = `${process.env.EXPO_PUBLIC_API_URL || 'http://localhost:8080'}/api/v1/auth`;

const authClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Safe zero-dependency persistence helper for Expo Web & native fallbacks
const storage = {
  getItem: (key) => {
    try {
      return typeof window !== 'undefined' && window.localStorage ? window.localStorage.getItem(key) : null;
    } catch (e) {
      return null;
    }
  },
  setItem: (key, value) => {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        window.localStorage.setItem(key, value);
      }
    } catch (e) {
      // ignore
    }
  },
  removeItem: (key) => {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        window.localStorage.removeItem(key);
      }
    } catch (e) {
      // ignore
    }
  }
};

let authToken = storage.getItem('authToken');

export const authService = {
  /**
   * Registers a new player account.
   */
  register: async (username, password) => {
    await authClient.post('/register', { username, password });
  },

  /**
   * Logs in a player and stores the JWT token.
   */
  login: async (username, password) => {
    const response = await authClient.post('/login', { username, password });
    if (response.data && response.data.token) {
      authToken = response.data.token;
      storage.setItem('authToken', authToken);
      return authToken;
    }
    throw new Error("Logowanie nie powiodło się - brak tokenu");
  },

  /**
   * Returns the current JWT token.
   */
  getToken: () => authToken,

  /**
   * Clears the current session.
   */
  logout: () => {
    authToken = null;
    storage.removeItem('authToken');
  }
};
