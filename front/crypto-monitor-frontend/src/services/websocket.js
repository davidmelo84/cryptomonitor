import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export const createWebSocket = (token, onConnect) => {
  const socket = new SockJS(`${API_BASE_URL}/ws`);
  const client = new Client({
    webSocketFactory: () => socket,
    connectHeaders: {
      Authorization: `Bearer ${token}`
    },
    onConnect: () => {
      console.log('✅ WebSocket conectado');
      onConnect(client);
    },
    onStompError: (error) => {
      console.error('❌ WebSocket error:', error);
    }
  });
  
  client.activate();
  return client;
};