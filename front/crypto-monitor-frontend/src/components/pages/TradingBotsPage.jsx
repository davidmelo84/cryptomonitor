import React, { useState, useEffect, useCallback } from 'react';
import { 
  ArrowLeft, Plus, Bot, TrendingUp, TrendingDown, 
  Play, Pause, Trash2, Settings, DollarSign, Activity,
  BarChart3, Percent, Award
} from 'lucide-react';

// Mock de dados para demonstração
const mockBots = [
  {
    id: 1,
    name: 'Grid Bot BTC',
    coinSymbol: 'BTC',
    strategy: 'GRID_TRADING',
    status: 'RUNNING',
    totalProfitLoss: 1250.50,
    totalTrades: 45,
    winningTrades: 32,
    isSimulation: false
  },
  {
    id: 2,
    name: 'DCA ETH Strategy',
    coinSymbol: 'ETH',
    strategy: 'DCA',
    status: 'PAUSED',
    totalProfitLoss: -150.25,
    totalTrades: 12,
    winningTrades: 5,
    isSimulation: true
  },
  {
    id: 3,
    name: 'Stop Loss ADA',
    coinSymbol: 'ADA',
    strategy: 'STOP_LOSS',
    status: 'RUNNING',
    totalProfitLoss: 520.80,
    totalTrades: 28,
    winningTrades: 20,
    isSimulation: false
  }
];

