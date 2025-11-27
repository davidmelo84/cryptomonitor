// front/crypto-monitor-frontend/src/components/pages/TradingBotsPage.jsx
// ✅ VERSÃO FINAL COMPLETA - DARK MODE + CRIAÇÃO DE BOTS (AGORA SEM HARD-CODED URL)

import React, { useState, useEffect, useCallback } from 'react';
import { useTheme } from '../../contexts/ThemeContext';
import ThemeToggle from '../common/ThemeToggle';
import { formatCurrency, formatPercent, formatPercentWithSign } from '../../utils/formatters';
import CreateBotModal from '../bots/CreateBotModal';
import { API_BASE_URL } from '../../utils/constants';   // <<<<<< ADICIONADO
import '../../styles/trading-bots.css';

function TradingBotsPage({ token, onBack }) {
  const { isDark } = useTheme();

  const [bots, setBots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all');
  const [showCreateModal, setShowCreateModal] = useState(false);

  const fetchBots = useCallback(async () => {
    try {
      setLoading(true);
      const response = await fetch(`${API_BASE_URL}/bots`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (!response.ok) throw new Error('Failed to fetch bots');

      const data = await response.json();
      if (data.success && data.bots) setBots(data.bots);
      else setBots([]);
    } catch (err) {
      console.error('Erro ao buscar bots:', err);
      setBots([]);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchBots();
  }, [fetchBots]);

  const filteredBots = bots.filter(bot => {
    if (filter === 'all') return true;
    if (filter === 'active') return bot.status === 'RUNNING';
    if (filter === 'inactive') return bot.status !== 'RUNNING';
    return true;
  });

  const toggleBotStatus = async (botId, currentStatus) => {
    try {
      const endpoint = currentStatus === 'RUNNING' ? 'stop' : 'start';
      const response = await fetch(
        `${API_BASE_URL}/bots/${botId}/${endpoint}`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          }
        }
      );
      if (!response.ok) throw new Error('Failed to update bot status');
      const data = await response.json();
      if (data.success) {
        alert(data.message);
        fetchBots();
      }
    } catch (err) {
      alert('Erro ao alterar status do bot: ' + err.message);
    }
  };

  const deleteBot = async (botId) => {
    if (!window.confirm('Deseja realmente excluir este bot?')) return;
    try {
      const response = await fetch(
        `${API_BASE_URL}/bots/${botId}`,
        {
          method: 'DELETE',
          headers: { 'Authorization': `Bearer ${token}` }
        }
      );
      if (!response.ok) throw new Error('Failed to delete bot');
      const data = await response.json();
      if (data.success) {
        alert(data.message);
        fetchBots();
      }
    } catch (err) {
      alert('Erro ao excluir bot: ' + err.message);
    }
  };

  const handleBotCreated = () => {
    setShowCreateModal(false);
    fetchBots();
  };

  if (loading) {
    return (
      <div className={`trading-bots-page ${isDark ? 'dark-mode' : ''}`}>
        <div className="loading-state">
          <div className="loading-spinner"></div>
          <p className="loading-text">Carregando bots de trading...</p>
        </div>
      </div>
    );
  }

  const activeBots = bots.filter(b => b.status === 'RUNNING').length;
  const totalProfit = bots.reduce((sum, b) => sum + parseFloat(b.totalProfitLoss || 0), 0);
  const totalTrades = bots.reduce((sum, b) => sum + (b.totalTrades || 0), 0);
  const avgWinRate = bots.length > 0
    ? bots.reduce((sum, b) => {
        const wins = b.winningTrades || 0;
        const total = b.totalTrades || 0;
        return sum + (total > 0 ? (wins / total) * 100 : 0);
      }, 0) / bots.length
    : 0;

  return (
    <div className={`trading-bots-page ${isDark ? 'dark-mode' : ''}`}>
      {/* Page Header */}
      <div className="page-header">
        <h1>🤖 Trading Bots</h1>
        <div className="header-actions">
          <ThemeToggle />
          <button className="btn btn-secondary" onClick={onBack}>← Voltar</button>
          <button className="btn btn-primary" onClick={() => setShowCreateModal(true)}>
            ➕ Novo Bot
          </button>
        </div>
      </div>

      {/* Summary Section */}
      <div className="summary-section">
        <div className="summary-grid">
          <div className="summary-card">
            <div className="summary-card-header">
              <div className="summary-card-icon">🤖</div>
              <span className="summary-card-title">Total de Bots</span>
            </div>
            <div className="summary-card-value">{bots.length}</div>
            <div className="summary-card-subtitle">{activeBots} ativos</div>
          </div>

          <div className="summary-card">
            <div className="summary-card-header">
              <div className="summary-card-icon">📊</div>
              <span className="summary-card-title">Lucro Total</span>
            </div>
            <div className="summary-card-value">{formatCurrency(totalProfit)}</div>
            <div className="summary-card-subtitle">
              <span className={`summary-card-change ${totalProfit >= 0 ? 'positive' : 'negative'}`}>
                {formatPercentWithSign(5.2)} este mês
              </span>
            </div>
          </div>

          <div className="summary-card">
            <div className="summary-card-header">
              <div className="summary-card-icon">📈</div>
              <span className="summary-card-title">Taxa de Vitória</span>
            </div>
            <div className="summary-card-value">{formatPercent(avgWinRate)}</div>
            <div className="summary-card-subtitle">Média de todos os bots</div>
          </div>

          <div className="summary-card">
            <div className="summary-card-header">
              <div className="summary-card-icon">🔄</div>
              <span className="summary-card-title">Operações</span>
            </div>
            <div className="summary-card-value">{totalTrades}</div>
            <div className="summary-card-subtitle">Total executadas</div>
          </div>
        </div>
      </div>

      {/* Bots Section */}
      <div className="bots-section">
        <div className="bots-header">
          <h2>Meus Bots</h2>
          <div className="bots-filters">
            <button className={`filter-btn ${filter === 'all' ? 'active' : ''}`} onClick={() => setFilter('all')}>
              Todos ({bots.length})
            </button>
            <button className={`filter-btn ${filter === 'active' ? 'active' : ''}`} onClick={() => setFilter('active')}>
              Ativos ({activeBots})
            </button>
            <button className={`filter-btn ${filter === 'inactive' ? 'active' : ''}`} onClick={() => setFilter('inactive')}>
              Inativos ({bots.length - activeBots})
            </button>
          </div>
        </div>

        {/* Empty State */}
        {filteredBots.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">🤖</div>
            <h3 className="empty-state-title">
              {filter === 'all' ? 'Nenhum bot criado ainda' : `Nenhum bot ${filter === 'active' ? 'ativo' : 'inativo'}`}
            </h3>
            <p className="empty-state-description">
              {filter === 'all'
                ? 'Crie seu primeiro trading bot para automatizar suas operações'
                : 'Ajuste os filtros para ver outros bots'}
            </p>
            {filter === 'all' && (
              <button className="btn btn-primary" onClick={() => setShowCreateModal(true)}>
                ➕ Criar Primeiro Bot
              </button>
            )}
          </div>
        ) : (
          <div className="bots-grid">
            {filteredBots.map((bot) => (
              <BotCard
                key={bot.id}
                bot={bot}
                onToggleStatus={toggleBotStatus}
                onDelete={deleteBot}
                isDark={isDark}
              />
            ))}
          </div>
        )}
      </div>

      {/* Modal */}
      {showCreateModal && (
        <CreateBotModal
          token={token}
          onClose={() => setShowCreateModal(false)}
          onBotCreated={handleBotCreated}
        />
      )}
    </div>
  );
}

