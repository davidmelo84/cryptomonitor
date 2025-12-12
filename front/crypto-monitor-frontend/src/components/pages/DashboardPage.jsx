// front/crypto-monitor-frontend/src/components/pages/DashboardPage.jsx
// ✅ VERSÃO CORRIGIDA - Botões Trading Bots e Telegram no Header

import React, { useState } from 'react';
import { useTheme } from '../../contexts/ThemeContext';
import { useTelegram } from '../../contexts/TelegramContext';
import { useToast } from '../common/Toast';
import { CryptoCardSkeleton } from '../common/Skeleton';

import { 
  TrendingUp, LogOut, User, RefreshCw, Settings, Bell, 
  Wallet, Bot, Send, Search, X, BarChart3, PlayCircle, 
  StopCircle, Mail, Clock, DollarSign
} from 'lucide-react';

import TelegramConfig from '../telegram/TelegramConfig';
import ChartTabs from '../dashboard/ChartTabs';

import { 
  useCryptos, 
  useMonitoringStatus,
  useStartMonitoring,
  useStopMonitoring
} from '../../hooks/useCryptoData';

import useHeartbeat from '../../hooks/useHeartbeat';

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
  const { showToast, ToastContainer } = useToast({ maxToasts: 3 });
  
  const [showTelegramConfig, setShowTelegramConfig] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [showSettings, setShowSettings] = useState(false);

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

  useHeartbeat(
    isMonitoringActive && !!user,
    user?.username,
    null
  );

  const handleStartStopMonitoring = async () => {
    if (isMonitoringActive) {
      try {
        await stopMonitoringMutation.mutateAsync(token);
        refetchMonitoringStatus();
        showToast('Monitoramento parado com sucesso!', 'info');
      } catch (error) {
        showToast('Erro ao parar monitoramento: ' + error.message, 'error');
      }
    } else {
      if (!monitoringEmail || monitoringEmail.trim() === '') {
        showToast('Configure um email válido antes de iniciar!', 'error');
        setShowSettings(true);
        return;
      }

      if (selectedCryptos.length === 0) {
        showToast('Selecione pelo menos uma criptomoeda!', 'error');
        return;
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

        if (telegramConfig.enabled && isConfigured()) {
          monitoringPayload.telegramConfig = {
            botToken: telegramConfig.botToken,
            chatId: telegramConfig.chatId,
            enabled: true
          };
        }

        console.log('🚀 Iniciando monitoramento:', monitoringPayload);
        
        await startMonitoringMutation.mutateAsync(monitoringPayload);
        refetchMonitoringStatus();
        
        let message = `✅ Monitoramento iniciado! ${cryptocurrencies.length} moeda(s) sendo monitorada(s).`;
        if (telegramConfig.enabled && isConfigured()) {
          message += ' Telegram ativo!';
        }
        showToast(message, 'success', 4000);
        
      } catch (error) {
        console.error('❌ Erro ao iniciar:', error);
        showToast('Erro ao iniciar monitoramento: ' + error.message, 'error');
      }
    }
  };

  const handleRefresh = () => {
    refetchCryptos();
    refetchMonitoringStatus();
    showToast('Dados atualizados!', 'info', 2000);
  };

  const filteredCryptos = availableCryptos.filter(crypto => 
    crypto.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    crypto.symbol?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const avgChange = selectedCryptos.length > 0 
    ? selectedCryptos.reduce((sum, c) => sum + (c.priceChange24h || 0), 0) / selectedCryptos.length 
    : 0;

  return (
    <div className="min-h-screen relative overflow-hidden">
      <ToastContainer />

      {/* Background */}
      <div 
        className="absolute inset-0 bg-cover bg-center bg-no-repeat"
        style={{
          backgroundImage: "url('https://images.unsplash.com/photo-1639762681485-074b7f938ba0?q=80&w=2832')",
        }}
      >
        <div className="absolute inset-0 bg-gradient-to-br from-slate-900/95 via-indigo-900/90 to-purple-900/95" />
      </div>

      {/* Animated Grid */}
      <div className="absolute inset-0 opacity-10">
        <div 
          className="w-full h-full"
          style={{
            backgroundImage: `
              linear-gradient(rgba(102, 126, 234, 0.3) 1px, transparent 1px),
              linear-gradient(90deg, rgba(102, 126, 234, 0.3) 1px, transparent 1px)
            `,
            backgroundSize: '50px 50px',
            animation: 'gridMove 20s linear infinite'
          }}
        />
      </div>

      {/* Content */}
      <div className="relative z-10">
        {/* Header */}
        <header className="backdrop-blur-xl bg-white/5 border-b border-white/10">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex items-center justify-between h-20">
              {/* Logo */}
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center shadow-lg shadow-blue-500/50">
                  <TrendingUp className="w-7 h-7 text-white" />
                </div>
                <div>
                  <h1 className="text-2xl font-bold text-white">Crypto Monitor</h1>
                  <p className="text-sm text-blue-200">Dashboard em tempo real</p>
                </div>
              </div>

              {/* Actions */}
              <div className="flex items-center gap-3">
                <div className="hidden md:flex items-center gap-2 backdrop-blur-sm bg-white/5 px-4 py-2 rounded-xl border border-white/10">
                  <User className="w-4 h-4 text-blue-300" />
                  <span className="text-white text-sm font-medium">{user?.username}</span>
                </div>

                <button
                  onClick={handleRefresh}
                  disabled={isRefetching}
                  className="backdrop-blur-sm bg-white/5 hover:bg-white/10 p-3 rounded-xl border border-white/10 transition-all disabled:opacity-50"
                >
                  <RefreshCw className={`w-5 h-5 text-white ${isRefetching ? 'animate-spin' : ''}`} />
                </button>

                <button 
                  onClick={() => setShowSettings(!showSettings)}
                  className="backdrop-blur-sm bg-white/5 hover:bg-white/10 p-3 rounded-xl border border-white/10 transition-all"
                >
                  <Settings className="w-5 h-5 text-white" />
                </button>

                <button className="backdrop-blur-sm bg-white/5 hover:bg-white/10 p-3 rounded-xl border border-white/10 transition-all">
                  <Bell className="w-5 h-5 text-white" />
                </button>

                <button 
                  onClick={onNavigateToPortfolio}
                  className="backdrop-blur-sm bg-gradient-to-r from-emerald-500/80 to-teal-600/80 hover:from-emerald-500 hover:to-teal-600 px-4 py-3 rounded-xl text-white font-medium flex items-center gap-2 transition-all"
                >
                  <Wallet className="w-5 h-5" />
                  <span className="hidden sm:inline">Portfolio</span>
                </button>

                {/* ✅ Trading Bots Button */}
                <button 
                  onClick={onNavigateToBots}
                  className="backdrop-blur-sm bg-gradient-to-r from-purple-500/80 to-pink-600/80 hover:from-purple-500 hover:to-pink-600 px-4 py-3 rounded-xl text-white font-medium flex items-center gap-2 transition-all"
                >
                  <Bot className="w-5 h-5" />
                  <span className="hidden sm:inline">Trading Bots</span>
                </button>

                {/* ✅ Telegram Button */}
                <button 
                  onClick={() => setShowTelegramConfig(true)}
                  className="backdrop-blur-sm bg-gradient-to-r from-blue-500/80 to-cyan-600/80 hover:from-blue-500 hover:to-cyan-600 px-4 py-3 rounded-xl text-white font-medium flex items-center gap-2 transition-all"
                >
                  <Send className="w-5 h-5" />
                  <span className="hidden sm:inline">Telegram</span>
                </button>

                <button 
                  onClick={onLogout}
                  className="backdrop-blur-sm bg-red-500/20 hover:bg-red-500/30 p-3 rounded-xl border border-red-500/30 transition-all"
                >
                  <LogOut className="w-5 h-5 text-red-300" />
                </button>
              </div>
            </div>
          </div>
        </header>

        {/* Main Content */}
        <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {cryptosLoading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {[...Array(6)].map((_, i) => (
                <CryptoCardSkeleton key={i} />
              ))}
            </div>
          ) : (
            <>
              {/* Stats Cards */}
              <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
                {/* Status Card */}
                <div 
                  onClick={handleStartStopMonitoring}
                  className={`backdrop-blur-xl rounded-2xl p-6 border cursor-pointer transition-all hover:scale-[1.02] ${
                    isMonitoringActive
                      ? 'bg-emerald-500/20 border-emerald-500/50 shadow-lg shadow-emerald-500/20'
                      : 'bg-white/5 border-white/10 hover:bg-white/10'
                  }`}
                >
                  <div className="flex items-center justify-between mb-4">
                    <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${
                      isMonitoringActive ? 'bg-emerald-500/30' : 'bg-white/10'
                    }`}>
                      {isMonitoringActive ? (
                        <StopCircle className="w-6 h-6 text-emerald-400" />
                      ) : (
                        <PlayCircle className="w-6 h-6 text-white/60" />
                      )}
                    </div>
                  </div>
                  <p className="text-white/60 text-sm mb-1">Status</p>
                  <p className={`text-xl font-bold ${isMonitoringActive ? 'text-emerald-400' : 'text-white'}`}>
                    {isMonitoringActive ? '✓ Ativo' : '○ Inativo'}
                  </p>
                  <p className="text-white/40 text-xs mt-2">
                    {isMonitoringActive ? 'Clique para parar' : 'Clique para iniciar'}
                  </p>
                </div>

                {/* Selecionadas */}
                <div className="backdrop-blur-xl bg-white/5 rounded-2xl p-6 border border-white/10 hover:bg-white/10 transition-all">
                  <div className="flex items-center justify-between mb-4">
                    <div className="w-12 h-12 rounded-xl bg-blue-500/20 flex items-center justify-center">
                      <BarChart3 className="w-6 h-6 text-blue-400" />
                    </div>
                  </div>
                  <p className="text-white/60 text-sm mb-1">Selecionadas</p>
                  <p className="text-3xl font-bold text-white">{selectedCryptos.length}</p>
                </div>

                {/* Variação Média */}
                <div className="backdrop-blur-xl bg-white/5 rounded-2xl p-6 border border-white/10 hover:bg-white/10 transition-all">
                  <div className="flex items-center justify-between mb-4">
                    <div className="w-12 h-12 rounded-xl bg-purple-500/20 flex items-center justify-center">
                      <TrendingUp className="w-6 h-6 text-purple-400" />
                    </div>
                    <span className={`text-sm font-semibold flex items-center gap-1 ${
                      avgChange >= 0 ? 'text-emerald-400' : 'text-red-400'
                    }`}>
                      {avgChange >= 0 ? '▲' : '▼'} {Math.abs(avgChange).toFixed(2)}%
                    </span>
                  </div>
                  <p className="text-white/60 text-sm mb-1">Variação Média</p>
                  <p className="text-3xl font-bold text-white">{avgChange >= 0 ? '+' : ''}{avgChange.toFixed(2)}%</p>
                </div>

                {/* Alertas */}
                <div className="backdrop-blur-xl bg-white/5 rounded-2xl p-6 border border-white/10 hover:bg-white/10 transition-all">
                  <div className="flex items-center justify-between mb-4">
                    <div className="w-12 h-12 rounded-xl bg-orange-500/20 flex items-center justify-center">
                      <Bell className="w-6 h-6 text-orange-400" />
                    </div>
                  </div>
                  <p className="text-white/60 text-sm mb-1">Alertas Ativos</p>
                  <p className="text-3xl font-bold text-white">{isMonitoringActive ? selectedCryptos.length * 2 : 0}</p>
                </div>
              </div>

              {/* Settings Card */}
              {showSettings && (
                <div className="mb-8" style={{ animation: 'slideDown 0.3s ease-out' }}>
                  <div className="backdrop-blur-xl bg-white/5 rounded-2xl border border-white/10 p-6">
                    <div className="flex items-center justify-between mb-6">
                      <h2 className="text-2xl font-bold text-white flex items-center gap-3">
                        <Settings className="w-7 h-7 text-blue-400" />
                        Configurações de Monitoramento
                      </h2>
                      <button
                        onClick={() => setShowSettings(false)}
                        className="w-10 h-10 rounded-xl bg-white/5 hover:bg-white/10 flex items-center justify-center text-white/60 hover:text-white transition-all"
                      >
                        <X className="w-6 h-6" />
                      </button>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div>
                        <label className="flex items-center gap-2 text-sm font-semibold text-white/80 mb-2">
                          <Mail className="w-4 h-4 text-blue-400" />
                          Email para Alertas
                        </label>
                        <input
                          type="email"
                          value={monitoringEmail}
                          onChange={(e) => setMonitoringEmail(e.target.value)}
                          placeholder="seu@email.com"
                          className="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white placeholder-white/40 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        />
                      </div>

                      <div>
                        <label className="flex items-center gap-2 text-sm font-semibold text-white/80 mb-2">
                          <Clock className="w-4 h-4 text-purple-400" />
                          Intervalo de Verificação
                        </label>
                        <select
                          value={monitoringInterval}
                          onChange={(e) => setMonitoringInterval(parseInt(e.target.value))}
                          className="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                        >
                          <option value={1}>1 minuto</option>
                          <option value={5}>5 minutos ⭐ Recomendado</option>
                          <option value={10}>10 minutos</option>
                          <option value={15}>15 minutos</option>
                          <option value={30}>30 minutos</option>
                          <option value={60}>1 hora</option>
                        </select>
                      </div>

                      <div>
                        <label className="flex items-center gap-2 text-sm font-semibold text-white/80 mb-2">
                          <DollarSign className="w-4 h-4 text-red-400" />
                          Alerta de Compra (% queda)
                        </label>
                        <input
                          type="number"
                          value={buyThreshold}
                          onChange={(e) => setBuyThreshold(parseFloat(e.target.value) || 0)}
                          step="0.5"
                          min="0"
                          placeholder="5.0"
                          className="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white placeholder-white/40 focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent"
                        />
                      </div>

                      <div>
                        <label className="flex items-center gap-2 text-sm font-semibold text-white/80 mb-2">
                          <DollarSign className="w-4 h-4 text-emerald-400" />
                          Alerta de Venda (% alta)
                        </label>
                        <input
                          type="number"
                          value={sellThreshold}
                          onChange={(e) => setSellThreshold(parseFloat(e.target.value) || 0)}
                          step="0.5"
                          min="0"
                          placeholder="10.0"
                          className="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white placeholder-white/40 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                        />
                      </div>
                    </div>

                    <div className="mt-6 p-4 bg-blue-500/10 border border-blue-500/30 rounded-xl">
                      <p className="text-blue-200 text-sm">
                        💡 <strong>Dica:</strong> Configure seu email e selecione as criptomoedas antes de iniciar o monitoramento.
                        Você receberá alertas quando os preços caírem {buyThreshold}% ou subirem {sellThreshold}%.
                      </p>
                    </div>
                  </div>
                </div>
              )}

              {/* Charts */}
              {selectedCryptos.length > 0 && (
                <div className="mb-8">
                  <ChartTabs selectedCryptos={selectedCryptos} />
                </div>
              )}

              {/* Crypto List */}
              <div className="backdrop-blur-xl bg-white/5 rounded-2xl border border-white/10 overflow-hidden">
                <div className="p-6 border-b border-white/10">
                  <div className="flex items-center justify-between gap-4 flex-wrap">
                    <div>
                      <h2 className="text-2xl font-bold text-white mb-1">
                        Criptomoedas Disponíveis
                      </h2>
                      <p className="text-white/60 text-sm">
                        {filteredCryptos.length} moedas disponíveis
                        {selectedCryptos.length > 0 && ` • ${selectedCryptos.length} selecionadas`}
                      </p>
                    </div>

                    <div className="flex items-center gap-3">
                      <div className="relative">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-white/40" />
                        <input
                          type="text"
                          value={searchTerm}
                          onChange={(e) => setSearchTerm(e.target.value)}
                          placeholder="Buscar moeda..."
                          className="pl-10 pr-4 py-2 bg-white/5 border border-white/10 rounded-xl text-white placeholder-white/40 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent w-64"
                        />
                      </div>

                      {selectedCryptos.length > 0 && (
                        <button
                          onClick={onClearSelection}
                          className="px-4 py-2 bg-red-500/20 hover:bg-red-500/30 text-red-300 rounded-xl font-medium flex items-center gap-2 border border-red-500/30 transition-all"
                        >
                          <X className="w-4 h-4" />
                          Limpar ({selectedCryptos.length})
                        </button>
                      )}
                    </div>
                  </div>
                </div>

                <div className="p-6">
                  {filteredCryptos.length === 0 ? (
                    <div className="text-center py-12">
                      <p className="text-white/40">Nenhuma moeda encontrada para "{searchTerm}"</p>
                    </div>
                  ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                      {filteredCryptos.map((crypto) => {
                        const identifier = crypto.coinId || crypto.name || crypto.symbol;
                        const isSelected = selectedCryptos.some(c => {
                          const selectedId = c.coinId || c.name || c.symbol;
                          return selectedId === identifier;
                        });
                        const isPositive = (crypto.priceChange24h || 0) >= 0;
                        
                        return (
                          <div
                            key={identifier}
                            onClick={() => onToggleCryptoSelection(crypto)}
                            className={`backdrop-blur-sm p-5 rounded-xl border cursor-pointer transition-all transform hover:scale-[1.02] ${
                              isSelected 
                                ? 'bg-blue-500/20 border-blue-500/50 shadow-lg shadow-blue-500/20' 
                                : 'bg-white/5 border-white/10 hover:bg-white/10'
                            }`}
                          >
                            <div className="flex items-start justify-between mb-3">
                              <div className="flex-1">
                                <h3 className="text-lg font-bold text-white mb-1">{crypto.name}</h3>
                                <p className="text-white/60 text-sm font-medium">{crypto.symbol?.toUpperCase()}</p>
                              </div>
                              <div className={`w-8 h-8 rounded-lg flex items-center justify-center border-2 transition-all ${
                                isSelected 
                                  ? 'bg-blue-500 border-blue-500' 
                                  : 'bg-white/5 border-white/20'
                              }`}>
                                {isSelected && (
                                  <svg className="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                                  </svg>
                                )}
                              </div>
                            </div>

                            <p className="text-2xl font-bold text-white mb-3">
                              ${(crypto.currentPrice || 0).toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2})}
                            </p>

                            <div className="flex items-center justify-between">
                              <div className={`px-3 py-1 rounded-lg text-sm font-bold ${
                                isPositive 
                                  ? 'bg-emerald-500/20 text-emerald-400' 
                                  : 'bg-red-500/20 text-red-400'
                              }`}>
                                {isPositive ? '▲' : '▼'} {Math.abs(crypto.priceChange24h || 0).toFixed(2)}%
                              </div>
                              {crypto.marketCap && (
                                <p className="text-white/40 text-xs">
                                  ${((crypto.marketCap || 0) / 1e9).toFixed(1)}B
                                </p>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              </div>
            </>
          )}
        </main>
      </div>

      {/* Telegram Modal */}
      {showTelegramConfig && (
        <div 
          className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4"
          onClick={() => setShowTelegramConfig(false)}
        >
          <div 
            className="backdrop-blur-xl bg-white/10 rounded-3xl max-w-2xl w-full max-h-[90vh] overflow-y-auto border border-white/20"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="p-6 border-b border-white/10 flex items-center justify-between">
              <h2 className="text-2xl font-bold text-white">📱 Configuração do Telegram</h2>
              <button 
                onClick={() => setShowTelegramConfig(false)}
                className="w-10 h-10 rounded-xl bg-white/5 hover:bg-white/10 flex items-center justify-center text-white/60 hover:text-white transition-all"
              >
                <X className="w-6 h-6" />
              </button>
            </div>
            <div className="p-6">
              <TelegramConfig userEmail={monitoringEmail} />
            </div>
          </div>
        </div>
      )}

      <style>{`
        @keyframes gridMove {
          0% { transform: translate(0, 0); }
          100% { transform: translate(50px, 50px); }
        }
        
        @keyframes slideDown {
          from { opacity: 0; transform: translateY(-20px); }
          to { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </div>
  );
}

export default DashboardPage;