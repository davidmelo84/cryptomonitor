// front/crypto-monitor-frontend/src/components/portfolio/TransactionHistory.jsx
// ✅ REFATORADO - SEM CSS INLINE

import React, { useState, useEffect } from 'react';
import { 
  formatCurrency, 
  formatDate, 
  formatQuantity 
} from '../../utils/formatters';
import '../../styles/TransactionHistory.css';

function TransactionHistory() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filter, setFilter] = useState('all'); // all, buy, sell

  useEffect(() => {
    fetchTransactions();
  }, []);

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      const response = await fetch('http://localhost:8080/api/transactions', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });

      if (!response.ok) throw new Error('Failed to fetch transactions');

      const data = await response.json();
      setTransactions(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const filteredTransactions = transactions.filter(tx => {
    if (filter === 'all') return true;
    return tx.type.toLowerCase() === filter;
  });

  const deleteTransaction = async (id) => {
    if (!window.confirm('Deseja realmente excluir esta transação?')) return;

    try {
      const response = await fetch(`http://localhost:8080/api/transactions/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });

      if (!response.ok) throw new Error('Failed to delete transaction');

      setTransactions(transactions.filter(tx => tx.id !== id));
    } catch (err) {
      alert('Erro ao excluir transação: ' + err.message);
    }
  };

  // Loading State
  if (loading) {
    return (
      <div className="transaction-history">
        <div className="loading">
          Carregando histórico...
        </div>
      </div>
    );
  }

  // Error State
  if (error) {
    return (
      <div className="transaction-history">
        <div className="error">
          Erro: {error}
        </div>
      </div>
    );
  }

  return (
    <div className="transaction-history">
      
      {/* Header with Filters */}
      <div className="transaction-header">
        <h2>Histórico de Transações</h2>
        
        <div className="transaction-filters">
          <button 
            className={filter === 'all' ? 'active' : ''}
            onClick={() => setFilter('all')}
          >
            Todas
          </button>
          <button 
            className={filter === 'buy' ? 'active' : ''}
            onClick={() => setFilter('buy')}
          >
            Compras
          </button>
          <button 
            className={filter === 'sell' ? 'active' : ''}
            onClick={() => setFilter('sell')}
          >
            Vendas
          </button>
        </div>
      </div>

      {/* Empty State */}
      {filteredTransactions.length === 0 ? (
        <div className="no-transactions">
          <p>Nenhuma transação encontrada</p>
        </div>
      ) : (
        <>
          {/* Transactions Table */}
          <div className="transactions-table-container">
            <table className="transactions-table">
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Cripto</th>
                  <th>Tipo</th>
                  <th>Quantidade</th>
                  <th>Preço</th>
                  <th>Total</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {filteredTransactions.map((tx) => (
                  <tr key={tx.id}>
                    {/* Date */}
                    <td>{formatDate(tx.date, 'short')}</td>
                    
                    {/* Crypto Symbol */}
                    <td className="crypto-cell">
                      <span className="crypto-symbol">
                        {tx.cryptoSymbol}
                      </span>
                    </td>
                    
                    {/* Transaction Type */}
                    <td>
                      <span className={`transaction-type ${tx.type.toLowerCase()}`}>
                        {tx.type === 'BUY' ? 'Compra' : 'Venda'}
                      </span>
                    </td>
                    
                    {/* Quantity */}
                    <td>{formatQuantity(tx.quantity)}</td>
                    
                    {/* Price */}
                    <td>{formatCurrency(tx.price)}</td>
                    
                    {/* Total */}
                    <td className="total-cell">
                      {formatCurrency(tx.quantity * tx.price)}
                    </td>
                    
                    {/* Actions */}
                    <td>
                      <button 
                        className="delete-btn"
                        onClick={() => deleteTransaction(tx.id)}
                        title="Excluir transação"
                      >
                        🗑️
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Summary Footer */}
          <div className="transaction-summary">
            <p>
              Total de transações: <strong>{filteredTransactions.length}</strong>
            </p>
          </div>
        </>
      )}
    </div>
  );
}

export default TransactionHistory;