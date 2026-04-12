import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BASE_URL } from './agentApi';
import { authService } from './authService';

const SocketContext = createContext(null);

export const useSocket = () => useContext(SocketContext);

export const SocketProvider = ({ children }) => {
  const [connected, setConnected] = useState(false);
  const stompClient = useRef(null);
  const subscriptions = useRef({});

  const connect = async () => {
    if (stompClient.current?.connected) return;

    const token = await authService.getToken();
    if (!token) return;

    // Use SockJS because Spring Boot is configured with it
    const socket = new SockJS(`${BASE_URL}/ws`);
    
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      onConnect: () => {
        console.log('Connected to WebSocket');
        setConnected(true);
      },
      onDisconnect: () => {
        console.log('Disconnected from WebSocket');
        setConnected(false);
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame.headers['message']);
      },
    });

    client.activate();
    stompClient.current = client;
  };

  const disconnect = () => {
    if (stompClient.current) {
      stompClient.current.deactivate();
      setConnected(false);
    }
  };

  const subscribeToAgent = (agentId, onMessage) => {
    if (!stompClient.current || !connected) return null;

    const topic = `/topic/agents/${agentId}`;
    console.log(`Subscribing to ${topic}`);
    
    const subscription = stompClient.current.subscribe(topic, (message) => {
      const payload = JSON.parse(message.body);
      console.log(`[Socket] Message from ${topic}:`, payload);
      onMessage(payload);
    });

    subscriptions.current[agentId] = subscription;
    return subscription;
  };

  const unsubscribeFromAgent = (agentId) => {
    if (subscriptions.current[agentId]) {
      subscriptions.current[agentId].unsubscribe();
      delete subscriptions.current[agentId];
    }
  };

  useEffect(() => {
    return () => disconnect();
  }, []);

  return (
    <SocketContext.Provider value={{ connected, connect, disconnect, subscribeToAgent, unsubscribeFromAgent }}>
      {children}
    </SocketContext.Provider>
  );
};
