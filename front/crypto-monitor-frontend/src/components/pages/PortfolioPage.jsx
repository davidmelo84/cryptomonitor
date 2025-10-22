// front/crypto-monitor-frontend/src/components/pages/PortfolioPage.jsx

import React, { useState, useEffect, useCallback } from 'react';
import {
  Wallet,
  TrendingUp,
  TrendingDown,
  Plus,
  History,
  DollarSign,
  Percent,
  PieChart
} from 'lucide-react';
import { API_BASE_URL } from '../../utils/constants';
import AddTransactionModal from '../portfolio/AddTransactionModal';
import TransactionHistory from '../portfolio/TransactionHistory';
import PortfolioChart from '../portfolio/PortfolioChart';

function PortfolioPage({ token }) {
  const [portfolio, setPortfolio] = useState([]);
  const [summary, setSummary] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);
  const [activeTab, setActiveTab] = useState('portfolio'); // 'portfolio', 'transactions'

  // ✅ CORREÇÃO: Envolver em useCallback para evitar warning
  const fetchPortfolio = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/portfolio`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
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

  // ✅ CORREÇÃO: Envolver em useCallback para evitar warning
  const fetchTransactions = useCallback(async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/portfolio/transactions`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        setTransactions(data);
      }
    } catch (error) {
      console.error('Erro ao buscar transações:', error);
    }
  }, [token]);

  // ✅ CORREÇÃO: Agora pode incluir as dependências sem warning
  useEffect(() => {
    fetchPortfolio();
    fetchTransactions();
  }, [fetchPortfolio, fetchTransactions]);

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
        await fetchPortfolio();
        await fetchTransactions();
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

  const formatCurrency = (value) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(value);
  };

  const formatNumber = (value, decimals = 8) => {
    return parseFloat(value).toFixed(decimals);
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-500 mx-auto mb-4"></div>
          <p className="text-gray-600">Carregando portfolio...</p>
        </div>
      </div>
    );
  }

  const isPositive = summary?.totalProfitLoss >= 0;

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-gray-100 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-4xl font-bold text-gray-800 flex items-center gap-3">
              <Wallet size={40} color="#667eea" />
              Meu Portfolio
            </h1>
            <p className="text-gray-600 mt-2">
              Gerencie seus investimentos em criptomoedas
            </p>
          </div>

          <button
            onClick={() => setShowAddModal(true)}
            className="bg-gradient-to-r from-indigo-500 to-purple-600 text-white px-6 py-3 rounded-lg font-bold flex items-center gap-2 hover:scale-105 transition-transform shadow-lg"
          >
            <Plus size={20} />
            Nova Transação
          </button>
        </div>

        {/* Summary Cards */}
        {summary && (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
            <div className="bg-white p-6 rounded-xl shadow-md">
              <div className="flex items-center gap-3 mb-3">
                <DollarSign size={24} color="#667eea" />
                <span className="text-gray-600 font-semibold">Total Investido</span>
              </div>
              <p className="text-3xl font-bold text-gray-800">
                {formatCurrency(summary.totalInvested)}
              </p>
            </div>

            <div className="bg-white p-6 rounded-xl shadow-md">
              <div className="flex items-center gap-3 mb-3">
                <TrendingUp size={24} color="#667eea" />
                <span className="text-gray-600 font-semibold">Valor Atual</span>
              </div>
              <p className="text-3xl font-bold text-gray-800">
                {formatCurrency(summary.totalCurrentValue)}
              </p>
            </div>

            <div className="bg-white p-6 rounded-xl shadow-md">
              <div className="flex items-center gap-3 mb-3">
                {isPositive ? (
                  <TrendingUp size={24} color="#10b981" />
                ) : (
                  <TrendingDown size={24} color="#ef4444" />
                )}
                <span className="text-gray-600 font-semibold">Lucro/Prejuízo</span>
              </div>
              <p className={`text-3xl font-bold ${isPositive ? 'text-green-600' : 'text-red-600'}`}>
                {isPositive ? '+' : ''}{formatCurrency(summary.totalProfitLoss)}
              </p>
            </div>

            <div className="bg-white p-6 rounded-xl shadow-md">
              <div className="flex items-center gap-3 mb-3">
                <Percent size={24} color="#667eea" />
                <span className="text-gray-600 font-semibold">Rentabilidade</span>
              </div>
              <p className={`text-3xl font-bold ${isPositive ? 'text-green-600' : 'text-red-600'}`}>
                {isPositive ? '+' : ''}{formatNumber(summary.totalProfitLossPercent, 2)}%
              </p>
            </div>
          </div>
        )}

        {/* Tabs */}
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

        {/* Tab Content */}
        {activeTab === 'portfolio' ? (
          <>
            {/* Portfolio Chart */}
            {portfolio.length > 0 && (
              <PortfolioChart portfolio={portfolio} />
            )}

            {/* Portfolio Table */}
            <div className="bg-white rounded-xl shadow-md overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-gradient-to-r from-indigo-500 to-purple-600 text-white">
                    <tr>
                      <th className="px-6 py-4 text-left font-bold">Ativo</th>
                      <th className="px-6 py-4 text-right font-bold">Quantidade</th>
                      <th className="px-6 py-4 text-right font-bold">Preço Médio</th>
                      <th className="px-6 py-4 text-right font-bold">Preço Atual</th>
                      <th className="px-6 py-4 text-right font-bold">Total Investido</th>
                      <th className="px-6 py-4 text-right font-bold">Valor Atual</th>
                      <th className="px-6 py-4 text-right font-bold">Lucro/Prejuízo</th>
                      <th className="px-6 py-4 text-right font-bold">%</th>
                    </tr>
                  </thead>
                  <tbody>
                    {portfolio.length === 0 ? (
                      <tr>
                        <td colSpan="8" className="px-6 py-12 text-center text-gray-500">
                          <Wallet size={48} className="mx-auto mb-4 text-gray-300" />
                          <p className="text-lg font-semibold">Portfolio vazio</p>
                          <p className="text-sm mt-2">Adicione sua primeira transação para começar</p>
                        </td>
                      </tr>
                    ) : (
                      portfolio.map((item, index) => {
                        const isProfitable = item.profitLoss >= 0;
                        return (
                          <tr 
                            key={item.id} 
                            className={`border-b hover:bg-gray-50 transition-colors ${
                              index % 2 === 0 ? 'bg-white' : 'bg-gray-50'
                            }`}
                          >
                            <td className="px-6 py-4">
                              <div>
                                <p className="font-bold text-gray-800">{item.coinName}</p>
                                <p className="text-sm text-gray-500">{item.coinSymbol}</p>
                              </div>
                            </td>
                            <td className="px-6 py-4 text-right font-mono">
                              {formatNumber(item.quantity)}
                            </td>
                            <td className="px-6 py-4 text-right font-semibold">
                              {formatCurrency(item.averageBuyPrice)}
                            </td>
                            <td className="px-6 py-4 text-right font-semibold">
                              {formatCurrency(item.currentPrice)}
                            </td>
                            <td className="px-6 py-4 text-right font-semibold">
                              {formatCurrency(item.totalInvested)}
                            </td>
                            <td className="px-6 py-4 text-right font-bold text-indigo-600">
                              {formatCurrency(item.currentValue)}
                            </td>
                            <td className={`px-6 py-4 text-right font-bold ${
                              isProfitable ? 'text-green-600' : 'text-red-600'
                            }`}>
                              {isProfitable ? '+' : ''}{formatCurrency(item.profitLoss)}
                            </td>
                            <td className={`px-6 py-4 text-right font-bold ${
                              isProfitable ? 'text-green-600' : 'text-red-600'
                            }`}>
                              <div className="flex items-center justify-end gap-1">
                                {isProfitable ? '↗' : '↘'}
                                {Math.abs(item.profitLossPercent).toFixed(2)}%
                              </div>
                            </td>
                          </tr>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        ) : (
          <TransactionHistory 
            transactions={transactions} 
            onRefresh={() => {
              fetchPortfolio();
              fetchTransactions();
            }}
            token={token}
          />
        )}
      </div>

      {/* Add Transaction Modal */}
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