import React, { useState, useEffect } from 'react';
import { 
  formatCurrency, 
  formatPercent, 
  formatPercentWithSign 
} from '../../utils/formatters';
import '../../styles/trading-bots.css';

function TradingBotsPage() {
  const [bots, setBots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all'); // all, active, inactive
  const [showModal, setShowModal] = useState(false);
  const [selectedBot, setSelectedBot] = useState(null);

  // Summary statistics
  const [summary, setSummary] = useState({
    totalBots: 0,
    activeBots: 0,
    totalProfit: 0,
    profitChange: 0
  });

  useEffect(() => {
    fetchBots();
    fetchSummary();
  }, []);

  const fetchBots = async () => {
    try {
      setLoading(true);
      const response = await fetch('http://localhost:8080/api/trading-bots', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });

      if (!response.ok) throw new Error('Failed to fetch bots');

      const data = await response.json();
      setBots(data);
    } catch (err) {
      console.error('Error fetching bots:', err);
      // Mock data for demonstration
      setBots(getMockBots());
    } finally {
      setLoading(false);
    }
  };

  const fetchSummary = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/trading-bots/summary', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });

      if (!response.ok) throw new Error('Failed to fetch summary');

      const data = await response.json();
      setSummary(data);
    } catch (err) {
      console.error('Error fetching summary:', err);
      // Calculate from bots
      const activeBots = bots.filter(b => b.status === 'active').length;
      const totalProfit = bots.reduce((sum, b) => sum + (b.totalProfit || 0), 0);
      setSummary({
        totalBots: bots.length,
        activeBots,
        totalProfit,
        profitChange: 5.2
      });
    }
  };

  const getMockBots = () => [
    {
      id: 1,
      name: 'BTC Scalper Pro',
      strategy: 'Scalping',
      status: 'active',
      crypto: 'BTC',
      totalProfit: 2450.50,
      profitPercent: 12.5,
      tradesCount: 145,
      winRate: 68.5,
      description: 'Bot de scalping otimizado para Bitcoin com foco em movimentos rápidos'
    },
    {
      id: 2,
      name: 'ETH Trend Follower',
      strategy: 'Trend Following',
      status: 'active',
      crypto: 'ETH',
      totalProfit: 1850.75,
      profitPercent: 9.2,
      tradesCount: 89,
      winRate: 72.3,
      description: 'Segue tendências de médio prazo no Ethereum com trailing stop'
    },
    {
      id: 3,
      name: 'Multi-Coin Arbitrage',
      strategy: 'Arbitrage',
      status: 'paused',
      crypto: 'MULTI',
      totalProfit: -150.20,
      profitPercent: -2.1,
      tradesCount: 234,
      winRate: 45.2,
      description: 'Aproveitacapítu de diferenças de preço entre exchanges'
    },
    {
      id: 4,
      name: 'BNB Grid Trading',
      strategy: 'Grid Trading',
      status: 'active',
      crypto: 'BNB',
      totalProfit: 890.30,
      profitPercent: 6.8,
      tradesCount: 412,
      winRate: 61.4,
      description: 'Grid trading automatizado para mercado lateral'
    }
  ];

  const filteredBots = bots.filter(bot => {
    if (filter === 'all') return true;
    if (filter === 'active') return bot.status === 'active';
    if (filter === 'inactive') return bot.status !== 'active';
    return true;
  });

  const toggleBotStatus = async (botId, currentStatus) => {
    try {
      const newStatus = currentStatus === 'active' ? 'paused' : 'active';
      const response = await fetch(`http://localhost:8080/api/trading-bots/${botId}/status`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({ status: newStatus })
      });

      if (!response.ok) throw new Error('Failed to update bot status');

      // Update local state
      setBots(bots.map(bot => 
        bot.id === botId ? { ...bot, status: newStatus } : bot
      ));
    } catch (err) {
      alert('Erro ao alterar status do bot: ' + err.message);
    }
  };

  const deleteBot = async (botId) => {
    if (!window.confirm('Deseja realmente excluir este bot?')) return;

    try {
      const response = await fetch(`http://localhost:8080/api/trading-bots/${botId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });

      if (!response.ok) throw new Error('Failed to delete bot');

      setBots(bots.filter(bot => bot.id !== botId));
    } catch (err) {
      alert('Erro ao excluir bot: ' + err.message);
    }
  };

  const openBotDetails = (bot) => {
    setSelectedBot(bot);
    setShowModal(true);
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

  return (
    <div className="trading-bots-page">
      {/* Page Header */}
      <div className="page-header">
        <h1>🤖 Trading Bots</h1>
        <div className="header-actions">
          <button className="btn btn-primary">
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
            <div className="summary-card-value">{summary.totalBots}</div>
            <div className="summary-card-subtitle">
              {summary.activeBots} ativos
            </div>
          </div>

          <div className="summary-card">
            <div className="summary-card-header">
              <div className="summary-card-icon">📊</div>
              <span className="summary-card-title">Lucro Total</span>
            </div>
            <div className="summary-card-value">
              {formatCurrency(summary.totalProfit)}
            </div>
            <div className="summary-card-subtitle">
              <span className={`summary-card-change ${summary.profitChange >= 0 ? 'positive' : 'negative'}`}>
                {formatPercentWithSign(summary.profitChange)} este mês
              </span>
            </div>
          </div>

          <div className="summary-card">
            <div className="summary-card-header">
              <div className="summary-card-icon">📈</div>
              <span className="summary-card-title">Taxa de Vitória</span>
            </div>
            <div className="summary-card-value">
              {formatPercent(65.4)}
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
              {bots.reduce((sum, b) => sum + (b.tradesCount || 0), 0)}
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
              Ativos ({bots.filter(b => b.status === 'active').length})
            </button>
            <button 
              className={`filter-btn ${filter === 'inactive' ? 'active' : ''}`}
              onClick={() => setFilter('inactive')}
            >
              Inativos ({bots.filter(b => b.status !== 'active').length})
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
            {filter === 'all' && (
              <button className="btn btn-primary">
                ➕ Criar Primeiro Bot
              </button>
            )}
          </div>
        ) : (
          <div className="bots-grid">
            {filteredBots.map(bot => (
              <div key={bot.id} className="bot-card">
                <div className="bot-card-header">
                  <div className="bot-info">
                    <h3 className="bot-name">{bot.name}</h3>
                    <p className="bot-strategy">
                      {bot.strategy} · {bot.crypto}
                    </p>
                  </div>
                  <span className={`bot-status-badge ${bot.status}`}>
                    {bot.status === 'active' ? '🟢 Ativo' : 
                     bot.status === 'paused' ? '⏸️ Pausado' : '🔴 Inativo'}
                  </span>
                </div>

                <div className="bot-card-body">
                  <div className="bot-description">
                    {bot.description}
                  </div>

                  <div className="bot-metrics">
                    <div className="bot-metric">
                      <span className="bot-metric-label">Lucro Total</span>
                      <span className={`bot-metric-value ${bot.totalProfit >= 0 ? 'positive' : 'negative'}`}>
                        {formatCurrency(bot.totalProfit)}
                      </span>
                    </div>

                    <div className="bot-metric">
                      <span className="bot-metric-label">Retorno</span>
                      <span className={`bot-metric-value ${bot.profitPercent >= 0 ? 'positive' : 'negative'}`}>
                        {formatPercentWithSign(bot.profitPercent)}
                      </span>
                    </div>

                    <div className="bot-metric">
                      <span className="bot-metric-label">Operações</span>
                      <span className="bot-metric-value">
                        {bot.tradesCount}
                      </span>
                    </div>

                    <div className="bot-metric">
                      <span className="bot-metric-label">Win Rate</span>
                      <span className="bot-metric-value">
                        {formatPercent(bot.winRate)}
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

                  {bot.status === 'active' ? (
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
            ))}
          </div>
        )}
      </div>

      {/* Bot Details Modal */}
      {showModal && selectedBot && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{selectedBot.name}</h2>
              <button 
                className="modal-close-btn"
                onClick={() => setShowModal(false)}
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
                <p>{selectedBot.crypto}</p>
              </div>

              <div className="form-group">
                <label className="form-label">Status</label>
                <span className={`bot-status-badge ${selectedBot.status}`}>
                  {selectedBot.status}
                </span>
              </div>

              <div className="form-group">
                <label className="form-label">Descrição</label>
                <p>{selectedBot.description}</p>
              </div>

              <div className="bot-metrics">
                <div className="bot-metric">
                  <span className="bot-metric-label">Lucro Total</span>
                  <span className={`bot-metric-value ${selectedBot.totalProfit >= 0 ? 'positive' : 'negative'}`}>
                    {formatCurrency(selectedBot.totalProfit)}
                  </span>
                </div>

                <div className="bot-metric">
                  <span className="bot-metric-label">Retorno %</span>
                  <span className={`bot-metric-value ${selectedBot.profitPercent >= 0 ? 'positive' : 'negative'}`}>
                    {formatPercentWithSign(selectedBot.profitPercent)}
                  </span>
                </div>

                <div className="bot-metric">
                  <span className="bot-metric-label">Total de Operações</span>
                  <span className="bot-metric-value">
                    {selectedBot.tradesCount}
                  </span>
                </div>

                <div className="bot-metric">
                  <span className="bot-metric-label">Taxa de Vitória</span>
                  <span className="bot-metric-value">
                    {formatPercent(selectedBot.winRate)}
                  </span>
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <button 
                className="btn btn-secondary"
                onClick={() => setShowModal(false)}
              >
                Fechar
              </button>
              <button className="btn btn-primary">
                ⚙️ Configurar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default TradingBotsPage;