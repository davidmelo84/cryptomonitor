// front/crypto-monitor-frontend/src/components/pages/DashboardPage.jsx
// ✅ VERSÃO FINAL - Com Modal Telegram + Gráficos + Tudo funcionando

import React, { useState } from 'react';
import { useTheme } from '../../contexts/ThemeContext';
import Header from '../dashboard/Header';
import StatusCard from '../dashboard/StatusCard';
import StatsCards from '../dashboard/StatsCards';
import SettingsCard from '../dashboard/SettingsCard';
import CryptocurrenciesCard from '../dashboard/CryptocurrenciesCard';
import ChartTabs from '../dashboard/ChartTabs';
import TelegramConfig from '../telegram/TelegramConfig';
import '../../styles/TelegramModal.css'; // ✅ IMPORTAR CSS DO MODAL

function DashboardPage({
  user,
  lastUpdate,
  isRefreshing,
  onRefresh,
  onLogout,
  isMonitoring,
  selectedCryptos,
  monitoringInterval,
  onStartStopMonitoring,
  monitoringEmail,
  setMonitoringEmail,
  setMonitoringInterval,
  buyThreshold,
  setBuyThreshold,
  sellThreshold,
  setSellThreshold,
  availableCryptos,
  onToggleCryptoSelection,
  onClearSelection,
  onNavigateToPortfolio,
  onNavigateToBots
}) {
  const { isDark } = useTheme();
  
  // ✅ ESTADO DO MODAL TELEGRAM
  const [showTelegramConfig, setShowTelegramConfig] = useState(false);

  return (
    <div className={`page-container ${isDark ? 'dark' : ''}`}>
      <Header
        user={user}
        lastUpdate={lastUpdate}
        isRefreshing={isRefreshing}
        onRefresh={onRefresh}
        onLogout={onLogout}
        onNavigateToPortfolio={onNavigateToPortfolio}
        onNavigateToBots={onNavigateToBots}
        onOpenTelegramConfig={() => setShowTelegramConfig(true)} // ✅ ABRIR MODAL
      />

      <div className="content-wrapper">
        {/* Status Card */}
        <StatusCard
          isMonitoring={isMonitoring}
          selectedCryptos={selectedCryptos}
          monitoringInterval={monitoringInterval}
          onStartStop={onStartStopMonitoring}
        />

        {/* Stats Cards */}
        {selectedCryptos.length > 0 && (
          <StatsCards
            selectedCryptos={selectedCryptos}
            isMonitoring={isMonitoring}
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
      </div>

      {/* ✅ MODAL TELEGRAM CONFIG */}
      {showTelegramConfig && (
        <div 
          className="telegram-modal-overlay" 
          onClick={() => setShowTelegramConfig(false)}
        >
          <div 
            className={`telegram-modal-content ${isDark ? 'dark' : ''}`}
            onClick={(e) => e.stopPropagation()}
          >
            {/* Header do Modal */}
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

            {/* Body do Modal */}
            <div className="telegram-modal-body">
              <TelegramConfig 
                userEmail={monitoringEmail}
                token={localStorage.getItem('token')}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default DashboardPage;