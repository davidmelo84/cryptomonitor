// front/crypto-monitor-frontend/src/components/portfolio/AddTransactionModal.jsx
// ✅ REFATORADO - SEM CSS INLINE
// ✅ URLs corrigidas para usar API_BASE_URL

import React, { useState, useEffect, useCallback } from 'react';
import { formatCurrency, formatSymbol } from '../../utils/formatters';
import { API_BASE_URL } from '../../utils/constants';
import '../../styles/AddTransactionModal.css';

function AddTransactionModal({ isOpen, onClose, onTransactionAdded }) {
  const [formData, setFormData] = useState({
    cryptoSymbol: '',
    type: 'BUY',
    quantity: '',
    price: '',
    date: new Date().toISOString().split('T')[0]
  });

  const [availableCryptos, setAvailableCryptos] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [currentPrice, setCurrentPrice] = useState(null);

  // ✅ CORREÇÃO: useCallback para fetchAvailableCryptos
  const fetchAvailableCryptos = useCallback(async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/crypto/list`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });

      if (!response.ok) throw new Error('Failed to fetch cryptos');

      const data = await response.json();
      setAvailableCryptos(data);
      
      if (data.length > 0 && !formData.cryptoSymbol) {
        setFormData(prev => ({ ...prev, cryptoSymbol: data[0].symbol }));
      }
    } catch (err) {
      console.error('Error fetching cryptos:', err);
      setAvailableCryptos([
        { symbol: 'BTC', name: 'Bitcoin' },
        { symbol: 'ETH', name: 'Ethereum' },
        { symbol: 'BNB', name: 'Binance Coin' }
      ]);
    }
  }, [formData.cryptoSymbol]); // Dependência ok

  // ✅ CORREÇÃO: useCallback para fetchCurrentPrice
  const fetchCurrentPrice = useCallback(async (symbol) => {
    try {
      const response = await fetch(`${API_BASE_URL}/crypto/${symbol}`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });

      if (!response.ok) throw new Error('Failed to fetch price');

      const data = await response.json();
      setCurrentPrice(data.currentPrice);
      
      if (!formData.price) {
        setFormData(prev => ({ ...prev, price: data.currentPrice }));
      }
    } catch (err) {
      console.error('Error fetching price:', err);
      setCurrentPrice(null);
    }
  }, [formData.price]);

  useEffect(() => {
    if (isOpen) {
      fetchAvailableCryptos();
    }
  }, [isOpen, fetchAvailableCryptos]);

  useEffect(() => {
    if (formData.cryptoSymbol) {
      fetchCurrentPrice(formData.cryptoSymbol);
    }
  }, [formData.cryptoSymbol, fetchCurrentPrice]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    if (!formData.cryptoSymbol || !formData.quantity || !formData.price) {
      setError('Preencha todos os campos obrigatórios');
      setLoading(false);
      return;
    }

    if (parseFloat(formData.quantity) <= 0) {
      setError('Quantidade deve ser maior que zero');
      setLoading(false);
      return;
    }

    if (parseFloat(formData.price) <= 0) {
      setError('Preço deve ser maior que zero');
      setLoading(false);
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/transactions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({
          ...formData,
          quantity: parseFloat(formData.quantity),
          price: parseFloat(formData.price)
        })
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Failed to add transaction');
      }

      const newTransaction = await response.json();
      
      setFormData({
        cryptoSymbol: availableCryptos[0]?.symbol || '',
        type: 'BUY',
        quantity: '',
        price: '',
        date: new Date().toISOString().split('T')[0]
      });

      if (onTransactionAdded) {
        onTransactionAdded(newTransaction);
      }

      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const calculateTotal = () => {
    const quantity = parseFloat(formData.quantity) || 0;
    const price = parseFloat(formData.price) || 0;
    return quantity * price;
  };

  const useCurrentPrice = () => {
    if (currentPrice) {
      setFormData(prev => ({ ...prev, price: currentPrice }));
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        
        <div className="modal-header">
          <h2>Nova Transação</h2>
          <button className="close-btn" onClick={onClose}>✕</button>
        </div>

        <form onSubmit={handleSubmit} className="transaction-form">
          
          {error && (
            <div className="error-message">
              ⚠️ {error}
            </div>
          )}

          <div className="form-group">
            <label htmlFor="cryptoSymbol">Criptomoeda</label>
            <select
              id="cryptoSymbol"
              name="cryptoSymbol"
              value={formData.cryptoSymbol}
              onChange={handleChange}
              required
            >
              <option value="">Selecione...</option>
              {availableCryptos.map(crypto => (
                <option key={crypto.symbol} value={crypto.symbol}>
                  {formatSymbol(crypto.symbol)} - {crypto.name}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label>Tipo de Transação</label>
            <div className="radio-group">
              <label className="radio-label">
                <input
                  type="radio"
                  name="type"
                  value="BUY"
                  checked={formData.type === 'BUY'}
                  onChange={handleChange}
                />
                <span className="radio-text">Compra</span>
              </label>
              <label className="radio-label">
                <input
                  type="radio"
                  name="type"
                  value="SELL"
                  checked={formData.type === 'SELL'}
                  onChange={handleChange}
                />
                <span className="radio-text">Venda</span>
              </label>
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="quantity">Quantidade</label>
            <input
              type="number"
              id="quantity"
              name="quantity"
              value={formData.quantity}
              onChange={handleChange}
              placeholder="0.00000000"
              step="0.00000001"
              min="0"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="price">Preço Unitário</label>
            <div className="price-input-group">
              <input
                type="number"
                id="price"
                name="price"
                value={formData.price}
                onChange={handleChange}
                placeholder="0.00"
                step="0.01"
                min="0"
                required
              />
              {currentPrice && (
                <button 
                  type="button" 
                  className="use-current-price-btn"
                  onClick={useCurrentPrice}
                  title="Usar preço atual"
                >
                  Preço atual: {formatCurrency(currentPrice)}
                </button>
              )}
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="date">Data</label>
            <input
              type="date"
              id="date"
              name="date"
              value={formData.date}
              onChange={handleChange}
              max={new Date().toISOString().split('T')[0]}
              required
            />
          </div>

          {formData.quantity && formData.price && (
            <div className="form-group total-display">
              <label>Valor Total</label>
              <div className="total-value">
                {formatCurrency(calculateTotal())}
              </div>
            </div>
          )}

          <div className="form-actions">
            <button 
              type="button" 
              className="btn-cancel" 
              onClick={onClose}
              disabled={loading}
            >
              Cancelar
            </button>
            <button 
              type="submit" 
              className="btn-submit"
              disabled={loading}
            >
              {loading ? 'Salvando...' : 'Adicionar Transação'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default AddTransactionModal;
