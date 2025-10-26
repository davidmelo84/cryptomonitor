// front/crypto-monitor-frontend/src/components/pages/DashboardPage.jsx
// ✅ VERSÃO OTIMIZADA - Usando React Query

import React, { useState } from 'react';
import { useTheme } from '../../contexts/ThemeContext';
import Header from '../dashboard/Header';
import StatusCard from '../dashboard/StatusCard';
import StatsCards from '../dashboard/StatsCards';
import SettingsCard from '../dashboard/SettingsCard';
import CryptocurrenciesCard from '../dashboard/CryptocurrenciesCard';
import ChartTabs from '../dashboard/ChartTabs';
import TelegramConfig from '../telegram/TelegramConfig';

// ✅ NOVO: Importar hooks do React Query
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
  isMonitoring,
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

  // ✅ NOVO: Usar React Query para buscar dados (com cache automático)
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

  // ✅ Derivar estado do monitoramento do cache
  const isMonitoringActive = monitoringStatusData?.active || false;

  // ✅ Handler para iniciar/parar monitoramento
  const handleStartStopMonitoring = async () => {
    if (isMonitoringActive) {
      // Parar monitoramento
      try {
        await stopMonitoringMutation.mutateAsync(token);
      } catch (error) {
        alert('Erro ao parar monitoramento: ' + error.message);
      }
    } else {
      // Iniciar monitoramento
      if (!monitoringEmail || selectedCryptos.length === 0) {
        alert('Configure o email e selecione pelo menos uma criptomoeda');
        return;
      }

      try {
        await startMonitoringMutation.mutateAsync({
          email: monitoringEmail,
          cryptocurrencies: selectedCryptos.map(c => c.coinId || c.name),
          interval: monitoringInterval,
          buyThreshold,
          sellThreshold,
          token
        });
      } catch (error) {
        alert('Erro ao iniciar monitoramento: ' + error.message);
      }
    }
  };

  // ✅ Handler de refresh (usa refetch do React Query)
  const handleRefresh = () => {
    refetchCryptos();
    refetchMonitoringStatus();
  };

  // ✅ Último update (derivado dos dados)
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
        {/* ✅ Loading State */}
        {cryptosLoading ? (
          <div className="text-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-500 mx-auto mb-4" />
            <p className="text-gray-600">Carregando criptomoedas...</p>
          </div>
        ) : (
          <>
            {/* Status Card */}
            <StatusCard
              isMonitoring={isMonitoringActive}
              selectedCryptos={selectedCryptos}
              monitoringInterval={monitoringInterval}
              onStartStop={handleStartStopMonitoring}
            />

            {/* Stats Cards */}
            {selectedCryptos.length > 0 && (
              <StatsCards
                selectedCryptos={selectedCryptos}
                isMonitoring={isMonitoringActive}
              />
            )}

            {/* Gráficos */}
            {selectedCryptos.length > 0 && (
              <ChartTabs selectedCryptos={selectedCryptos} />
            )}

            {/* Settings Card */}
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

            {/* Cryptocurrencies Card */}
            <CryptocurrenciesCard
              availableCryptos={availableCryptos}
              selectedCryptos={selectedCryptos}
              onToggleSelection={onToggleCryptoSelection}
              onClearSelection={onClearSelection}
            />
          </>
        )}
      </div>

      {/* Modal Telegram Config */}
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
                aria-label="Fechar"
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