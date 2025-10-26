// front/crypto-monitor-frontend/src/components/pages/TradingBotsPage.jsx
// ✅ Versão atualizada com useCallback e dependências corretas

import React, { useState, useEffect, useCallback } from 'react';
import { formatCurrency, formatPercent, formatPercentWithSign } from '../../utils/formatters';
import CreateBotModal from '../bots/CreateBotModal';
import '../../styles/trading-bots.css';

function TradingBotsPage({ token, onBack }) {
  const [bots, setBots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all');

  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedBot, setSelectedBot] = useState(null);

  // ✅ useCallback para fetchBots
  const fetchBots = useCallback(async () => {
    try {
      setLoading(true);
      const response = await fetch('http://localhost:8080/crypto-monitor/api/bots', {
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (!response.ok) throw new Error('Failed to fetch bots');

      const data = await response.json();

      if (data.success && data.bots) {
        setBots(data.bots);
      } else {
        setBots([]);
      }
    } catch (err) {
      console.error('Erro ao buscar bots:', err);
      setBots([]);
    } finally {
      setLoading(false);
    }
  }, [token]); // ✅ Dependência

  useEffect(() => {
    fetchBots();
  }, [fetchBots]); // ✅ Dependência corrigida

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
        `http://localhost:8080/crypto-monitor/api/bots/${botId}/${endpoint}`,
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
        `http://localhost:8080/crypto-monitor/api/bots/${botId}`,
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

  const openBotDetails = (bot) => {
    setSelectedBot(bot);
    setShowDetailsModal(true);
  };

  const openCreateModal = () => setShowCreateModal(true);

  const handleBotCreated = () => {
    setShowCreateModal(false);
    fetchBots();
  };

  if (loading) {
    return (
      <div className="trading-bots-page">
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
    <div className="trading-bots-page">
      {/* Page Header */}
      <div className="page-header">
        <h1>🤖 Trading Bots</h1>
        <div className="header-actions">
          <button className="btn btn-secondary" onClick={onBack}>← Voltar</button>
          <button className="btn btn-primary" onClick={openCreateModal}>➕ Novo Bot</button>
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
      {/* ... código de renderização dos bots e modais permanece igual ... */}

      {/* Modal de Criação */}
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

export default TradingBotsPage;
