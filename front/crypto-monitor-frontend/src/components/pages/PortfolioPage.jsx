// front/crypto-monitor-frontend/src/components/pages/PortfolioPage.jsx
// ✅ VERSÃO COMPLETA E AUTOCONTIDA - Todos componentes inclusos

import React, { useState, useEffect, useMemo } from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts';
import { useTheme } from '../../contexts/ThemeContext';
import { API_BASE_URL } from '../../utils/constants';

const COLORS = ['#6366f1', '#8b5cf6', '#ec4899', '#f59e0b', '#10b981', '#3b82f6', '#ef4444'];

const PortfolioPage = ({ 
  user, 
  token, 
  onLogout, 
  onBack,
  availableCryptos = []
}) => {
  const { theme } = useTheme();
  
  // Estados principais
  const [transactions, setTransactions] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  // Estados do formulário
  const [selectedCrypto, setSelectedCrypto] = useState('');
  const [transactionType, setTransactionType] = useState('buy');
  const [amount, setAmount] = useState('');
  const [price, setPrice] = useState('');
  const [transactionDate, setTransactionDate] = useState(
    new Date().toISOString().split('T')[0]
  );

  // ✅ Buscar transações
  useEffect(() => {
    if (token) {
      fetchTransactions();
    }
  }, [token]);

  const fetchTransactions = async () => {
    setIsLoading(true);
    
    try {
      const response = await fetch(`${API_BASE_URL}/portfolio/transactions`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (response.ok) {
        const data = await response.json();
        setTransactions(data);
      } else {
        throw new Error('API falhou');
      }
    } catch (err) {
      console.error('Erro ao buscar transações:', err);
      
      // Fallback localStorage
      const saved = localStorage.getItem('portfolio_transactions');
      if (saved) {
        setTransactions(JSON.parse(saved));
      }
    } finally {
      setIsLoading(false);
    }
  };

  // ✅ Calcular dados do portfólio
  const portfolioData = useMemo(() => {
    if (!transactions.length || !availableCryptos.length) {
      return {
        holdings: [],
        totalValue: 0,
        totalInvested: 0,
        profitLoss: 0,
        profitLossPercentage: 0
      };
    }

    const holdingsMap = new Map();

    transactions.forEach(tx => {
      const crypto = tx.cryptocurrency || tx.coinId;
      if (!crypto) return;

      if (!holdingsMap.has(crypto)) {
        holdingsMap.set(crypto, {
          cryptocurrency: crypto,
          totalAmount: 0,
          totalInvested: 0
        });
      }

      const holding = holdingsMap.get(crypto);
      const txAmount = parseFloat(tx.amount || 0);
      const txPrice = parseFloat(tx.price || 0);

      if (tx.type === 'buy') {
        holding.totalAmount += txAmount;
        holding.totalInvested += txAmount * txPrice;
      } else if (tx.type === 'sell') {
        holding.totalAmount -= txAmount;
        holding.totalInvested -= txAmount * txPrice;
      }
    });

    const holdings = Array.from(holdingsMap.values())
      .filter(h => h.totalAmount > 0)
      .map(holding => {
        const currentCrypto = availableCryptos.find(
          c => (c.coinId || c.symbol?.toLowerCase()) === holding.cryptocurrency.toLowerCase()
        );

        const currentPrice = currentCrypto?.currentPrice || 0;
        const currentValue = holding.totalAmount * currentPrice;
        const profitLoss = currentValue - holding.totalInvested;
        const profitLossPercentage = holding.totalInvested > 0
          ? (profitLoss / holding.totalInvested) * 100
          : 0;

        return {
          ...holding,
          currentPrice,
          currentValue,
          profitLoss,
          profitLossPercentage,
          name: currentCrypto?.name || holding.cryptocurrency.toUpperCase(),
          symbol: currentCrypto?.symbol || holding.cryptocurrency.toUpperCase()
        };
      });

    const totalValue = holdings.reduce((sum, h) => sum + h.currentValue, 0);
    const totalInvested = holdings.reduce((sum, h) => sum + h.totalInvested, 0);
    const profitLoss = totalValue - totalInvested;
    const profitLossPercentage = totalInvested > 0
      ? (profitLoss / totalInvested) * 100
      : 0;

    return {
      holdings,
      totalValue,
      totalInvested,
      profitLoss,
      profitLossPercentage
    };
  }, [transactions, availableCryptos]);

  // ✅ Adicionar transação
  const handleAddTransaction = async (e) => {
    e.preventDefault();

    if (!selectedCrypto || !amount || !price) {
      alert('Preencha todos os campos obrigatórios');
      return;
    }

    const newTransaction = {
      cryptocurrency: selectedCrypto,
      type: transactionType,
      amount: parseFloat(amount),
      price: parseFloat(price),
      date: transactionDate,
      timestamp: new Date().toISOString()
    };

    try {
      const response = await fetch(`${API_BASE_URL}/portfolio/transactions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(newTransaction)
      });

      if (response.ok) {
        const savedTransaction = await response.json();
        setTransactions(prev => [savedTransaction, ...prev]);
      } else {
        throw new Error('API falhou');
      }
    } catch (err) {
      console.error('Erro:', err);
      
      const localTransaction = { ...newTransaction, id: Date.now() };
      const updatedTransactions = [localTransaction, ...transactions];
      setTransactions(updatedTransactions);
      localStorage.setItem('portfolio_transactions', JSON.stringify(updatedTransactions));
    }

    setSelectedCrypto('');
    setAmount('');
    setPrice('');
    setTransactionType('buy');
  };

  // ✅ Deletar transação
  const handleDeleteTransaction = async (transactionId) => {
    if (!window.confirm('Deletar esta transação?')) return;

    try {
      await fetch(`${API_BASE_URL}/portfolio/transactions/${transactionId}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      });

      setTransactions(prev => prev.filter(t => t.id !== transactionId));
    } catch (err) {
      const updatedTransactions = transactions.filter(t => t.id !== transactionId);
      setTransactions(updatedTransactions);
      localStorage.setItem('portfolio_transactions', JSON.stringify(updatedTransactions));
    }
  };

  // ✅ Dados para o gráfico
  const chartData = portfolioData.holdings.map(h => ({
    name: h.symbol,
    value: h.currentValue
  }));

  // ✅ RENDER
  const cardClass = theme === 'dark' 
    ? 'bg-gray-800 border-gray-700' 
    : 'bg-white border-gray-200';
  
  const textClass = theme === 'dark' ? 'text-white' : 'text-gray-900';
  const mutedClass = theme === 'dark' ? 'text-gray-400' : 'text-gray-600';

  return (
    <div className={`min-h-screen ${theme === 'dark' ? 'bg-gray-900' : 'bg-gray-50'}`}>
      {/* Header */}
      <header className={`${cardClass} border-b shadow-sm`}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex justify-between items-center">
            <h1 className={`text-2xl font-bold ${textClass}`}>
              💼 Meu Portfólio
            </h1>
            <div className="flex items-center gap-4">
              <span className={mutedClass}>Olá, {user?.username}</span>
              <button
                onClick={onLogout}
                className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg transition"
              >
                Sair
              </button>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Botão Voltar */}
        <button
          onClick={onBack}
          className={`mb-6 flex items-center gap-2 px-4 py-2 rounded-lg transition ${
            theme === 'dark'
              ? 'bg-gray-800 hover:bg-gray-700'
              : 'bg-white hover:bg-gray-50'
          } ${textClass}`}
        >
          ← Voltar ao Dashboard
        </button>

        {isLoading ? (
          <div className="text-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-500 mx-auto" />
            <p className={`mt-4 ${mutedClass}`}>Carregando...</p>
          </div>
        ) : (
          <div className="space-y-6">
            {/* Overview Cards */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className={`p-6 rounded-lg border ${cardClass}`}>
                <p className={mutedClass}>Valor Total</p>
                <p className={`text-2xl font-bold ${textClass}`}>
                  ${portfolioData.totalValue.toFixed(2)}
                </p>
              </div>
              <div className={`p-6 rounded-lg border ${cardClass}`}>
                <p className={mutedClass}>Investido</p>
                <p className={`text-2xl font-bold ${textClass}`}>
                  ${portfolioData.totalInvested.toFixed(2)}
                </p>
              </div>
              <div className={`p-6 rounded-lg border ${cardClass}`}>
                <p className={mutedClass}>Lucro/Prejuízo</p>
                <p className={`text-2xl font-bold ${
                  portfolioData.profitLoss >= 0 ? 'text-green-500' : 'text-red-500'
                }`}>
                  ${portfolioData.profitLoss.toFixed(2)}
                </p>
              </div>
              <div className={`p-6 rounded-lg border ${cardClass}`}>
                <p className={mutedClass}>Retorno</p>
                <p className={`text-2xl font-bold ${
                  portfolioData.profitLossPercentage >= 0 ? 'text-green-500' : 'text-red-500'
                }`}>
                  {portfolioData.profitLossPercentage.toFixed(2)}%
                </p>
              </div>
            </div>

            {/* Chart */}
            {portfolioData.holdings.length > 0 && (
              <div className={`p-6 rounded-lg border ${cardClass}`}>
                <h2 className={`text-xl font-bold mb-4 ${textClass}`}>
                  Distribuição do Portfólio
                </h2>
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie
                      data={chartData}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                      outerRadius={100}
                      fill="#8884d8"
                      dataKey="value"
                    >
                      {chartData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip formatter={(value) => `$${value.toFixed(2)}`} />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            )}

            {/* Form */}
            <div className={`p-6 rounded-lg border ${cardClass}`}>
              <h2 className={`text-xl font-bold mb-4 ${textClass}`}>
                Nova Transação
              </h2>
              <form onSubmit={handleAddTransaction} className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className={`block text-sm font-medium mb-2 ${textClass}`}>
                      Criptomoeda
                    </label>
                    <select
                      value={selectedCrypto}
                      onChange={(e) => setSelectedCrypto(e.target.value)}
                      className={`w-full px-4 py-2 rounded-lg border ${
                        theme === 'dark'
                          ? 'bg-gray-700 border-gray-600 text-white'
                          : 'bg-white border-gray-300 text-gray-900'
                      }`}
                      required
                    >
                      <option value="">Selecione...</option>
                      {availableCryptos.map(crypto => (
                        <option key={crypto.coinId} value={crypto.coinId}>
                          {crypto.name} ({crypto.symbol})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className={`block text-sm font-medium mb-2 ${textClass}`}>
                      Tipo
                    </label>
                    <select
                      value={transactionType}
                      onChange={(e) => setTransactionType(e.target.value)}
                      className={`w-full px-4 py-2 rounded-lg border ${
                        theme === 'dark'
                          ? 'bg-gray-700 border-gray-600 text-white'
                          : 'bg-white border-gray-300 text-gray-900'
                      }`}
                    >
                      <option value="buy">Compra</option>
                      <option value="sell">Venda</option>
                    </select>
                  </div>

                  <div>
                    <label className={`block text-sm font-medium mb-2 ${textClass}`}>
                      Quantidade
                    </label>
                    <input
                      type="number"
                      step="0.00000001"
                      value={amount}
                      onChange={(e) => setAmount(e.target.value)}
                      className={`w-full px-4 py-2 rounded-lg border ${
                        theme === 'dark'
                          ? 'bg-gray-700 border-gray-600 text-white'
                          : 'bg-white border-gray-300 text-gray-900'
                      }`}
                      required
                    />
                  </div>

                  <div>
                    <label className={`block text-sm font-medium mb-2 ${textClass}`}>
                      Preço (USD)
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      value={price}
                      onChange={(e) => setPrice(e.target.value)}
                      className={`w-full px-4 py-2 rounded-lg border ${
                        theme === 'dark'
                          ? 'bg-gray-700 border-gray-600 text-white'
                          : 'bg-white border-gray-300 text-gray-900'
                      }`}
                      required
                    />
                  </div>

                  <div>
                    <label className={`block text-sm font-medium mb-2 ${textClass}`}>
                      Data
                    </label>
                    <input
                      type="date"
                      value={transactionDate}
                      onChange={(e) => setTransactionDate(e.target.value)}
                      className={`w-full px-4 py-2 rounded-lg border ${
                        theme === 'dark'
                          ? 'bg-gray-700 border-gray-600 text-white'
                          : 'bg-white border-gray-300 text-gray-900'
                      }`}
                      required
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  className="w-full px-6 py-3 bg-indigo-500 hover:bg-indigo-600 text-white rounded-lg transition font-medium"
                >
                  Adicionar Transação
                </button>
              </form>
            </div>

            {/* Transaction History */}
            <div className={`p-6 rounded-lg border ${cardClass}`}>
              <h2 className={`text-xl font-bold mb-4 ${textClass}`}>
                Histórico de Transações
              </h2>
              
              {transactions.length === 0 ? (
                <p className={mutedClass}>Nenhuma transação registrada</p>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr className={`border-b ${theme === 'dark' ? 'border-gray-700' : 'border-gray-200'}`}>
                        <th className={`text-left py-3 px-4 ${textClass}`}>Data</th>
                        <th className={`text-left py-3 px-4 ${textClass}`}>Crypto</th>
                        <th className={`text-left py-3 px-4 ${textClass}`}>Tipo</th>
                        <th className={`text-right py-3 px-4 ${textClass}`}>Qtd</th>
                        <th className={`text-right py-3 px-4 ${textClass}`}>Preço</th>
                        <th className={`text-right py-3 px-4 ${textClass}`}>Total</th>
                        <th className={`text-center py-3 px-4 ${textClass}`}>Ações</th>
                      </tr>
                    </thead>
                    <tbody>
                      {transactions.map((tx) => (
                        <tr 
                          key={tx.id} 
                          className={`border-b ${theme === 'dark' ? 'border-gray-700' : 'border-gray-100'}`}
                        >
                          <td className={`py-3 px-4 ${mutedClass}`}>
                            {new Date(tx.date).toLocaleDateString()}
                          </td>
                          <td className={`py-3 px-4 ${textClass}`}>
                            {tx.cryptocurrency?.toUpperCase()}
                          </td>
                          <td className="py-3 px-4">
                            <span className={`px-2 py-1 rounded text-xs font-medium ${
                              tx.type === 'buy'
                                ? 'bg-green-100 text-green-800'
                                : 'bg-red-100 text-red-800'
                            }`}>
                              {tx.type === 'buy' ? 'Compra' : 'Venda'}
                            </span>
                          </td>
                          <td className={`py-3 px-4 text-right ${textClass}`}>
                            {tx.amount}
                          </td>
                          <td className={`py-3 px-4 text-right ${textClass}`}>
                            ${tx.price?.toFixed(2)}
                          </td>
                          <td className={`py-3 px-4 text-right font-medium ${textClass}`}>
                            ${(tx.amount * tx.price).toFixed(2)}
                          </td>
                          <td className="py-3 px-4 text-center">
                            <button
                              onClick={() => handleDeleteTransaction(tx.id)}
                              className="text-red-500 hover:text-red-700 transition"
                            >
                              🗑️
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        )}
      </main>
    </div>
  );
};

export default PortfolioPage;