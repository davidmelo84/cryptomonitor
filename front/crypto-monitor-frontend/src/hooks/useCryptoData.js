// front/crypto-monitor-frontend/src/hooks/useCryptoData.js
// ✅ CORRIGIDO - Normalização adequada dos dados

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { API_BASE_URL } from '../utils/constants';

// ============================================
// FETCH FUNCTIONS
// ============================================

const fetchCryptos = async () => {
  console.log('🔍 Buscando criptomoedas...');
  const response = await fetch(`${API_BASE_URL}/crypto/current`);
  
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
// HOOKS
// ============================================

export const useCryptos = () => {
  return useQuery({
    queryKey: ['cryptos'],
    queryFn: fetchCryptos,
    staleTime: 60 * 1000,
    cacheTime: 5 * 60 * 1000,
    refetchOnWindowFocus: true,
    retry: 2,
    
    // ✅ Normalizar dados (API retorna 'id', mas precisamos de 'coinId')
    select: (data) => {
      return data.map(crypto => ({
        coinId: crypto.id || crypto.coinId || crypto.symbol?.toLowerCase(), // ✅ CORREÇÃO
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
    queryKey: ['monitoring-status'],
    queryFn: () => fetchMonitoringStatus(token),
    staleTime: 30 * 1000,
    enabled: !!token,
    retry: 1
  });
};

export const useStartMonitoring = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ email, cryptocurrencies, interval, buyThreshold, sellThreshold, token }) => {
      console.log('📤 Iniciando monitoramento...');
      console.log('   Email:', email);
      console.log('   Cryptos:', cryptocurrencies);
      
      const payload = {
        email,
        cryptocurrencies,
        checkIntervalMinutes: interval,
        buyThreshold,
        sellThreshold
      };
      
      console.log('📦 Payload:', JSON.stringify(payload, null, 2));
      
      const response = await fetch(`${API_BASE_URL}/monitoring/start`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(payload)
      });
      
      console.log('📡 Response status:', response.status);
      
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