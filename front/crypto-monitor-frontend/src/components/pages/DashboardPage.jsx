// front/crypto-monitor-frontend/src/components/pages/DashboardPage.jsx
// ✅ VERSÃO COM INTEGRAÇÃO TELEGRAM CONTEXT

import React, { useState, useCallback } from 'react';
import { useTheme } from '../../contexts/ThemeContext';
import { useTelegram } from '../../contexts/TelegramContext';
import Header from '../dashboard/Header';
import StatusCard from '../dashboard/StatusCard';
import StatsCards from '../dashboard/StatsCards';
import SettingsCard from '../dashboard/SettingsCard';
import CryptocurrenciesCard from '../dashboard/CryptocurrenciesCard';
import ChartTabs from '../dashboard/ChartTabs';
import TelegramConfig from '../telegram/TelegramConfig';

import { 
  useCryptos, 
  useMonitoringStatus,
  useStartMonitoring,
  useStopMonitoring
} from '../../hooks/useCryptoData';

import '../../styles/TelegramModal.css';

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
  
  // ✅ USAR TELEGRAM CONTEXT
  const { telegramConfig, isConfigured } = useTelegram();
  
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

  // ✅ HANDLER COM INTEGRAÇÃO TELEGRAM
  const handleStartStopMonitoring = useCallback(async () => {
    console.log('🔘 handleStartStopMonitoring chamado');
    console.log('   isMonitoringActive:', isMonitoringActive);
    console.log('   monitoringEmail:', monitoringEmail);
    console.log('   selectedCryptos:', selectedCryptos.length);
    console.log('   Telegram configurado?', isConfigured());
    console.log('   Telegram habilitado?', telegramConfig.enabled);

    if (isMonitoringActive) {
      // ========== PARAR MONITORAMENTO ==========
      console.log('🛑 Tentando parar monitoramento...');
      
      try {
        const result = await stopMonitoringMutation.mutateAsync(token);
        console.log('✅ Monitoramento parado:', result);
        refetchMonitoringStatus();
      } catch (error) {
        console.error('❌ Erro ao parar:', error);
        alert('Erro ao parar monitoramento: ' + error.message);
      }
      
    } else {
      // ========== INICIAR MONITORAMENTO ==========
      console.log('▶️ Tentando iniciar monitoramento...');
      
      // ✅ VALIDAÇÕES
      if (!monitoringEmail || monitoringEmail.trim() === '') {
        alert('⚠️ Configure um email válido antes de iniciar!');
        return;
      }

      if (selectedCryptos.length === 0) {
        alert('⚠️ Selecione pelo menos uma criptomoeda!');
        return;
      }

      // ✅ AVISO SE TELEGRAM HABILITADO MAS NÃO TESTADO
      if (telegramConfig.enabled && !telegramConfig.isConnected) {
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
        const cryptocurrencies = selectedCryptos.map(c => {
          return c.coinId || c.id || c.symbol?.toLowerCase() || c.name?.toLowerCase();
        });

        // ✅ PREPARAR PAYLOAD COM TELEGRAM
        const monitoringPayload = {
          email: monitoringEmail,
          cryptocurrencies,
          interval: monitoringInterval,
          buyThreshold,
          sellThreshold,
          token
        };

        // ✅ ADICIONAR CONFIGS DO TELEGRAM SE HABILITADO
        if (telegramConfig.enabled && isConfigured()) {
          monitoringPayload.telegramConfig = {
            botToken: telegramConfig.botToken,
            chatId: telegramConfig.chatId,
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
        
        let alertMessage = `✅ Monitoramento iniciado!\n\n` +
                          `• Email: ${monitoringEmail}\n` +
                          `• Moedas: ${cryptocurrencies.length}\n` +
                          `• Intervalo: ${monitoringInterval} min`;
        
        if (telegramConfig.enabled && isConfigured()) {
          alertMessage += `\n• Telegram: Habilitado ✅`;
        }
        
        alert(alertMessage);
        
      } catch (error) {
        console.error('❌ Erro ao iniciar:', error);
        alert('Erro ao iniciar monitoramento: ' + error.message);
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
    telegramConfig,
    isConfigured,
    startMonitoringMutation,
    stopMonitoringMutation,
    refetchMonitoringStatus
  ]);

  const handleRefresh = () => {
    refetchCryptos();
    refetchMonitoringStatus();
  };

  const lastUpdate = cryptosLoading ? null : new Date();

  return (
    <div className={`page-container ${isDark ? 'dark' : ''}`}>
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
          <div className="text-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-500 mx-auto mb-4" />
            <p className="text-gray-600">Carregando criptomoedas...</p>
          </div>
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

      {/* ✅ Modal Telegram - Agora persiste dados */}
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
              <TelegramConfig 
                userEmail={monitoringEmail}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default DashboardPage;