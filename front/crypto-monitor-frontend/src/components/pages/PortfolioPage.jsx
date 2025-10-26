// front/crypto-monitor-frontend/src/components/pages/PortfolioPage.jsx
// ✅ VERSÃO CORRIGIDA - Com Dark Mode e Theme Toggle

import React, { useState, useEffect } from 'react';
import { ArrowLeft, Plus, TrendingUp, DollarSign, Percent, Wallet } from 'lucide-react';
import { useTheme } from '../../contexts/ThemeContext';
import { API_BASE_URL } from '../../utils/constants';
import { formatCurrency } from '../../utils/formatters';
import ThemeToggle from '../common/ThemeToggle';
import PortfolioTable from '../portfolio/PortfolioTable';
import AddTransactionModal from '../portfolio/AddTransactionModal';
import TransactionHistory from '../portfolio/TransactionHistory';
import PortfolioChart from '../portfolio/PortfolioChart';
import '../../styles/portfolio.css';

function PortfolioPage({ token, user, onBack, availableCryptos }) {
  const { isDark } = useTheme();
  const [portfolio, setPortfolio] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [activeTab, setActiveTab] = useState('holdings');

  useEffect(() => {
    if (token) {
      fetchPortfolio();
      fetchTransactions();
    }
  }, [token]);

  const fetchPortfolio = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/portfolio`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (response.ok) {
        const data = await response.json();
        setPortfolio(data);
      }
    } catch (error) {
      console.error('Erro ao buscar portfolio:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchTransactions = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/transactions`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (response.ok) {
        const data = await response.json();
        setTransactions(data);
      }
    } catch (error) {
      console.error('Erro ao buscar transações:', error);
    }
  };

  const handleAddTransaction = async (transactionData) => {
    try {
      const response = await fetch(`${API_BASE_URL}/transactions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(transactionData)
      });

      if (response.ok) {
        await fetchPortfolio();
        await fetchTransactions();
        setShowAddModal(false);
      }
    } catch (error) {
      console.error('Erro ao adicionar transação:', error);
    }
  };

  // Cálculo do resumo do portfolio
  const summary = {
    totalInvested: portfolio.reduce((sum, item) => sum + parseFloat(item.totalInvested || 0), 0),
    totalCurrentValue: portfolio.reduce((sum, item) => sum + parseFloat(item.currentValue || 0), 0),
    get totalProfitLoss() {
      return this.totalCurrentValue - this.totalInvested;
    },
    get totalProfitLossPercent() {
      return this.totalInvested > 0 ? ((this.totalProfitLoss / this.totalInvested) * 100).toFixed(2) : 0;
    }
  };

  const isProfitable = summary.totalProfitLoss >= 0;

  return (
    <div className={`portfolio-page ${isDark ? 'dark' : ''}`}>
      {/* Header com Theme Toggle */}
      <div className="portfolio-header">
        <div className="portfolio-header-left">
          <button onClick={onBack} className="back-button">
            <ArrowLeft size={20} />
            Voltar
          </button>
          <div className="portfolio-title-section">
            <h1 className="portfolio-title">
              <Wallet size={32} />
              Meu Portfolio
            </h1>
            <p className="portfolio-subtitle">
              Gerencie seus investimentos em criptomoedas
            </p>
          </div>
        </div>
        <div className="portfolio-header-right">
          <ThemeToggle />
          <button
            onClick={() => setShowAddModal(true)}
            className="add-transaction-button"
          >
            <Plus size={20} />
            Nova Transação
          </button>
        </div>
      </div>

      {/* Cards de Resumo */}
      <div className="portfolio-summary-cards">
        <div className={`summary-card ${isDark ? 'dark' : ''}`}>
          <div className="summary-card-icon total">
            <DollarSign size={24} />
          </div>
          <div className="summary-card-content">
            <p className="summary-card-label">Total Investido</p>
            <p className="summary-card-value">{formatCurrency(summary.totalInvested)}</p>
          </div>
        </div>

        <div className={`summary-card ${isDark ? 'dark' : ''}`}>
          <div className="summary-card-icon current">
            <TrendingUp size={24} />
          </div>
          <div className="summary-card-content">
            <p className="summary-card-label">Valor Atual</p>
            <p className="summary-card-value">{formatCurrency(summary.totalCurrentValue)}</p>
          </div>
        </div>

        <div className={`summary-card ${isDark ? 'dark' : ''} ${isProfitable ? 'profit' : 'loss'}`}>
          <div className={`summary-card-icon ${isProfitable ? 'profit' : 'loss'}`}>
            <Percent size={24} />
          </div>
          <div className="summary-card-content">
            <p className="summary-card-label">Lucro/Prejuízo</p>
            <p className={`summary-card-value ${isProfitable ? 'profit' : 'loss'}`}>
              {isProfitable ? '+' : ''}{formatCurrency(summary.totalProfitLoss)}
            </p>
            <p className={`summary-card-percent ${isProfitable ? 'profit' : 'loss'}`}>
              {isProfitable ? '▲' : '▼'} {Math.abs(summary.totalProfitLossPercent)}%
            </p>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="portfolio-tabs">
        <button
          onClick={() => setActiveTab('holdings')}
          className={`portfolio-tab ${activeTab === 'holdings' ? 'active' : ''} ${isDark ? 'dark' : ''}`}
        >
          Holdings
        </button>
        <button
          onClick={() => setActiveTab('chart')}
          className={`portfolio-tab ${activeTab === 'chart' ? 'active' : ''} ${isDark ? 'dark' : ''}`}
        >
          Gráfico
        </button>
        <button
          onClick={() => setActiveTab('history')}
          className={`portfolio-tab ${activeTab === 'history' ? 'active' : ''} ${isDark ? 'dark' : ''}`}
        >
          Histórico
        </button>
      </div>

      {/* Conteúdo das Tabs */}
      <div className="portfolio-content">
        {loading ? (
          <div className="portfolio-loading">
            <div className="spinner"></div>
            <p>Carregando portfolio...</p>
          </div>
        ) : (
          <>
            {activeTab === 'holdings' && (
              <PortfolioTable
                portfolio={portfolio}
                onRefresh={fetchPortfolio}
              />
            )}

            {activeTab === 'chart' && (
              <PortfolioChart portfolio={portfolio} />
            )}

            {activeTab === 'history' && (
              <TransactionHistory
                transactions={transactions}
                onRefresh={fetchTransactions}
              />
            )}
          </>
        )}
      </div>

      {/* Modal de Adicionar Transação */}
      {showAddModal && (
        <AddTransactionModal
          onClose={() => setShowAddModal(false)}
          onSubmit={handleAddTransaction}
          availableCryptos={availableCryptos}
        />
      )}
    </div>
  );
}

export default PortfolioPage;