function TradingBotsPage({ token, onBack }) {
  const [bots, setBots] = useState(mockBots);
  const [loading, setLoading] = useState(false);
  const [filter, setFilter] = useState('all');
  const [showCreateModal, setShowCreateModal] = useState(false);

  const filteredBots = bots.filter(bot => {
    if (filter === 'all') return true;
    if (filter === 'active') return bot.status === 'RUNNING';
    if (filter === 'inactive') return bot.status !== 'RUNNING';
    return true;
  });

  const activeBots = bots.filter(b => b.status === 'RUNNING').length;
  const totalProfit = bots.reduce((sum, b) => sum + b.totalProfitLoss, 0);
  const totalTrades = bots.reduce((sum, b) => sum + b.totalTrades, 0);
  const avgWinRate = bots.length > 0
    ? bots.reduce((sum, b) => {
        const wins = b.winningTrades || 0;
        const total = b.totalTrades || 0;
        return sum + (total > 0 ? (wins / total) * 100 : 0);
      }, 0) / bots.length
    : 0;

  const getStrategyLabel = (strategy) => {
    switch (strategy) {
      case 'GRID_TRADING': return 'Grid Trading';
      case 'DCA': return 'Dollar Cost Average';
      case 'STOP_LOSS': return 'Stop Loss / Take Profit';
      default: return strategy;
    }
  };

  const formatCurrency = (value) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2
    }).format(value);
  };

  const formatPercent = (value) => {
    return value.toFixed(2) + '%';
  };

  return (
    <div className="min-h-screen relative overflow-hidden">
      {/* Background */}
      <div 
        className="absolute inset-0 bg-cover bg-center bg-no-repeat"
        style={{
          backgroundImage: "url('https://images.unsplash.com/photo-1639762681485-074b7f938ba0?q=80&w=2832')",
        }}
      >
        <div className="absolute inset-0 bg-gradient-to-br from-slate-900/95 via-purple-900/90 to-indigo-900/95" />
      </div>

      {/* Animated Grid */}
      <div className="absolute inset-0 opacity-10">
        <div 
          className="w-full h-full"
          style={{
            backgroundImage: `
              linear-gradient(rgba(139, 92, 246, 0.3) 1px, transparent 1px),
              linear-gradient(90deg, rgba(139, 92, 246, 0.3) 1px, transparent 1px)
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
              {/* Logo & Back */}
              <div className="flex items-center gap-4">
                <button
                  onClick={onBack}
                  className="w-12 h-12 rounded-xl bg-white/5 hover:bg-white/10 flex items-center justify-center border border-white/10 transition-all"
                >
                  <ArrowLeft className="w-6 h-6 text-white" />
                </button>
                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-purple-500 to-pink-600 flex items-center justify-center shadow-lg shadow-purple-500/50">
                  <Bot className="w-7 h-7 text-white" />
                </div>
                <div>
                  <h1 className="text-2xl font-bold text-white">Trading Bots</h1>
                  <p className="text-sm text-purple-200">Automatize suas operações</p>
                </div>
              </div>

              {/* Actions */}
              <button
                onClick={() => setShowCreateModal(true)}
                className="backdrop-blur-sm bg-gradient-to-r from-emerald-500/80 to-teal-600/80 hover:from-emerald-500 hover:to-teal-600 px-6 py-3 rounded-xl text-white font-medium flex items-center gap-2 transition-all shadow-lg shadow-emerald-500/20"
              >
                <Plus className="w-5 h-5" />
                Novo Bot
              </button>
            </div>
          </div>
        </header>

        {/* Main Content */}
        <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {loading ? (
            <div className="flex flex-col items-center justify-center py-20">
              <div className="w-16 h-16 border-4 border-purple-500/30 border-t-purple-500 rounded-full animate-spin mb-4" />
              <p className="text-white/60">Carregando bots...</p>
            </div>
          ) : (
            <>
              {/* Summary Cards */}
              <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
                {/* Total Bots */}
                <div className="backdrop-blur-xl bg-white/5 rounded-2xl p-6 border border-white/10 hover:bg-white/10 transition-all">
                  <div className="flex items-center justify-between mb-4">
                    <div className="w-12 h-12 rounded-xl bg-blue-500/20 flex items-center justify-center">
                      <Bot className="w-6 h-6 text-blue-400" />
                    </div>
                  </div>
                  <p className="text-white/60 text-sm mb-1">Total de Bots</p>
                  <p className="text-3xl font-bold text-white">{bots.length}</p>
                  <p className="text-emerald-400 text-sm mt-2">{activeBots} ativos</p>
                </div>

                {/* Lucro Total */}
                <div className="backdrop-blur-xl bg-white/5 rounded-2xl p-6 border border-white/10 hover:bg-white/10 transition-all">
                  <div className="flex items-center justify-between mb-4">
                    <div className="w-12 h-12 rounded-xl bg-emerald-500/20 flex items-center justify-center">
                      <DollarSign className="w-6 h-6 text-emerald-400" />
                    </div>
                    <span className={`text-sm font-semibold flex items-center gap-1 ${
                      totalProfit >= 0 ? 'text-emerald-400' : 'text-red-400'
                    }`}>
                      {totalProfit >= 0 ? '▲' : '▼'} {Math.abs(totalProfit / 100).toFixed(1)}%
                    </span>
                  </div>
                  <p className="text-white/60 text-sm mb-1">Lucro Total</p>
                  <p className="text-3xl font-bold text-white">{formatCurrency(totalProfit)}</p>
                </div>

                {/* Taxa de Vitória */}
                <div className="backdrop-blur-xl bg-white/5 rounded-2xl p-6 border border-white/10 hover:bg-white/10 transition-all">
                  <div className="flex items-center justify-between mb-4">
                    <div className="w-12 h-12 rounded-xl bg-purple-500/20 flex items-center justify-center">
                      <Award className="w-6 h-6 text-purple-400" />
                    </div>
                  </div>
                  <p className="text-white/60 text-sm mb-1">Taxa de Vitória</p>
                  <p className="text-3xl font-bold text-white">{formatPercent(avgWinRate)}</p>
                  <p className="text-white/40 text-sm mt-2">Média geral</p>
                </div>

                {/* Operações */}
                <div className="backdrop-blur-xl bg-white/5 rounded-2xl p-6 border border-white/10 hover:bg-white/10 transition-all">
                  <div className="flex items-center justify-between mb-4">
                    <div className="w-12 h-12 rounded-xl bg-orange-500/20 flex items-center justify-center">
                      <Activity className="w-6 h-6 text-orange-400" />
                    </div>
                  </div>
                  <p className="text-white/60 text-sm mb-1">Operações</p>
                  <p className="text-3xl font-bold text-white">{totalTrades}</p>
                  <p className="text-white/40 text-sm mt-2">Total executadas</p>
                </div>
              </div>

              {/* Filters */}
              <div className="backdrop-blur-xl bg-white/5 rounded-t-2xl border-x border-t border-white/10">
                <div className="flex items-center justify-between p-4">
                  <h2 className="text-xl font-bold text-white">Meus Bots</h2>
                  <div className="flex gap-2">
                    <button
                      onClick={() => setFilter('all')}
                      className={`px-4 py-2 rounded-xl font-semibold text-sm transition-all ${
                        filter === 'all'
                          ? 'bg-purple-500 text-white shadow-lg shadow-purple-500/50'
                          : 'bg-white/5 text-white/60 hover:bg-white/10'
                      }`}
                    >
                      Todos ({bots.length})
                    </button>
                    <button
                      onClick={() => setFilter('active')}
                      className={`px-4 py-2 rounded-xl font-semibold text-sm transition-all ${
                        filter === 'active'
                          ? 'bg-emerald-500 text-white shadow-lg shadow-emerald-500/50'
                          : 'bg-white/5 text-white/60 hover:bg-white/10'
                      }`}
                    >
                      Ativos ({activeBots})
                    </button>
                    <button
                      onClick={() => setFilter('inactive')}
                      className={`px-4 py-2 rounded-xl font-semibold text-sm transition-all ${
                        filter === 'inactive'
                          ? 'bg-red-500 text-white shadow-lg shadow-red-500/50'
                          : 'bg-white/5 text-white/60 hover:bg-white/10'
                      }`}
                    >
                      Inativos ({bots.length - activeBots})
                    </button>
                  </div>
                </div>
              </div>

              {/* Bots Grid */}
              <div className="backdrop-blur-xl bg-white/5 rounded-b-2xl border-x border-b border-white/10 p-6">
                {filteredBots.length === 0 ? (
                  <div className="text-center py-20">
                    <Bot className="w-16 h-16 text-white/20 mx-auto mb-4" />
                    <h3 className="text-xl font-bold text-white mb-2">
                      {filter === 'all' ? 'Nenhum bot criado' : `Nenhum bot ${filter === 'active' ? 'ativo' : 'inativo'}`}
                    </h3>
                    <p className="text-white/60 mb-6">
                      {filter === 'all' 
                        ? 'Crie seu primeiro bot de trading para automatizar suas operações'
                        : 'Ajuste os filtros para ver outros bots'
                      }
                    </p>
                    {filter === 'all' && (
                      <button
                        onClick={() => setShowCreateModal(true)}
                        className="px-6 py-3 bg-gradient-to-r from-emerald-500 to-teal-600 text-white rounded-xl font-semibold hover:shadow-lg hover:shadow-emerald-500/50 transition-all"
                      >
                        <Plus className="w-5 h-5 inline mr-2" />
                        Criar Primeiro Bot
                      </button>
                    )}
                  </div>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {filteredBots.map((bot) => (
                      <BotCard key={bot.id} bot={bot} />
                    ))}
                  </div>
                )}
              </div>
            </>
          )}
        </main>
      </div>

      <style>{`
        @keyframes gridMove {
          0% { transform: translate(0, 0); }
          100% { transform: translate(50px, 50px); }
        }
      `}</style>
    </div>
  );
}

function BotCard({ bot }) {
  const isRunning = bot.status === 'RUNNING';
  const isProfitable = bot.totalProfitLoss >= 0;
  const winRate = bot.totalTrades > 0 ? (bot.winningTrades / bot.totalTrades) * 100 : 0;

  const formatCurrency = (value) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2
    }).format(value);
  };

  const getStrategyLabel = (strategy) => {
    switch (strategy) {
      case 'GRID_TRADING': return 'Grid Trading';
      case 'DCA': return 'DCA';
      case 'STOP_LOSS': return 'Stop Loss';
      default: return strategy;
    }
  };

  return (
    <div className="backdrop-blur-sm bg-white/5 rounded-xl border border-white/10 p-6 hover:bg-white/10 transition-all">
      {/* Header */}
      <div className="flex items-start justify-between mb-4">
        <div className="flex-1">
          <h3 className="text-lg font-bold text-white mb-1">{bot.name}</h3>
          <p className="text-sm text-white/60">{getStrategyLabel(bot.strategy)}</p>
        </div>
        <span className={`px-3 py-1 rounded-lg text-xs font-bold ${
          isRunning 
            ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' 
            : 'bg-red-500/20 text-red-400 border border-red-500/30'
        }`}>
          {isRunning ? '🟢 Ativo' : '⚪ Parado'}
        </span>
      </div>

      {/* Metrics */}
      <div className="space-y-3 mb-4">
        <div className="flex items-center justify-between">
          <span className="text-sm text-white/60">Moeda</span>
          <span className="text-sm font-bold text-white">{bot.coinSymbol}</span>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-sm text-white/60">Lucro/Prejuízo</span>
          <span className={`text-sm font-bold ${isProfitable ? 'text-emerald-400' : 'text-red-400'}`}>
            {formatCurrency(bot.totalProfitLoss)}
          </span>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-sm text-white/60">Operações</span>
          <span className="text-sm font-bold text-white">{bot.totalTrades}</span>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-sm text-white/60">Taxa Vitória</span>
          <span className="text-sm font-bold text-purple-400">{winRate.toFixed(1)}%</span>
        </div>
      </div>

      {bot.isSimulation && (
        <div className="mb-4 p-3 bg-blue-500/10 border border-blue-500/30 rounded-lg">
          <p className="text-xs text-blue-300">🎮 Modo Simulação</p>
        </div>
      )}

      {/* Actions */}
      <div className="flex gap-2">
        <button className={`flex-1 px-4 py-2 rounded-lg font-semibold text-sm transition-all ${
          isRunning
            ? 'bg-red-500/20 hover:bg-red-500/30 text-red-300 border border-red-500/30'
            : 'bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 border border-emerald-500/30'
        }`}>
          {isRunning ? <Pause className="w-4 h-4 inline mr-1" /> : <Play className="w-4 h-4 inline mr-1" />}
          {isRunning ? 'Pausar' : 'Iniciar'}
        </button>
        <button className="px-4 py-2 rounded-lg bg-white/5 hover:bg-white/10 border border-white/10 transition-all">
          <Settings className="w-4 h-4 text-white" />
        </button>
        <button className="px-4 py-2 rounded-lg bg-red-500/20 hover:bg-red-500/30 border border-red-500/30 transition-all">
          <Trash2 className="w-4 h-4 text-red-300" />
        </button>
      </div>
    </div>
  );
}

export default TradingBotsPage;