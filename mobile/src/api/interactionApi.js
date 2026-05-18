import axios from 'axios';
import { authService } from './authService';
import { BASE_URL } from './agentApi';

const API_BASE_URL = `${BASE_URL}/api/v1`;

const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 5000,
});

apiClient.interceptors.request.use(
  (config) => {
    const token = authService.getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export const interactionApi = {
  /**
   * Pobiera listę akcji dla klikniętego obiektu.
   */
  getActions: async (agentId, targetId, targetType, targetName = '') => {
    const response = await apiClient.get('/api/v1/interactions', {
      params: { agentId, targetId, targetType, targetName }
    });
    return response.data; // InteractionResponse
  },

  /**
   * Wykonuje akcję — używa endpoint z deskryptora.
   */
  executeAction: async (endpoint, httpMethod = 'POST', params = {}) => {
    const method = httpMethod.toLowerCase();
    // Dla POST/PUT przekazujemy null jako body i parametry w config.params
    if (method === 'post' || method === 'put' || method === 'patch') {
      const response = await apiClient[method](endpoint, null, { params });
      return response.data;
    }
    const response = await apiClient[method](endpoint, { params });
    return response.data;
  }
};
