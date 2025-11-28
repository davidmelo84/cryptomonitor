// front/crypto-monitor-frontend/src/components/portfolio/TransactionHistory.jsx
// ✅ AGORA COMPATÍVEL COM PortfolioPage (usa props, não faz fetch interno)

import React, { useState, useMemo } from 'react';
import { 
  formatCurrency, 
  formatDate, 
  formatQuantity 
} from '../../utils/formatters';

import { API_BASE_URL } from '../../utils/constants';   // ✅ IMPORTAÇÃO CORRETA

import '../../styles/components/transactions.css';

function TransactionHistory({ transactions, onRefresh }) {
  const [filter, setFilter] = useState('all'); // all, buy, sell

  // Aplica filtro sem alterar estado externo
  const filteredTransactions = useMemo(() => {
    if (!Array.isArray(transactions)) return [];
    if (filter === 'all') return transactions;
    return transactions.filter(tx => tx.type?.toLowerCase() === filter);
  }, [transactions, filter]);

  const deleteTransaction = async (id) => {
    if (!window.confirm('Deseja realmente excluir esta transação?')) return;

    try {
      // ✅ URL CORRIGIDA — SEM HARDCODED
      const response = await fetch(`${API_BASE_URL}/transactions/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) throw new Error('Failed to delete transaction');

      // Recarrega lista usando a função da página
      await onRefresh();
    } catch (err) {
      alert('Erro ao excluir transação: ' + err.message);
    }
  };

  return (
    <div className="transaction-history">
      
      {/* Header com Filtros */}
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

      {/* Empty */}
      {filteredTransactions.length === 0 ? (
        <div className="no-transactions">
          <p>Nenhuma transação encontrada</p>
        </div>
      ) : (
        <>
          {/* Tabela */}
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
                    <td>{formatDate(tx.date, 'short')}</td>
                    <td className="crypto-cell">
                      <span className="crypto-symbol">{tx.cryptoSymbol}</span>
                    </td>
                    
                    <td>
                      <span className={`transaction-type ${tx.type.toLowerCase()}`}>
                        {tx.type === 'BUY' ? 'Compra' : 'Venda'}
                      </span>
                    </td>

                    <td>{formatQuantity(tx.quantity)}</td>
                    <td>{formatCurrency(tx.price)}</td>
                    <td className="total-cell">
                      {formatCurrency(tx.quantity * tx.price)}
                    </td>

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

          {/* Footer */}
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
