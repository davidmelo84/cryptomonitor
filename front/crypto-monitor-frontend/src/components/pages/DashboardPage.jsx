// front/crypto-monitor-frontend/src/components/pages/DashboardPage.jsx
// ✅ VERSÃO CORRIGIDA - Com monitoramento funcionando

import React, { useState, useCallback } from 'react';
import { useTheme } from '../../contexts/ThemeContext';
import Header from '../dashboard/Header';
import StatusCard from '../dashboard/StatusCard';
import StatsCards from '../dashboard/StatsCards';
import SettingsCard from '../dashboard/SettingsCard';
import CryptocurrenciesCard from '../dashboard/CryptocurrenciesCard';
import ChartTabs from '../dashboard/ChartTabs';
import TelegramConfig from '../telegram/TelegramConfig';

// ✅ React Query hooks
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
  const [showTelegramConfig, setShowTelegramConfig] = useState(false);

  // ✅ React Query - buscar dados com cache
  const { 
    data: availableCryptos = [], 
    isLoading: cryptosLoading,
    isRefetching,
    refetch: refetchCryptos 
  } = useCryptos();

  const {
    data: monitoringStatusData,
    refetch: refetchMonitoringStatus
  } = useMonitoringStatus(token);

  const startMonitoringMutation = useStartMonitoring();
  const stopMonitoringMutation = useStopMonitoring();

  // ✅ Derivar estado do monitoramento
  const isMonitoringActive = monitoringStatusData?.active || false;

  // ✅ CORREÇÃO: Handler completo para start/stop
  const handleStartStopMonitoring = useCallback(async () => {
    console.log('🔘 handleStartStopMonitoring chamado');
    console.log('   isMonitoringActive:', isMonitoringActive);
    console.log('   monitoringEmail:', monitoringEmail);
    console.log('   selectedCryptos:', selectedCryptos.length);

    if (isMonitoringActive) {
      // ========== PARAR MONITORAMENTO ==========
      console.log('🛑 Tentando parar monitoramento...');
      
      try {
        const result = await stopMonitoringMutation.mutateAsync(token);
        console.log('✅ Monitoramento parado:', result);
        
        // Atualizar status
        refetchMonitoringStatus();
        
      } catch (error) {
        console.error('❌ Erro ao parar:', error);
        alert('Erro ao parar monitoramento: ' + error.message);
      }
      
    } else {
      // ========== INICIAR MONITORAMENTO ==========
      console.log('▶️ Tentando iniciar monitoramento...');
      
      // ✅ VALIDAÇÃO COMPLETA
      if (!monitoringEmail || monitoringEmail.trim() === '') {
        alert('⚠️ Configure um email válido antes de iniciar!');
        return;
      }

      if (selectedCryptos.length === 0) {
        alert('⚠️ Selecione pelo menos uma criptomoeda!');
        return;
      }

      try {
        // ✅ Extrair coinIds das cryptos selecionadas
        const cryptocurrencies = selectedCryptos.map(c => {
          // Tentar obter coinId de várias formas
          return c.coinId || c.id || c.symbol?.toLowerCase() || c.name?.toLowerCase();
        });

        console.log('📤 Enviando dados:', {
          email: monitoringEmail,
          cryptocurrencies,
          interval: monitoringInterval,
          buyThreshold,
          sellThreshold
        });

        const result = await startMonitoringMutation.mutateAsync({
          email: monitoringEmail,
          cryptocurrencies,
          interval: monitoringInterval,
          buyThreshold,
          sellThreshold,
          token
        });

        console.log('✅ Monitoramento iniciado:', result);
        
        // Atualizar status
        refetchMonitoringStatus();
        
        alert(`✅ Monitoramento iniciado!\n\n• Email: ${monitoringEmail}\n• Moedas: ${cryptocurrencies.length}\n• Intervalo: ${monitoringInterval} min`);
        
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
    startMonitoringMutation,
    stopMonitoringMutation,
    refetchMonitoringStatus
  ]);

  // ✅ Handler de refresh
  const handleRefresh = () => {
    refetchCryptos();
    refetchMonitoringStatus();
  };

  // ✅ Último update
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
            {/* ✅ CORREÇÃO: Passar handler correto */}
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

      {/* Modal Telegram */}
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
                token={token}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default DashboardPage;