/* ============================================
   COMPONENTE: BOT CARD
============================================ */
function BotCard({ bot, onToggleStatus, onDelete, isDark }) {
  const isRunning = bot.status === 'RUNNING';
  const isProfitable = (bot.totalProfitLoss || 0) >= 0;

  const getStrategyLabel = (strategy) => {
    switch (strategy) {
      case 'GRID_TRADING': return 'Grid Trading';
      case 'DCA': return 'Dollar Cost Average';
      case 'STOP_LOSS': return 'Stop Loss / Take Profit';
      default: return strategy;
    }
  };

  const winRate = bot.totalTrades > 0
    ? ((bot.winningTrades || 0) / bot.totalTrades) * 100
    : 0;

  return (
    <div className={`bot-card ${isDark ? 'dark' : ''}`}>
      <div className="bot-card-header">
        <div className="bot-info">
          <h3 className="bot-name">{bot.name}</h3>
          <p className="bot-strategy">{getStrategyLabel(bot.strategy)}</p>
        </div>
        <span className={`bot-status-badge ${isRunning ? 'active' : 'inactive'}`}>
          {isRunning ? '🟢 Ativo' : '⚪ Inativo'}
        </span>
      </div>

      <div className="bot-card-body">
        <div className="bot-metrics">
          <div className="bot-metric">
            <span className="bot-metric-label">Moeda</span>
            <span className="bot-metric-value">{bot.coinSymbol}</span>
          </div>

          <div className="bot-metric">
            <span className="bot-metric-label">Lucro/Prejuízo</span>
            <span className={`bot-metric-value ${isProfitable ? 'positive' : 'negative'}`}>
              {formatCurrency(bot.totalProfitLoss || 0)}
            </span>
          </div>

          <div className="bot-metric">
            <span className="bot-metric-label">Operações</span>
            <span className="bot-metric-value">{bot.totalTrades || 0}</span>
          </div>

          <div className="bot-metric">
            <span className="bot-metric-label">Taxa de Vitória</span>
            <span className="bot-metric-value">{formatPercent(winRate)}</span>
          </div>
        </div>

        {bot.isSimulation && (
          <div className="bot-description">
            🎮 Modo Simulação - Este bot não executa operações reais
          </div>
        )}
      </div>

      <div className="bot-card-footer">
        <button
          className={`btn btn-sm ${isRunning ? 'btn-warning' : 'btn-success'}`}
          onClick={() => onToggleStatus(bot.id, bot.status)}
        >
          {isRunning ? '⏸️ Pausar' : '▶️ Iniciar'}
        </button>

        <button
          className="btn btn-sm btn-secondary"
          onClick={() => alert('Detalhes do bot (em desenvolvimento)')}
        >
          📊 Detalhes
        </button>

        <button
          className="btn btn-sm btn-danger"
          onClick={() => onDelete(bot.id)}
          disabled={isRunning}
        >
          🗑️ Excluir
        </button>
      </div>
    </div>
  );
}

export default TradingBotsPage;
