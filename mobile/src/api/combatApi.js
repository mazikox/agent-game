import axios from 'axios';
import { authService } from './authService';
import { BASE_URL } from './agentApi';

const apiClient = axios.create({
  baseURL: `${BASE_URL}/api/combat`,
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
  (error) => Promise.reject(error)
);

export const combatApi = {
  /**
   * Initiates combat between an agent and a creature.
   */
  initiate: async (agentId, creatureId) => {
    const response = await apiClient.post('/initiate', null, {
      params: { agentId, creatureId }
    });
    return response.data;
  },

  /**
   * Performs a combat action.
   */
  executeAction: async (agentId, actionType) => {
    const response = await apiClient.post('/action', null, {
      params: { agentId, actionType }
    });
    return response.data;
  }
};
