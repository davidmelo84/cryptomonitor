// front/crypto-monitor-frontend/src/hooks/useCryptoData.js
// ✅ VERSÃO FINAL COM VALIDAÇÃO DE TELEGRAM

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { API_BASE_URL } from '../utils/constants';

// ============================================
// FETCH FUNCTIONS - COM TOKEN JWT
// ============================================

const fetchCryptos = async (token) => {
  console.log('🔍 Buscando criptomoedas...');
  
  const response = await fetch(`${API_BASE_URL}/crypto/current`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  if (!response.ok) {
    const error = await response.text();
    console.error('❌ Erro ao buscar cryptos:', error);
    throw new Error('Failed to fetch cryptos');
  }
  
  const data = await response.json();
  console.log('✅ Cryptos recebidas:', data.length);
  return data;
};

const fetchMonitoringStatus = async (token) => {
  console.log('🔍 Buscando status do monitoramento...');
  const response = await fetch(`${API_BASE_URL}/monitoring/status`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  
  if (!response.ok) {
    const error = await response.text();
    console.error('❌ Erro ao buscar status:', error);
    throw new Error('Failed to fetch status');
  }
  
  const data = await response.json();
  console.log('✅ Status recebido:', data);
  return data;
};

// ============================================
// HOOKS - ✅ RECEBEM TOKEN COMO PARÂMETRO
// ============================================

export const useCryptos = (token) => {
  return useQuery({
    queryKey: ['cryptos', token],
    queryFn: () => fetchCryptos(token),
    staleTime: 60 * 1000,
    cacheTime: 5 * 60 * 1000,
    refetchOnWindowFocus: false,
    retry: 2,
    enabled: !!token,
    
    select: (data) => {
      return data.map(crypto => ({
        coinId: crypto.id || crypto.coinId || crypto.symbol?.toLowerCase(),
        name: crypto.name,
        symbol: crypto.symbol,
        currentPrice: crypto.current_price || crypto.currentPrice || 0,
        priceChange24h: crypto.price_change_percentage_24h || crypto.priceChange24h || 0,
        marketCap: crypto.market_cap || crypto.marketCap || 0
      }));
    }
  });
};

export const useMonitoringStatus = (token) => {
  return useQuery({
    queryKey: ['monitoring-status', token],
    queryFn: () => fetchMonitoringStatus(token),
    staleTime: 30 * 1000,
    enabled: !!token,
    retry: 1
  });
};

export const useStartMonitoring = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ email, cryptocurrencies, interval, buyThreshold, sellThreshold, token, telegramConfig }) => {
      console.log('📤 Iniciando monitoramento...');
      
      const payload = {
        email,
        cryptocurrencies,
        checkIntervalMinutes: interval,
        buyThreshold,
        sellThreshold
      };
      
      // ✅ Validar e adicionar Telegram se configurado
      if (telegramConfig?.enabled && telegramConfig?.botToken && telegramConfig?.chatId) {
        // Validação do formato
        const tokenRegex = /^\d+:[A-Za-z0-9_-]+$/;
        const chatIdRegex = /^-?\d+$/;
        
        if (!tokenRegex.test(telegramConfig.botToken)) {
          throw new Error('⚠️ Token do Telegram inválido. O formato deve ser semelhante a "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11".');
        }
        
        if (!chatIdRegex.test(telegramConfig.chatId)) {
          throw new Error('⚠️ Chat ID do Telegram inválido. Deve conter apenas números (ex: 123456789).');
        }

        payload.telegramConfig = {
          botToken: telegramConfig.botToken,
          chatId: telegramConfig.chatId,
          enabled: true
        };
      }
      
      console.log('📦 Payload:', JSON.stringify(payload, null, 2));
      
      const response = await fetch(`${API_BASE_URL}/monitoring/start`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(payload)
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        console.error('❌ Erro do servidor:', errorText);
        
        try {
          const errorData = JSON.parse(errorText);
          throw new Error(errorData.message || errorData.error || 'Failed to start monitoring');
        } catch {
          throw new Error(`Server error: ${response.status}`);
        }
      }
      
      const data = await response.json();
      console.log('✅ Resposta:', data);
      return data;
    },
    
    onSuccess: (data) => {
      console.log('✅ Monitoramento iniciado!', data);
      queryClient.invalidateQueries({ queryKey: ['monitoring-status'] });
    },
    
    onError: (error) => {
      console.error('❌ Erro:', error);
    }
  });
};

export const useStopMonitoring = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (token) => {
      console.log('🛑 Parando monitoramento...');
      
      const response = await fetch(`${API_BASE_URL}/monitoring/stop`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        }
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        console.error('❌ Erro:', errorText);
        throw new Error('Failed to stop monitoring');
      }
      
      const data = await response.json();
      console.log('✅ Parado:', data);
      return data;
    },
    
    onSuccess: () => {
      console.log('✅ Monitoramento parado!');
      queryClient.invalidateQueries({ queryKey: ['monitoring-status'] });
    }
  });
};
