import axios from 'axios';
import { authService } from './authService';

// The backend is running at localhost:8080. 
// For Expo Web, we can use localhost. 
// For a physical device, we would need the local IP (e.g., 192.168.1.x).
const API_BASE_URL = 'http://localhost:8080/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 5000,
});

// Request interceptor for adding the bearer token
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

export const agentApi = {
  /**
   * Fetches the list of all agents.
   */
  getAllAgents: async () => {
    const response = await apiClient.get('/agents');
    return response.data;
  },

  /**
   * Fetches details for a specific agent.
   */
  getAgentDetails: async (id) => {
    const response = await apiClient.get(`/agents/${id}`);
    return response.data;
  },

  /**
   * Fetches the goals, dimensions and portals for a specific location.
   */
  getLocationDetails: async (id) => {
    const response = await apiClient.get(`/locations/${id}`);
    return response.data;
  },

  /**
   * Sends a high-level goal to the agent's AI brain.
   */
  sendGoal: async (id, goal) => {
    const response = await apiClient.post(`/agents/${id}/goal`, null, {
      params: { goal }
    });
    return response.data;
  },
};
