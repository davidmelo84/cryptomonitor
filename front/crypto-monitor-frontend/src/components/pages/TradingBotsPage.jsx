// front/crypto-monitor-frontend/src/components/pages/TradingBotsPage.jsx
// ✅ VERSÃO CORRIGIDA - MODAL DE CRIAÇÃO FUNCIONANDO

import React, { useState, useEffect } from 'react';
import { 
  formatCurrency, 
  formatPercent, 
  formatPercentWithSign 
} from '../../utils/formatters';
import CreateBotModal from '../bots/CreateBotModal'; // ✅ NOVO COMPONENTE
import '../../styles/trading-bots.css';

function TradingBotsPage({ token, onBack }) {
  const [bots, setBots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all');
  
  // ✅ ESTADOS PARA MODAIS
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedBot, setSelectedBot] = useState(null);

  useEffect(() => {
    fetchBots();
  }, []);

  const fetchBots = async () => {
    try {
      setLoading(true);
      const response = await fetch('http://localhost:8080/crypto-monitor/api/bots', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
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
  };

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
          headers: {
            'Authorization': `Bearer ${token}`
          }
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

  // ✅ NOVO: Handler para abrir modal de criação
  const openCreateModal = () => {
    setShowCreateModal(true);
  };

  // ✅ NOVO: Handler quando bot é criado com sucesso
  const handleBotCreated = () => {
    setShowCreateModal(false);
    fetchBots(); // Recarrega lista
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
          <button className="btn btn-secondary" onClick={onBack}>
            ← Voltar
          </button>
          {/* ✅ CORRIGIDO: Adicionado onClick */}
          <button className="btn btn-primary" onClick={openCreateModal}>
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
            <div className="summary-card-subtitle">
              {activeBots} ativos
            </div>
          </div>

          <div className="summary-card">
            <div className="summary-card-header">
              <div className="summary-card-icon">📊</div>
              <span className="summary-card-title">Lucro Total</span>
            </div>
            <div className="summary-card-value">
              {formatCurrency(totalProfit)}
            </div>
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
            <div className="summary-card-value">
              {formatPercent(avgWinRate)}
            </div>
            <div className="summary-card-subtitle">
              Média de todos os bots
            </div>
          </div>

          <div className="summary-card">
            <div className="summary-card-header">
              <div className="summary-card-icon">🔄</div>
              <span className="summary-card-title">Operações</span>
            </div>
            <div className="summary-card-value">
              {totalTrades}
            </div>
            <div className="summary-card-subtitle">
              Total executadas
            </div>
          </div>
        </div>
      </div>

      {/* Bots Section */}
      <div className="bots-section">
        <div className="bots-header">
          <h2>Seus Bots</h2>
          <div className="bots-filters">
            <button 
              className={`filter-btn ${filter === 'all' ? 'active' : ''}`}
              onClick={() => setFilter('all')}
            >
              Todos ({bots.length})
            </button>
            <button 
              className={`filter-btn ${filter === 'active' ? 'active' : ''}`}
              onClick={() => setFilter('active')}
            >
              Ativos ({activeBots})
            </button>
            <button 
              className={`filter-btn ${filter === 'inactive' ? 'active' : ''}`}
              onClick={() => setFilter('inactive')}
            >
              Inativos ({bots.length - activeBots})
            </button>
          </div>
        </div>

        {filteredBots.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">🤖</div>
            <h3 className="empty-state-title">Nenhum bot encontrado</h3>
            <p className="empty-state-description">
              {filter === 'all' 
                ? 'Crie seu primeiro bot de trading para começar a automatizar suas operações.'
                : `Nenhum bot ${filter === 'active' ? 'ativo' : 'inativo'} no momento.`
              }
            </p>
            {/* ✅ CORRIGIDO: Adicionado onClick */}
            {filter === 'all' && (
              <button className="btn btn-primary" onClick={openCreateModal}>
                ➕ Criar Primeiro Bot
              </button>
            )}
          </div>
        ) : (
          <div className="bots-grid">
            {filteredBots.map(bot => {
              const isRunning = bot.status === 'RUNNING';
              const profitLoss = parseFloat(bot.totalProfitLoss || 0);
              const winRate = bot.totalTrades > 0 
                ? (bot.winningTrades / bot.totalTrades) * 100 
                : 0;
              
              return (
                <div key={bot.id} className="bot-card">
                  <div className="bot-card-header">
                    <div className="bot-info">
                      <h3 className="bot-name">{bot.name}</h3>
                      <p className="bot-strategy">
                        {bot.strategy} · {bot.coinSymbol}
                      </p>
                    </div>
                    <span className={`bot-status-badge ${isRunning ? 'active' : 'inactive'}`}>
                      {isRunning ? '🟢 Ativo' : '🔴 Parado'}
                    </span>
                  </div>

                  <div className="bot-card-body">
                    <div className="bot-description">
                      Bot de trading automático - {bot.isSimulation ? 'Modo Simulação' : 'Modo Real'}
                    </div>

                    <div className="bot-metrics">
                      <div className="bot-metric">
                        <span className="bot-metric-label">Lucro Total</span>
                        <span className={`bot-metric-value ${profitLoss >= 0 ? 'positive' : 'negative'}`}>
                          {formatCurrency(profitLoss)}
                        </span>
                      </div>

                      <div className="bot-metric">
                        <span className="bot-metric-label">Operações</span>
                        <span className="bot-metric-value">
                          {bot.totalTrades || 0}
                        </span>
                      </div>

                      <div className="bot-metric">
                        <span className="bot-metric-label">Win Rate</span>
                        <span className="bot-metric-value">
                          {formatPercent(winRate)}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="bot-card-footer">
                    <button 
                      className="btn btn-secondary btn-sm"
                      onClick={() => openBotDetails(bot)}
                    >
                      📊 Detalhes
                    </button>

                    {isRunning ? (
                      <button 
                        className="btn btn-warning btn-sm"
                        onClick={() => toggleBotStatus(bot.id, bot.status)}
                      >
                        ⏸️ Pausar
                      </button>
                    ) : (
                      <button 
                        className="btn btn-success btn-sm"
                        onClick={() => toggleBotStatus(bot.id, bot.status)}
                      >
                        ▶️ Ativar
                      </button>
                    )}

                    <button 
                      className="btn btn-danger btn-sm"
                      onClick={() => deleteBot(bot.id)}
                    >
                      🗑️ Excluir
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* ✅ NOVO: Modal de Criação de Bot */}
      {showCreateModal && (
        <CreateBotModal
          token={token}
          onClose={() => setShowCreateModal(false)}
          onBotCreated={handleBotCreated}
        />
      )}

      {/* Modal de Detalhes (mantido) */}
      {showDetailsModal && selectedBot && (
        <div className="modal-overlay" onClick={() => setShowDetailsModal(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{selectedBot.name}</h2>
              <button 
                className="modal-close-btn"
                onClick={() => setShowDetailsModal(false)}
              >
                ✕
              </button>
            </div>

            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">Estratégia</label>
                <p>{selectedBot.strategy}</p>
              </div>

              <div className="form-group">
                <label className="form-label">Criptomoeda</label>
                <p>{selectedBot.coinSymbol}</p>
              </div>

              <div className="form-group">
                <label className="form-label">Status</label>
                <span className={`bot-status-badge ${selectedBot.status === 'RUNNING' ? 'active' : 'inactive'}`}>
                  {selectedBot.status}
                </span>
              </div>

              <div className="bot-metrics">
                <div className="bot-metric">
                  <span className="bot-metric-label">Lucro Total</span>
                  <span className={`bot-metric-value ${selectedBot.totalProfitLoss >= 0 ? 'positive' : 'negative'}`}>
                    {formatCurrency(selectedBot.totalProfitLoss || 0)}
                  </span>
                </div>

                <div className="bot-metric">
                  <span className="bot-metric-label">Total de Operações</span>
                  <span className="bot-metric-value">
                    {selectedBot.totalTrades || 0}
                  </span>
                </div>

                <div className="bot-metric">
                  <span className="bot-metric-label">Taxa de Vitória</span>
                  <span className="bot-metric-value">
                    {formatPercent(
                      selectedBot.totalTrades > 0 
                        ? (selectedBot.winningTrades / selectedBot.totalTrades) * 100 
                        : 0
                    )}
                  </span>
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <button 
                className="btn btn-secondary"
                onClick={() => setShowDetailsModal(false)}
              >
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default TradingBotsPage;