// src/services/websocket.js
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { API_BASE_URL } from '../utils/constants';

// ================================================================
//  createWebSocket — WebSocket com STOMP + SockJS
//  - Token obrigatório
//  - Reconexão automática
//  - Callback onConnect recebe o cliente STOMP conectado
// ================================================================
export const createWebSocket = (token, onConnect) => {
  if (!token) {
    console.error("❌ createWebSocket chamado sem token.");
    return null;
  }

  console.log("🔌 Iniciando conexão WebSocket...");

  const socket = new SockJS(`${API_BASE_URL}/ws`);

  const client = new Client({
    webSocketFactory: () => socket,

    connectHeaders: {
      Authorization: `Bearer ${token}`
    },

    // === Reconexão automática ===
    reconnectDelay: 5000, // tenta reconectar a cada 5s
    debug: () => {},      // desativa logs verbosos

    onConnect: () => {
      console.log("✅ WebSocket conectado");
      if (onConnect) onConnect(client);
    },

    onStompError: (error) => {
      console.error("❌ Erro STOMP:", error);
    },

    onWebSocketError: (event) => {
      console.error("❌ Erro WebSocket:", event);
    }
  });

  client.activate();
  return client;
};
