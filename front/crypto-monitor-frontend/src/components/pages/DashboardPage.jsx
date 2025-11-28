// front/crypto-monitor-frontend/src/components/pages/DashboardPage.jsx
// ✅ COM TOAST E SKELETON - SUBSTITUA O ARQUIVO COMPLETO

import React, { useState, useCallback } from 'react';
import { useTheme } from '../../contexts/ThemeContext';
import { useTelegram } from '../../contexts/TelegramContext';
import { useToast } from '../common/Toast'; // ✅ NOVO
import { CryptoCardSkeleton } from '../common/Skeleton'; // ✅ NOVO

import Header from '../dashboard/Header';
import StatusCard from '../dashboard/StatusCard';
import StatsCards from '../dashboard/StatsCards';
import SettingsCard from '../dashboard/SettingsCard';
import CryptocurrenciesCard from '../dashboard/CryptocurrenciesCard';
import ChartTabs from '../dashboard/ChartTabs';
import TelegramConfig from '../telegram/TelegramConfig';
import '../../styles/components/dashboard.css';
import '../../styles/components/telegram.css';

import { 
  useCryptos, 
  useMonitoringStatus,
  useStartMonitoring,
  useStopMonitoring
} from '../../hooks/useCryptoData';

import useHeartbeat from '../../hooks/useHeartbeat'; // ✅ NOVO IMPORT

