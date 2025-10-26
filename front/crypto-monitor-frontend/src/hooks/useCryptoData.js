// front/crypto-monitor-frontend/src/hooks/useCryptoData.js
// ✅ Hook customizado com cache automático

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { API_BASE_URL } from '../utils/constants';

// ============================================
// FETCH FUNCTIONS (sem duplicação)
// ============================================

const fetchCryptos = async () => {
  const response = await fetch(`${API_BASE_URL}/crypto/current`);
  if (!response.ok) throw new Error('Failed to fetch cryptos');
  return response.json();
};

const fetchMonitoringStatus = async (token) => {
  const response = await fetch(`${API_BASE_URL}/monitoring/status`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!response.ok) throw new Error('Failed to fetch status');
  return response.json();
};

// ============================================
// HOOKS (com cache automático)
// ============================================

/**
 * ✅ Hook para buscar criptomoedas
 * Cache: 60 segundos
 * Refetch automático em foco da janela
 */
export const useCryptos = () => {
  return useQuery({
    queryKey: ['cryptos'],
    queryFn: fetchCryptos,
    staleTime: 60 * 1000, // 60s cache
    cacheTime: 5 * 60 * 1000, // 5min no cache
    refetchOnWindowFocus: true,
    retry: 2,
    
    // ✅ NORMALIZAÇÃO: Evita re-renders se dados não mudaram
    select: (data) => {
      return data.map(crypto => ({
        coinId: crypto.id,
        name: crypto.name,
        symbol: crypto.symbol,
        currentPrice: crypto.current_price || 0,
        priceChange24h: crypto.price_change_percentage_24h || 0,
        marketCap: crypto.market_cap || 0
      }));
    }
  });
};

/**
 * ✅ Hook para status do monitoramento
 * Cache: 30 segundos
 */
export const useMonitoringStatus = (token) => {
  return useQuery({
    queryKey: ['monitoring-status'],
    queryFn: () => fetchMonitoringStatus(token),
    staleTime: 30 * 1000,
    enabled: !!token, // Só executa se tiver token
    retry: 1
  });
};

/**
 * ✅ Hook para iniciar monitoramento
 * Invalida cache automaticamente após sucesso
 */
export const useStartMonitoring = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ email, cryptocurrencies, interval, buyThreshold, sellThreshold, token }) => {
      const response = await fetch(`${API_BASE_URL}/monitoring/start`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          email,
          cryptocurrencies,
          checkIntervalMinutes: interval,
          buyThreshold,
          sellThreshold
        })
      });
      
      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Failed to start monitoring');
      }
      
      return response.json();
    },
    
    // ✅ Atualiza cache automaticamente
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['monitoring-status'] });
    }
  });
};

/**
 * ✅ Hook para parar monitoramento
 */
export const useStopMonitoring = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (token) => {
      const response = await fetch(`${API_BASE_URL}/monitoring/stop`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        }
      });
      
      if (!response.ok) throw new Error('Failed to stop monitoring');
      return response.json();
    },
    
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['monitoring-status'] });
    }
  });
};