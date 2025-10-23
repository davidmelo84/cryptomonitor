// front/crypto-monitor-frontend/src/components/portfolio/TransactionHistory.jsx

import React from 'react';
import { TrendingUp, TrendingDown, Trash2 } from 'lucide-react';
import { API_BASE_URL } from '../../utils/constants';
import '../../styles/portfolio.css';

function TransactionHistory({ transactions, onRefresh, token }) {
  // ============================================
  // FORMATTERS
  // ============================================
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

  const formatQuantity = (value) => {
    return parseFloat(value).toFixed(8);
  };

  // ============================================
  // HANDLERS
  // ============================================
  const handleDelete = async (transactionId) => {
    if (!window.confirm(
      '⚠️ Tem certeza que deseja deletar esta transação?\n\n' +
      'Esta ação não pode ser desfeita.'
    )) {
      return;
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/portfolio/transaction/${transactionId}`,
        {
          method: 'DELETE',
          headers: { 'Authorization': `Bearer ${token}` }
        }
      );

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

  // ============================================
  // COMPUTED VALUES
  // ============================================
  const summary = {
    total: transactions.length,
    buys: transactions.filter(tx => tx.type === 'BUY').length,
    sells: transactions.filter(tx => tx.type === 'SELL').length
  };

  // ============================================
  // RENDER
  // ============================================
  return (
    <div className="transaction-history-container">
      
      {/* ============================================
          TABLE
          ============================================ */}
      <div className="overflow-x-auto">
        <table className="transaction-table">
          <thead>
            <tr>
              <th>Data</th>
              <th>Tipo</th>
              <th>Ativo</th>
              <th className="text-right">Quantidade</th>
              <th className="text-right">Preço Unitário</th>
              <th className="text-right">Valor Total</th>
              <th>Observações</th>
              <th className="text-center">Ações</th>
            </tr>
          </thead>
          <tbody>
            {transactions.length === 0 ? (
              <tr>
                <td colSpan="8" className="portfolio-empty">
                  <p className="portfolio-empty-title">
                    Nenhuma transação encontrada
                  </p>
                  <p className="portfolio-empty-subtitle">
                    Adicione sua primeira transação para começar
                  </p>
                </td>
              </tr>
            ) : (
              transactions.map((tx) => (
                <TransactionRow
                  key={tx.id}
                  transaction={tx}
                  onDelete={handleDelete}
                  formatters={{ formatCurrency, formatDate, formatQuantity }}
                />
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* ============================================
          SUMMARY
          ============================================ */}
      {transactions.length > 0 && (
        <div className="bg-gray-50 p-6 border-t-2 border-gray-200">
          <div className="grid grid-cols-3 gap-6 max-w-3xl mx-auto">
            <SummaryCard
              label="Total de Transações"
              value={summary.total}
              color="text-gray-800"
            />
            <SummaryCard
              label="Compras"
              value={summary.buys}
              color="text-green-600"
            />
            <SummaryCard
              label="Vendas"
              value={summary.sells}
              color="text-red-600"
            />
          </div>
        </div>
      )}
    </div>
  );
}

// ============================================
// TRANSACTION ROW COMPONENT
// ============================================
function TransactionRow({ transaction, onDelete, formatters }) {
  const { formatCurrency, formatDate, formatQuantity } = formatters;
  const isBuy = transaction.type === 'BUY';

  return (
    <tr>
      {/* Data */}
      <td className="font-mono text-gray-600">
        {formatDate(transaction.transactionDate)}
      </td>

      {/* Tipo */}
      <td>
        <span className={`transaction-type-badge ${isBuy ? 'buy' : 'sell'}`}>
          {isBuy ? <TrendingUp size={16} /> : <TrendingDown size={16} />}
          {isBuy ? 'Compra' : 'Venda'}
        </span>
      </td>

      {/* Ativo */}
      <td>
        <div>
          <p className="font-bold text-gray-800">{transaction.coinName}</p>
          <p className="text-sm text-gray-500">{transaction.coinSymbol}</p>
        </div>
      </td>

      {/* Quantidade */}
      <td className="text-right font-mono font-semibold">
        {formatQuantity(transaction.quantity)}
      </td>

      {/* Preço Unitário */}
      <td className="text-right font-semibold">
        {formatCurrency(transaction.pricePerUnit)}
      </td>

      {/* Valor Total */}
      <td className="text-right font-bold text-indigo-600">
        {formatCurrency(transaction.totalValue)}
      </td>

      {/* Observações */}
      <td className="text-sm text-gray-600 max-w-xs truncate">
        {transaction.notes || '-'}
      </td>

      {/* Ações */}
      <td className="text-center">
        <button
          onClick={() => onDelete(transaction.id)}
          className="transaction-delete-btn"
          title="Deletar transação"
        >
          <Trash2 size={18} />
        </button>
      </td>
    </tr>
  );
}

// ============================================
// SUMMARY CARD COMPONENT
// ============================================
function SummaryCard({ label, value, color }) {
  return (
    <div className="text-center">
      <p className="text-sm text-gray-600 font-semibold mb-1">{label}</p>
      <p className={`text-2xl font-bold ${color}`}>{value}</p>
    </div>
  );
}

export default TransactionHistory;