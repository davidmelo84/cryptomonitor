// front/crypto-monitor-frontend/src/components/pages/PortfolioPage.jsx

import React, { useState, useEffect, useCallback } from 'react';
import { Wallet, Plus, PieChart, History } from 'lucide-react';
import { API_BASE_URL } from '../../utils/constants';
import AddTransactionModal from '../portfolio/AddTransactionModal';
import TransactionHistory from '../portfolio/TransactionHistory';
import PortfolioChart from '../portfolio/PortfolioChart';
import PortfolioSummary from '../portfolio/PortfolioSummary';
import PortfolioTable from '../portfolio/PortfolioTable';

function PortfolioPage({ token, onBack }) {
  // ============================================
  // STATE MANAGEMENT
  // ============================================
  const [portfolio, setPortfolio] = useState([]);
  const [summary, setSummary] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [activeTab, setActiveTab] = useState('portfolio');

  // ============================================
  // DATA FETCHING
  // ============================================
  const fetchPortfolio = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/portfolio`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (response.ok) {
        const data = await response.json();
        setPortfolio(data.portfolio || []);
        setSummary({
          totalInvested: data.totalInvested || 0,
          totalCurrentValue: data.totalCurrentValue || 0,
          totalProfitLoss: data.totalProfitLoss || 0,
          totalProfitLossPercent: data.totalProfitLossPercent || 0
        });
      }
    } catch (error) {
      console.error('Erro ao buscar portfolio:', error);
    } finally {
      setLoading(false);
    }
  }, [token]);

  const fetchTransactions = useCallback(async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/portfolio/transactions`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (response.ok) {
        const data = await response.json();
        setTransactions(data);
      }
    } catch (error) {
      console.error('Erro ao buscar transações:', error);
    }
  }, [token]);

  useEffect(() => {
    fetchPortfolio();
    fetchTransactions();
  }, [fetchPortfolio, fetchTransactions]);

  // ============================================
  // TRANSACTION HANDLERS
  // ============================================
  const handleAddTransaction = async (transaction) => {
    try {
      const response = await fetch(`${API_BASE_URL}/portfolio/transaction`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(transaction)
      });

      if (response.ok) {
        await Promise.all([fetchPortfolio(), fetchTransactions()]);
        setShowAddModal(false);
        alert('✅ Transação adicionada com sucesso!');
      } else {
        const error = await response.json();
        alert(`❌ Erro: ${error.error || 'Falha ao adicionar transação'}`);
      }
    } catch (error) {
      console.error('Erro ao adicionar transação:', error);
      alert('❌ Erro ao adicionar transação');
    }
  };

  const handleRefresh = () => {
    fetchPortfolio();
    fetchTransactions();
  };

  // ============================================
  // LOADING STATE
  // ============================================
  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-500 mx-auto mb-4" />
          <p className="text-gray-600">Carregando portfolio...</p>
        </div>
      </div>
    );
  }

  // ============================================
  // RENDER
  // ============================================
  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-gray-100 p-6">
      <div className="max-w-7xl mx-auto">
        
        {/* ============================================
            HEADER
            ============================================ */}
        <header className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-4xl font-bold text-gray-800 flex items-center gap-3">
              <Wallet size={40} color="#667eea" />
              Meu Portfolio
            </h1>
            <p className="text-gray-600 mt-2">
              Gerencie seus investimentos em criptomoedas
            </p>
          </div>

          <div className="flex gap-3">
            <button
              onClick={onBack}
              className="bg-gray-500 text-white px-6 py-3 rounded-lg font-bold hover:scale-105 transition-transform shadow-lg"
            >
              ← Voltar
            </button>
            
            <button
              onClick={() => setShowAddModal(true)}
              className="bg-gradient-to-r from-indigo-500 to-purple-600 text-white px-6 py-3 rounded-lg font-bold flex items-center gap-2 hover:scale-105 transition-transform shadow-lg"
            >
              <Plus size={20} />
              Nova Transação
            </button>
          </div>
        </header>

        {/* ============================================
            SUMMARY CARDS
            ============================================ */}
        {summary && <PortfolioSummary summary={summary} />}

        {/* ============================================
            TABS
            ============================================ */}
        <div className="flex gap-4 mb-6 border-b-2 border-gray-200">
          <button
            onClick={() => setActiveTab('portfolio')}
            className={`px-6 py-3 font-bold transition-all ${
              activeTab === 'portfolio'
                ? 'text-indigo-600 border-b-4 border-indigo-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            <div className="flex items-center gap-2">
              <PieChart size={20} />
              Portfolio
            </div>
          </button>

          <button
            onClick={() => setActiveTab('transactions')}
            className={`px-6 py-3 font-bold transition-all ${
              activeTab === 'transactions'
                ? 'text-indigo-600 border-b-4 border-indigo-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            <div className="flex items-center gap-2">
              <History size={20} />
              Histórico
            </div>
          </button>
        </div>

        {/* ============================================
            TAB CONTENT
            ============================================ */}
        {activeTab === 'portfolio' ? (
          <>
            {portfolio.length > 0 && <PortfolioChart portfolio={portfolio} />}
            <PortfolioTable portfolio={portfolio} />
          </>
        ) : (
          <TransactionHistory 
            transactions={transactions}
            onRefresh={handleRefresh}
            token={token}
          />
        )}
      </div>

      {/* ============================================
          ADD TRANSACTION MODAL
          ============================================ */}
      {showAddModal && (
        <AddTransactionModal
          onClose={() => setShowAddModal(false)}
          onSubmit={handleAddTransaction}
        />
      )}
    </div>
  );
}

export default PortfolioPage;