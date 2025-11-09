// front/crypto-monitor-frontend/src/hooks/useHeartbeat.js
// ✅ CRIAR ESTE ARQUIVO NOVO

import { useEffect, useRef } from 'react';

/**
 * Hook customizado para enviar heartbeat ao backend
 * Mantém o monitoramento ativo enquanto usuário está na página
 * 
 * @param {boolean} isActive - Se deve enviar heartbeat (usuário logado)
 * @param {string} username - Nome do usuário
 * @param {object} stompClient - Cliente WebSocket (se disponível)
 */
export const useHeartbeat = (isActive, username, stompClient = null) => {
  const intervalRef = useRef(null);
  const lastSentRef = useRef(0);

  useEffect(() => {
    // ✅ Só ativa se usuário está logado
    if (!isActive || !username) {
      console.log('🔇 Heartbeat desativado (usuário não logado)');
      return;
    }

    console.log('💓 Heartbeat ativado para:', username);

    // ✅ Função que envia heartbeat
    const sendHeartbeat = () => {
      const now = Date.now();
      
      // Evita enviar muito rápido (mínimo 50 segundos entre envios)
      if (now - lastSentRef.current < 50000) {
        return;
      }

      lastSentRef.current = now;

      // ✅ Tenta enviar via WebSocket primeiro
      if (stompClient && stompClient.connected) {
        try {
          stompClient.send('/app/heartbeat', {}, JSON.stringify({
            username: username,
            timestamp: now
          }));
          console.log('💓 Heartbeat enviado via WebSocket:', username);
        } catch (error) {
          console.warn('⚠️ Erro ao enviar heartbeat via WebSocket:', error);
          // Fallback: você pode adicionar HTTP request aqui se necessário
        }
      } else {
        console.log('⚠️ WebSocket não conectado, heartbeat não enviado');
        // Alternativa: fazer HTTP request
        // sendHeartbeatViaHttp(username);
      }
    };

    // ✅ Enviar imediatamente ao montar
    sendHeartbeat();

    // ✅ Configurar intervalo de 60 segundos
    intervalRef.current = setInterval(() => {
      sendHeartbeat();
    }, 60000); // 60 segundos

    // ✅ Cleanup ao desmontar
    return () => {
      console.log('🔇 Heartbeat desativado (componente desmontado)');
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [isActive, username, stompClient]);

  // ✅ Retorna função para forçar envio manual (opcional)
  return {
    sendNow: () => {
      if (stompClient && stompClient.connected) {
        stompClient.send('/app/heartbeat', {}, JSON.stringify({
          username: username,
          timestamp: Date.now()
        }));
      }
    }
  };
};

// ✅ Função alternativa via HTTP (se WebSocket não estiver disponível)
const sendHeartbeatViaHttp = async (username, token) => {
  try {
    await fetch('http://localhost:8080/crypto-monitor/api/heartbeat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        username: username,
        timestamp: Date.now()
      })
    });
    console.log('💓 Heartbeat enviado via HTTP:', username);
  } catch (error) {
    console.warn('⚠️ Erro ao enviar heartbeat via HTTP:', error);
  }
};

export default useHeartbeat;