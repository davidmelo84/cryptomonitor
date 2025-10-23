// front/crypto-monitor-frontend/src/components/pages/DashboardPage.jsx
// ✅ VERSÃO CORRIGIDA - Com gráficos funcionando

import React from 'react';
import { useTheme } from '../../contexts/ThemeContext';
import Header from '../dashboard/Header';
import StatusCard from '../dashboard/StatusCard';
import StatsCards from '../dashboard/StatsCards';
import SettingsCard from '../dashboard/SettingsCard';
import CryptocurrenciesCard from '../dashboard/CryptocurrenciesCard';
import ChartTabs from '../dashboard/ChartTabs'; // ✅ ADICIONADO

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

        {/* ✅ GRÁFICOS - ADICIONADO */}
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
    </div>
  );
}

export default DashboardPage;