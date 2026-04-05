import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/v1/auth';

const authClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json',
  },
});

let authToken = null;

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
  }
};
