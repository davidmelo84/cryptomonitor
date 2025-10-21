// front/crypto-monitor-frontend/src/components/portfolio/TransactionHistory.jsx

import React from 'react';
import { TrendingUp, TrendingDown, Trash2 } from 'lucide-react';
import { API_BASE_URL } from '../../utils/constants';

function TransactionHistory({ transactions, onRefresh, token }) {
  const formatCurrency = (value) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(value);
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const handleDelete = async (transactionId) => {
    if (!window.confirm('⚠️ Tem certeza que deseja deletar esta transação?\n\nEsta ação não pode ser desfeita.')) {
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/portfolio/transaction/${transactionId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        alert('✅ Transação deletada com sucesso!');
        onRefresh();
      } else {
        const error = await response.json();
        alert(`❌ Erro: ${error.error || 'Falha ao deletar transação'}`);
      }
    } catch (error) {
      console.error('Erro ao deletar transação:', error);
      alert('❌ Erro ao deletar transação');
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-md overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-gradient-to-r from-indigo-500 to-purple-600 text-white">
            <tr>
              <th className="px-6 py-4 text-left font-bold">Data</th>
              <th className="px-6 py-4 text-left font-bold">Tipo</th>
              <th className="px-6 py-4 text-left font-bold">Ativo</th>
              <th className="px-6 py-4 text-right font-bold">Quantidade</th>
              <th className="px-6 py-4 text-right font-bold">Preço Unitário</th>
              <th className="px-6 py-4 text-right font-bold">Valor Total</th>
              <th className="px-6 py-4 text-left font-bold">Observações</th>
              <th className="px-6 py-4 text-center font-bold">Ações</th>
            </tr>
          </thead>
          <tbody>
            {transactions.length === 0 ? (
              <tr>
                <td colSpan="8" className="px-6 py-12 text-center text-gray-500">
                  <p className="text-lg font-semibold">Nenhuma transação encontrada</p>
                  <p className="text-sm mt-2">Adicione sua primeira transação para começar</p>
                </td>
              </tr>
            ) : (
              transactions.map((tx, index) => {
                const isBuy = tx.type === 'BUY';
                return (
                  <tr 
                    key={tx.id}
                    className={`border-b hover:bg-gray-50 transition-colors ${
                      index % 2 === 0 ? 'bg-white' : 'bg-gray-50'
                    }`}
                  >
                    <td className="px-6 py-4 text-sm text-gray-600 font-mono">
                      {formatDate(tx.transactionDate)}
                    </td>
                    <td className="px-6 py-4">
                      <span className={`inline-flex items-center gap-2 px-3 py-1 rounded-full font-bold text-sm ${
                        isBuy 
                          ? 'bg-green-100 text-green-700' 
                          : 'bg-red-100 text-red-700'
                      }`}>
                        {isBuy ? <TrendingUp size={16} /> : <TrendingDown size={16} />}
                        {isBuy ? 'Compra' : 'Venda'}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div>
                        <p className="font-bold text-gray-800">{tx.coinName}</p>
                        <p className="text-sm text-gray-500">{tx.coinSymbol}</p>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-right font-mono font-semibold">
                      {parseFloat(tx.quantity).toFixed(8)}
                    </td>
                    <td className="px-6 py-4 text-right font-semibold">
                      {formatCurrency(tx.pricePerUnit)}
                    </td>
                    <td className="px-6 py-4 text-right font-bold text-indigo-600">
                      {formatCurrency(tx.totalValue)}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600 max-w-xs truncate">
                      {tx.notes || '-'}
                    </td>
                    <td className="px-6 py-4 text-center">
                      <button
                        onClick={() => handleDelete(tx.id)}
                        className="text-red-600 hover:bg-red-50 p-2 rounded-lg transition-colors"
                        title="Deletar transação"
                      >
                        <Trash2 size={18} />
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Summary */}
      {transactions.length > 0 && (
        <div className="bg-gray-50 p-6 border-t-2 border-gray-200">
          <div className="grid grid-cols-3 gap-6 max-w-3xl mx-auto">
            <div className="text-center">
              <p className="text-sm text-gray-600 font-semibold mb-1">Total de Transações</p>
              <p className="text-2xl font-bold text-gray-800">{transactions.length}</p>
            </div>
            <div className="text-center">
              <p className="text-sm text-gray-600 font-semibold mb-1">Compras</p>
              <p className="text-2xl font-bold text-green-600">
                {transactions.filter(tx => tx.type === 'BUY').length}
              </p>
            </div>
            <div className="text-center">
              <p className="text-sm text-gray-600 font-semibold mb-1">Vendas</p>
              <p className="text-2xl font-bold text-red-600">
                {transactions.filter(tx => tx.type === 'SELL').length}
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default TransactionHistory;