import { useState, useCallback } from 'react';
import { interactionApi } from '../../../api/interactionApi';

export function useInteractionPanel(agentId) {
  const [panel, setPanel] = useState(null); // null = ukryty
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const openPanel = useCallback(async (targetId, targetType, targetName, targetX = null, targetY = null) => {
    // 1. Optymistycznie pokazuj panel ze spinnerem z koordynatami
    setPanel({ targetId, targetType, targetName, targetX, targetY, actions: null });
    setLoading(true);
    setError(null);

    try {
      // 2. Pobierz prawdziwe akcje z backendu
      const response = await interactionApi.getActions(agentId, targetId, targetType, targetName);
      setPanel(prev => ({
        ...response,
        targetX: prev?.targetX,
        targetY: prev?.targetY,
      }));
    } catch (err) {
      setError('Nie udało się pobrać akcji');
      console.error('[useInteractionPanel]', err);
    } finally {
      setLoading(false);
    }
  }, [agentId]);

  const closePanel = useCallback(() => {
    setPanel(null);
    setError(null);
  }, []);

  const executeAction = useCallback(async (action, extraParams = {}) => {
    if (!action.enabled || !action.endpoint) return;

    // Podmień {agentId} w endpoint jeśli potrzeba
    const resolvedEndpoint = action.endpoint.replace('{agentId}', agentId);

    try {
      const response = await interactionApi.executeAction(resolvedEndpoint, action.httpMethod, extraParams);
      closePanel();
      return response;
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.message;
      setError('Akcja nie powiodła się: ' + errorMsg);
      throw err;
    }
  }, [agentId, closePanel]);

  return { panel, loading, error, openPanel, closePanel, executeAction };
}
