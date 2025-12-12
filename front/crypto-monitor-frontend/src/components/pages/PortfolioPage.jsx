// front/crypto-monitor-frontend/src/components/pages/PortfolioPage.jsx
// ✅ VERSÃO MODERNIZADA - Visual consistente com Dashboard

import React, { useState, useEffect, useCallback } from 'react';
import { 
  ArrowLeft, Plus, TrendingUp, TrendingDown, DollarSign, 
  Percent, Wallet, PieChart, History, X 
} from 'lucide-react';
import { API_BASE_URL } from '../../utils/constants';
import { formatCurrency } from '../../utils/formatters';
import PortfolioTable from '../portfolio/PortfolioTable';
import AddTransactionModal from '../portfolio/AddTransactionModal';
import TransactionHistory from '../portfolio/TransactionHistory';
import PortfolioChart from '../portfolio/PortfolioChart';

function PortfolioPage({ token, user, onBack }) {
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

  const fetchPortfolio = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/portfolio`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      
      if (response.ok) {
        const data = await response.json();
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

  const isProfitable = portfolioData.totalProfitLoss >= 0;

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
                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-purple-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-purple-500/50">
                  <Wallet className="w-7 h-7 text-white" />
                </div>
                <div>
                  <h1 className="text-2xl font-bold text-white">Meu Portfolio</h1>
                  <p className="text-sm text-purple-200">Gerencie seus investimentos</p>
                </div>
              </div>

              {/* Actions */}
              <button
                onClick={() => setShowAddModal(true)}
                className="backdrop-blur-sm bg-gradient-to-r from-emerald-500/80 to-teal-600/80 hover:from-emerald-500 hover:to-teal-600 px-6 py-3 rounded-xl text-white font-medium flex items-center gap-2 transition-all"
              >
                <Plus className="w-5 h-5" />
                Nova Transação
              </button>
            </div>
          </div>
        </header>

        {/* Main Content */}
        <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {loading ? (
            <div className="flex flex-col items-center justify-center py-20">
              <div className="w-16 h-16 border-4 border-purple-500/30 border-t-purple-500 rounded-full animate-spin mb-4" />
              <p className="text-white/60">Carregando portfolio...</p>
            </div>
          ) : (
            <>
              {/* Summary Cards */}
              <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
                {/* Total Investido */}
                <div className="backdrop-blur-xl bg-white/5 rounded-2xl p-6 border border-white/10 hover:bg-white/10 transition-all">
                  <div className="flex items-center justify-between mb-4">
                    <div className="w-12 h-12 rounded-xl bg-blue-500/20 flex items-center justify-center">
                      <DollarSign className="w-6 h-6 text-blue-400" />
                    </div>
                  </div>
                  <p className="text-white/60 text-sm mb-1">Total Investido</p>
                  <p className="text-3xl font-bold text-white">
                    {formatCurrency(portfolioData.totalInvested)}
                  </p>
                </div>

                {/* Valor Atual */}
                <div className="backdrop-blur-xl bg-white/5 rounded-2xl p-6 border border-white/10 hover:bg-white/10 transition-all">
                  <div className="flex items-center justify-between mb-4">
                    <div className="w-12 h-12 rounded-xl bg-purple-500/20 flex items-center justify-center">
                      <TrendingUp className="w-6 h-6 text-purple-400" />
                    </div>
                  </div>
                  <p className="text-white/60 text-sm mb-1">Valor Atual</p>
                  <p className="text-3xl font-bold text-white">
                    {formatCurrency(portfolioData.totalCurrentValue)}
                  </p>
                </div>

                {/* Lucro/Prejuízo */}
                <div className={`backdrop-blur-xl rounded-2xl p-6 border transition-all ${
                  isProfitable
                    ? 'bg-emerald-500/20 border-emerald-500/50 shadow-lg shadow-emerald-500/20'
                    : 'bg-red-500/20 border-red-500/50 shadow-lg shadow-red-500/20'
                }`}>
                  <div className="flex items-center justify-between mb-4">
                    <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${
                      isProfitable ? 'bg-emerald-500/30' : 'bg-red-500/30'
                    }`}>
                      {isProfitable ? (
                        <TrendingUp className="w-6 h-6 text-emerald-400" />
                      ) : (
                        <TrendingDown className="w-6 h-6 text-red-400" />
                      )}
                    </div>
                  </div>
                  <p className="text-white/80 text-sm mb-1">Lucro/Prejuízo</p>
                  <p className={`text-3xl font-bold ${isProfitable ? 'text-emerald-400' : 'text-red-400'}`}>
                    {isProfitable ? '+' : ''}{formatCurrency(portfolioData.totalProfitLoss)}
                  </p>
                </div>

                {/* Rentabilidade */}
                <div className={`backdrop-blur-xl rounded-2xl p-6 border transition-all ${
                  isProfitable
                    ? 'bg-emerald-500/20 border-emerald-500/50'
                    : 'bg-red-500/20 border-red-500/50'
                }`}>
                  <div className="flex items-center justify-between mb-4">
                    <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${
                      isProfitable ? 'bg-emerald-500/30' : 'bg-red-500/30'
                    }`}>
                      <Percent className="w-6 h-6 text-white" />
                    </div>
                    <span className={`text-sm font-semibold flex items-center gap-1 ${
                      isProfitable ? 'text-emerald-400' : 'text-red-400'
                    }`}>
                      {isProfitable ? '▲' : '▼'} {Math.abs(portfolioData.totalProfitLossPercent)}%
                    </span>
                  </div>
                  <p className="text-white/80 text-sm mb-1">Rentabilidade</p>
                  <p className={`text-3xl font-bold ${isProfitable ? 'text-emerald-400' : 'text-red-400'}`}>
                    {isProfitable ? '+' : ''}{portfolioData.totalProfitLossPercent.toFixed(2)}%
                  </p>
                </div>
              </div>

              {/* Tabs */}
              <div className="backdrop-blur-xl bg-white/5 rounded-t-2xl border-x border-t border-white/10">
                <div className="flex gap-2 p-2">
                  <button
                    onClick={() => setActiveTab('holdings')}
                    className={`flex items-center gap-2 px-6 py-3 rounded-xl font-semibold transition-all ${
                      activeTab === 'holdings'
                        ? 'bg-purple-500 text-white shadow-lg shadow-purple-500/50'
                        : 'text-white/60 hover:bg-white/5'
                    }`}
                  >
                    <Wallet className="w-5 h-5" />
                    Holdings
                  </button>
                  <button
                    onClick={() => setActiveTab('chart')}
                    className={`flex items-center gap-2 px-6 py-3 rounded-xl font-semibold transition-all ${
                      activeTab === 'chart'
                        ? 'bg-purple-500 text-white shadow-lg shadow-purple-500/50'
                        : 'text-white/60 hover:bg-white/5'
                    }`}
                  >
                    <PieChart className="w-5 h-5" />
                    Gráfico
                  </button>
                  <button
                    onClick={() => setActiveTab('history')}
                    className={`flex items-center gap-2 px-6 py-3 rounded-xl font-semibold transition-all ${
                      activeTab === 'history'
                        ? 'bg-purple-500 text-white shadow-lg shadow-purple-500/50'
                        : 'text-white/60 hover:bg-white/5'
                    }`}
                  >
                    <History className="w-5 h-5" />
                    Histórico
                  </button>
                </div>
              </div>

              {/* Tab Content */}
              <div className="backdrop-blur-xl bg-white/5 rounded-b-2xl border-x border-b border-white/10 p-6">
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
              </div>
            </>
          )}
        </main>
      </div>

      {/* Add Transaction Modal */}
      {showAddModal && (
        <div 
          className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4"
          onClick={() => setShowAddModal(false)}
        >
          <div 
            className="backdrop-blur-xl bg-white/10 rounded-3xl max-w-2xl w-full max-h-[90vh] overflow-y-auto border border-white/20"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="p-6 border-b border-white/10 flex items-center justify-between">
              <h2 className="text-2xl font-bold text-white">💰 Nova Transação</h2>
              <button 
                onClick={() => setShowAddModal(false)}
                className="w-10 h-10 rounded-xl bg-white/5 hover:bg-white/10 flex items-center justify-center text-white/60 hover:text-white transition-all"
              >
                <X className="w-6 h-6" />
              </button>
            </div>
            <div className="p-6">
              <AddTransactionModal
                isOpen={showAddModal}
                onClose={() => setShowAddModal(false)}
                onTransactionAdded={handleAddTransaction}
              />
            </div>
          </div>
        </div>
      )}

      <style>{`
        @keyframes gridMove {
          0% { transform: translate(0, 0); }
          100% { transform: translate(50px, 50px); }
        }
      `}</style>
    </div>
  );
}

export default PortfolioPage;