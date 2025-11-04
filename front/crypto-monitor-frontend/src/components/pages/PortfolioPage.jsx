// front/crypto-monitor-frontend/src/components/pages/PortfolioPage.jsx
// ✅ VERSÃO CORRIGIDA - Backend retorna objeto, não array

import React, { useState, useEffect, useCallback } from 'react';
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
  
  // ✅ CORREÇÃO: Backend retorna objeto { portfolio: [], totalInvested, ... }
  const [portfolioData, setPortfolioData] = useState({
    portfolio: [],
    totalInvested: 0,
    totalCurrentValue: 0,
    totalProfitLoss: 0,
    totalProfitLossPercent: 0
  });
  
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [activeTab, setActiveTab] = useState('holdings');

  // ✅ useCallback para fetchPortfolio
  const fetchPortfolio = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/portfolio`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      
      if (response.ok) {
        const data = await response.json();
        console.log('📦 Portfolio recebido:', data);
        
        // ✅ CORREÇÃO: Validar estrutura
        if (data && typeof data === 'object') {
          setPortfolioData({
            portfolio: Array.isArray(data.portfolio) ? data.portfolio : [],
            totalInvested: data.totalInvested || 0,
            totalCurrentValue: data.totalCurrentValue || 0,
            totalProfitLoss: data.totalProfitLoss || 0,
            totalProfitLossPercent: data.totalProfitLossPercent || 0
          });
        }
      }
    } catch (error) {
      console.error('❌ Erro ao buscar portfolio:', error);
    } finally {
      setLoading(false);
    }
  }, [token]);

  // ✅ useCallback para fetchTransactions
  const fetchTransactions = useCallback(async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/transactions`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (response.ok) {
        const data = await response.json();
        setTransactions(Array.isArray(data) ? data : []);
      }
    } catch (error) {
      console.error('Erro ao buscar transações:', error);
    }
  }, [token]);

  useEffect(() => {
    if (token) {
      fetchPortfolio();
      fetchTransactions();
    }
  }, [token, fetchPortfolio, fetchTransactions]);

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

  // ✅ CORREÇÃO: Usar portfolioData ao invés de portfolio
  const isProfitable = portfolioData.totalProfitLoss >= 0;

  return (
    <div className={`portfolio-page ${isDark ? 'dark' : ''}`}>
      {/* Header */}
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
          <button onClick={() => setShowAddModal(true)} className="add-transaction-button">
            <Plus size={20} />
            Nova Transação
          </button>
        </div>
      </div>

      {/* Resumo */}
      <div className="portfolio-summary-cards">
        <div className={`summary-card ${isDark ? 'dark' : ''}`}>
          <div className="summary-card-icon total">
            <DollarSign size={24} />
          </div>
          <div className="summary-card-content">
            <p className="summary-card-label">Total Investido</p>
            <p className="summary-card-value">
              {formatCurrency(portfolioData.totalInvested)}
            </p>
          </div>
        </div>

        <div className={`summary-card ${isDark ? 'dark' : ''}`}>
          <div className="summary-card-icon current">
            <TrendingUp size={24} />
          </div>
          <div className="summary-card-content">
            <p className="summary-card-label">Valor Atual</p>
            <p className="summary-card-value">
              {formatCurrency(portfolioData.totalCurrentValue)}
            </p>
          </div>
        </div>

        <div className={`summary-card ${isDark ? 'dark' : ''} ${isProfitable ? 'profit' : 'loss'}`}>
          <div className={`summary-card-icon ${isProfitable ? 'profit' : 'loss'}`}>
            <Percent size={24} />
          </div>
          <div className="summary-card-content">
            <p className="summary-card-label">Lucro/Prejuízo</p>
            <p className={`summary-card-value ${isProfitable ? 'profit' : 'loss'}`}>
              {isProfitable ? '+' : ''}{formatCurrency(portfolioData.totalProfitLoss)}
            </p>
            <p className={`summary-card-percent ${isProfitable ? 'profit' : 'loss'}`}>
              {isProfitable ? '▲' : '▼'} {Math.abs(portfolioData.totalProfitLossPercent)}%
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
                portfolio={portfolioData.portfolio} 
                onRefresh={fetchPortfolio} 
              />
            )}
            {activeTab === 'chart' && (
              <PortfolioChart portfolio={portfolioData.portfolio} />
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

      {/* Modal */}
      {showAddModal && (
        <AddTransactionModal
          isOpen={showAddModal}
          onClose={() => setShowAddModal(false)}
          onTransactionAdded={handleAddTransaction}
        />
      )}
    </div>
  );
}

export default PortfolioPage;