function DashboardPage({
  user,
  token,
  onLogout,
  selectedCryptos,
  monitoringInterval,
  monitoringEmail,
  setMonitoringEmail,
  setMonitoringInterval,
  buyThreshold,
  setBuyThreshold,
  sellThreshold,
  setSellThreshold,
  onToggleCryptoSelection,
  onClearSelection,
  onNavigateToPortfolio,
  onNavigateToBots
}) {
  const { isDark } = useTheme();
  const { telegramConfig, isConfigured } = useTelegram();
  const { showToast, ToastContainer } = useToast({maxToasts: 3});
  
  const [showTelegramConfig, setShowTelegramConfig] = useState(false);

  const { 
    data: availableCryptos = [], 
    isLoading: cryptosLoading,
    isRefetching,
    refetch: refetchCryptos 
  } = useCryptos(token);

  const {
    data: monitoringStatusData,
    refetch: refetchMonitoringStatus
  } = useMonitoringStatus(token);

  const startMonitoringMutation = useStartMonitoring();
  const stopMonitoringMutation = useStopMonitoring();

  const isMonitoringActive = monitoringStatusData?.active || false;

  // ✅ NOVO: Ativar heartbeat quando monitoramento está ativo
  useHeartbeat(
    isMonitoringActive && !!user,
    user?.username,
    null
  );

  // ✅ SIMPLIFICADO — Removido useMemo desnecessário
  const telegramConfigMemo = telegramConfig;

  const handleStartStopMonitoring = useCallback(async () => {
    console.log('🔘 handleStartStopMonitoring chamado');

    if (isMonitoringActive) {
      // ========== PARAR MONITORAMENTO ==========
      console.log('🛑 Tentando parar monitoramento...');
      
      try {
        const result = await stopMonitoringMutation.mutateAsync(token);
        console.log('✅ Monitoramento parado:', result);
        refetchMonitoringStatus();
        
        showToast('Monitoramento parado com sucesso!', 'info');
        
      } catch (error) {
        console.error('❌ Erro ao parar:', error);
        showToast('Erro ao parar monitoramento: ' + error.message, 'error');
      }
      
    } else {
      // ========== INICIAR MONITORAMENTO ==========
      console.log('▶️ Tentando iniciar monitoramento...');
      
      if (!monitoringEmail || monitoringEmail.trim() === '') {
        showToast('Configure um email válido antes de iniciar!', 'error');
        return;
      }

      if (selectedCryptos.length === 0) {
        showToast('Selecione pelo menos uma criptomoeda!', 'error');
        return;
      }

      if (telegramConfigMemo.enabled && !telegramConfigMemo.isConnected) {
        const confirmStart = window.confirm(
          '⚠️ Telegram está habilitado mas não foi testado.\n\n' +
          'Deseja continuar mesmo assim?\n\n' +
          'Clique em "Cancelar" para testar a conexão primeiro.'
        );
        
        if (!confirmStart) {
          setShowTelegramConfig(true);
          return;
        }
      }

      try {
        const cryptocurrencies = selectedCryptos.map(c =>
          c.coinId || c.id || c.symbol?.toLowerCase() || c.name?.toLowerCase()
        );

        const monitoringPayload = {
          email: monitoringEmail,
          cryptocurrencies,
          interval: monitoringInterval,
          buyThreshold,
          sellThreshold,
          token
        };

        if (telegramConfigMemo.enabled && isConfigured()) {
          monitoringPayload.telegramConfig = {
            botToken: telegramConfigMemo.botToken,
            chatId: telegramConfigMemo.chatId,
            enabled: true
          };
          console.log('📱 Telegram será usado para notificações');
        }

        console.log('📤 Enviando dados:', {
          ...monitoringPayload,
          telegramConfig: monitoringPayload.telegramConfig
            ? '***CONFIGURADO***'
            : 'NÃO CONFIGURADO'
        });

        const result = await startMonitoringMutation.mutateAsync(monitoringPayload);

        console.log('✅ Monitoramento iniciado:', result);
        refetchMonitoringStatus();
        
        let message = `Monitoramento iniciado! ${cryptocurrencies.length} moeda(s) sendo monitorada(s).`;
        if (telegramConfigMemo.enabled && isConfigured()) {
          message += ' Telegram ativo!';
        }
        showToast(message, 'success', 4000);
        
      } catch (error) {
        console.error('❌ Erro ao iniciar:', error);
        showToast('Erro ao iniciar monitoramento: ' + error.message, 'error');
      }
    }
  }, [
    isMonitoringActive,
    monitoringEmail,
    selectedCryptos,
    monitoringInterval,
    buyThreshold,
    sellThreshold,
    token,
    telegramConfigMemo,
    isConfigured,
    startMonitoringMutation,
    stopMonitoringMutation,
    refetchMonitoringStatus,
    showToast
  ]);

  const handleRefresh = () => {
    refetchCryptos();
    refetchMonitoringStatus();
    showToast('Dados atualizados!', 'info', 2000);
  };

  const lastUpdate = cryptosLoading ? null : new Date();

  return (
    <div className={`page-container ${isDark ? 'dark' : ''}`}>
      <ToastContainer />
      
      <Header
        user={user}
        lastUpdate={lastUpdate}
        isRefreshing={isRefetching}
        onRefresh={handleRefresh}
        onLogout={onLogout}
        onNavigateToPortfolio={onNavigateToPortfolio}
        onNavigateToBots={onNavigateToBots}
        onOpenTelegramConfig={() => setShowTelegramConfig(true)}
      />

      <div className="content-wrapper">
        {cryptosLoading ? (
          <>
            <div style={{ 
              background: isDark ? '#1f2937' : 'white',
              padding: '2rem',
              borderRadius: '20px',
              marginBottom: '2rem',
              boxShadow: '0 4px 15px rgba(0, 0, 0, 0.1)'
            }}>
              <div style={{ textAlign: 'center', color: isDark ? '#9ca3af' : '#6b7280' }}>
                Carregando criptomoedas...
              </div>
            </div>
            
            <div className="cryptocurrencies-grid" style={{ 
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
              gap: '1.25rem',
              marginTop: '2rem'
            }}>
              {[...Array(6)].map((_, i) => (
                <CryptoCardSkeleton key={i} />
              ))}
            </div>
          </>
        ) : (
          <>
            <StatusCard
              isMonitoring={isMonitoringActive}
              onStartStop={handleStartStopMonitoring}
              selectedCryptos={selectedCryptos}
              monitoringEmail={monitoringEmail}
            />

            {selectedCryptos.length > 0 && (
              <StatsCards
                selectedCryptos={selectedCryptos}
                isMonitoring={isMonitoringActive}
              />
            )}

            {selectedCryptos.length > 0 && (
              <ChartTabs selectedCryptos={selectedCryptos} />
            )}

            <SettingsCard
              monitoringEmail={monitoringEmail}
              setMonitoringEmail={setMonitoringEmail}
              monitoringInterval={monitoringInterval}
              setMonitoringInterval={setMonitoringInterval}
              buyThreshold={buyThreshold}
              setBuyThreshold={setBuyThreshold}
              sellThreshold={sellThreshold}
              setSellThreshold={setSellThreshold}
            />

            <CryptocurrenciesCard
              availableCryptos={availableCryptos}
              selectedCryptos={selectedCryptos}
              onToggleSelection={onToggleCryptoSelection}
              onClearSelection={onClearSelection}
            />
          </>
        )}
      </div>

      {showTelegramConfig && (
        <div 
          className="telegram-modal-overlay" 
          onClick={() => setShowTelegramConfig(false)}
        >
          <div 
            className={`telegram-modal-content ${isDark ? 'dark' : ''}`}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="telegram-modal-header">
              <h2 className="telegram-modal-title">
                📱 Configuração do Telegram
              </h2>
              <button 
                className="telegram-modal-close"
                onClick={() => setShowTelegramConfig(false)}
              >
                ✕
              </button>
            </div>

            <div className="telegram-modal-body">
              <TelegramConfig userEmail={monitoringEmail} />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default DashboardPage;
