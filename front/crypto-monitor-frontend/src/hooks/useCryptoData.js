// front/crypto-monitor-frontend/src/hooks/useCryptoData.js
// ============================================
// ✅ VERSÃO FINAL — TOKEN PERSISTENTE + TIMEOUT
// ============================================

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { API_BASE_URL } from '../utils/constants';
import { loadAuthData } from '../utils/storage';

// ============================================
// ⚠️ FETCH COM TIMEOUT E TRATAMENTO DE REDE
// ============================================
const fetchWithTimeout = async (url, options, timeout = 10000) => {
  const controller = new AbortController();
  const id = setTimeout(() => controller.abort(), timeout);

  try {
    const response = await fetch(url, {
      ...options,
      signal: controller.signal
    });
    clearTimeout(id);
    return response;
  } catch (error) {
    clearTimeout(id);

    if (error.name === "AbortError") {
      throw new Error("⏳ Timeout: o servidor demorou a responder");
    }

    throw new Error("📡 Erro de rede: verifique sua conexão");
  }
};

// ============================================
// FETCH FUNCTIONS COM TOKEN AUTOMÁTICO
// ============================================

const fetchCryptos = async (token) => {
  console.log('🔍 Buscando criptomoedas...');

  const authToken = token || loadAuthData()?.token;
  if (!authToken) throw new Error('Token não encontrado');

  const response = await fetchWithTimeout(
    `${API_BASE_URL}/crypto/current`,
    {
      headers: { Authorization: `Bearer ${authToken}` }
    },
    10000 // 10s timeout
  );

  if (response.status === 401) {
    localStorage.removeItem('token');
    sessionStorage.removeItem('token');
    window.location.href = "/";
    throw new Error("Sessão expirada");
  }

  if (!response.ok) {
    const error = await response.text();
    console.error("Erro ao buscar cryptos:", error);
    throw new Error("Falha ao carregar dados das criptos");
  }

  const data = await response.json();
  console.log("Cryptos recebidas:", data.length);
  return data;
};

const fetchMonitoringStatus = async (token) => {
  console.log('🔍 Buscando status...');

  const authToken = token || loadAuthData()?.token;
  if (!authToken) throw new Error('Token não encontrado');

  const response = await fetchWithTimeout(
    `${API_BASE_URL}/monitoring/status`,
    {
      headers: { Authorization: `Bearer ${authToken}` }
    },
    10000
  );

  if (response.status === 401) {
    localStorage.removeItem('token');
    sessionStorage.removeItem('token');
    window.location.href = "/";
    throw new Error("Sessão expirada");
  }

  if (!response.ok) {
    const error = await response.text();
    console.error("Erro ao buscar status:", error);
    throw new Error("Falha ao carregar status do monitoramento");
  }

  return await response.json();
};

// ============================================
// HOOKS — TOKEN AUTOMÁTICO + TIMEOUT
// ============================================

export const useCryptos = (token) => {
  return useQuery({
    queryKey: ['cryptos', token],
    queryFn: () => fetchCryptos(token),
    staleTime: 60_000,
    cacheTime: 300_000,
    refetchOnWindowFocus: false,
    retry: 2,
    enabled: !!(token || loadAuthData()?.token),

    select: (data) => data.map(crypto => ({
      coinId: crypto.id || crypto.coinId || crypto.symbol?.toLowerCase(),
      name: crypto.name,
      symbol: crypto.symbol,
      currentPrice: crypto.current_price || crypto.currentPrice || 0,
      priceChange24h: crypto.price_change_percentage_24h || crypto.priceChange24h || 0,
      marketCap: crypto.market_cap || crypto.marketCap || 0
    }))
  });
};

export const useMonitoringStatus = (token) => {
  return useQuery({
    queryKey: ['monitoring-status', token],
    queryFn: () => fetchMonitoringStatus(token),
    staleTime: 30_000,
    retry: 1,
    enabled: !!(token || loadAuthData()?.token)
  });
};

// ============================================
// MUTATIONS (start / stop monitoring)
// ============================================

export const useStartMonitoring = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ email, cryptocurrencies, interval, buyThreshold, sellThreshold, token, telegramConfig }) => {
      const authToken = token || loadAuthData()?.token;

      const payload = {
        email,
        cryptocurrencies,
        checkIntervalMinutes: interval,
        buyThreshold,
        sellThreshold
      };

      // TELEGRAM
      if (telegramConfig?.enabled) {
        const tokenRegex = /^\d+:[A-Za-z0-9_-]+$/;
        const chatIdRegex = /^-?\d+$/;

        if (!tokenRegex.test(telegramConfig.botToken)) {
          throw new Error("Token do Telegram inválido.");
        }
        if (!chatIdRegex.test(telegramConfig.chatId)) {
          throw new Error("Chat ID inválido.");
        }

        payload.telegramConfig = {
          botToken: telegramConfig.botToken,
          chatId: telegramConfig.chatId,
          enabled: true
        };
      }

      const response = await fetchWithTimeout(
        `${API_BASE_URL}/monitoring/start`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${authToken}`
          },
          body: JSON.stringify(payload)
        },
        12000
      );

      if (!response.ok) {
        const errorText = await response.text();
        console.error("Erro no servidor:", errorText);
        throw new Error(errorText || "Erro ao iniciar monitoramento");
      }

      return await response.json();
    },

    onSuccess: () => {
      queryClient.invalidateQueries(['monitoring-status']);
    }
  });
};

export const useStopMonitoring = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (token) => {
      const authToken = token || loadAuthData()?.token;

      const response = await fetchWithTimeout(
        `${API_BASE_URL}/monitoring/stop`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${authToken}`
          }
        },
        8000
      );

      if (!response.ok) {
        throw new Error("Erro ao parar monitoramento");
      }

      return await response.json();
    },

    onSuccess: () => {
      queryClient.invalidateQueries(['monitoring-status']);
    }
  });
};
