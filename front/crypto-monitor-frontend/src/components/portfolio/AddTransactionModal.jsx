// front/crypto-monitor-frontend/src/components/portfolio/AddTransactionModal.jsx
// ✅ VERSÃO COM VISUAL MELHORADO

import React, { useState, useEffect, useCallback } from 'react';
import { formatCurrency, formatSymbol } from '../../utils/formatters';
import { API_BASE_URL } from '../../utils/constants';
import { DollarSign, Hash, Calendar, TrendingUp, TrendingDown } from 'lucide-react';

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
  }, [formData.cryptoSymbol]);

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
    <form onSubmit={handleSubmit} className="space-y-6">
      {error && (
        <div className="bg-red-500/20 border border-red-500/50 rounded-xl p-4 flex items-center gap-3">
          <span className="text-red-300 text-sm">⚠️ {error}</span>
        </div>
      )}

      {/* Crypto Selection */}
      <div>
        <label className="flex items-center gap-2 text-sm font-semibold text-white/80 mb-2">
          <TrendingUp className="w-4 h-4 text-purple-400" />
          Criptomoeda
        </label>
        <select
          name="cryptoSymbol"
          value={formData.cryptoSymbol}
          onChange={handleChange}
          required
          className="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent"
        >
          <option value="">Selecione...</option>
          {availableCryptos.map(crypto => (
            <option key={crypto.symbol} value={crypto.symbol} className="bg-gray-900">
              {formatSymbol(crypto.symbol)} - {crypto.name}
            </option>
          ))}
        </select>
      </div>

      {/* Type Selection */}
      <div>
        <label className="text-sm font-semibold text-white/80 mb-3 block">
          Tipo de Transação
        </label>
        <div className="grid grid-cols-2 gap-4">
          <button
            type="button"
            onClick={() => handleChange({ target: { name: 'type', value: 'BUY' }})}
            className={`flex items-center justify-center gap-2 px-6 py-4 rounded-xl font-semibold transition-all ${
              formData.type === 'BUY'
                ? 'bg-emerald-500 text-white shadow-lg shadow-emerald-500/50'
                : 'bg-white/5 text-white/60 border border-white/10 hover:bg-white/10'
            }`}
          >
            <TrendingUp className="w-5 h-5" />
            Compra
          </button>
          <button
            type="button"
            onClick={() => handleChange({ target: { name: 'type', value: 'SELL' }})}
            className={`flex items-center justify-center gap-2 px-6 py-4 rounded-xl font-semibold transition-all ${
              formData.type === 'SELL'
                ? 'bg-red-500 text-white shadow-lg shadow-red-500/50'
                : 'bg-white/5 text-white/60 border border-white/10 hover:bg-white/10'
            }`}
          >
            <TrendingDown className="w-5 h-5" />
            Venda
          </button>
        </div>
      </div>

      {/* Quantity */}
      <div>
        <label className="flex items-center gap-2 text-sm font-semibold text-white/80 mb-2">
          <Hash className="w-4 h-4 text-blue-400" />
          Quantidade
        </label>
        <input
          type="number"
          name="quantity"
          value={formData.quantity}
          onChange={handleChange}
          placeholder="0.00000000"
          step="0.00000001"
          min="0"
          required
          className="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white placeholder-white/40 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
      </div>

      {/* Price */}
      <div>
        <label className="flex items-center gap-2 text-sm font-semibold text-white/80 mb-2">
          <DollarSign className="w-4 h-4 text-emerald-400" />
          Preço Unitário
        </label>
        <div className="flex gap-2">
          <input
            type="number"
            name="price"
            value={formData.price}
            onChange={handleChange}
            placeholder="0.00"
            step="0.01"
            min="0"
            required
            className="flex-1 px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white placeholder-white/40 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
          />
          {currentPrice && (
            <button
              type="button"
              onClick={useCurrentPrice}
              className="px-4 py-3 bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 rounded-xl font-medium border border-emerald-500/30 transition-all whitespace-nowrap"
            >
              Atual: {formatCurrency(currentPrice)}
            </button>
          )}
        </div>
      </div>

      {/* Date */}
      <div>
        <label className="flex items-center gap-2 text-sm font-semibold text-white/80 mb-2">
          <Calendar className="w-4 h-4 text-orange-400" />
          Data
        </label>
        <input
          type="date"
          name="date"
          value={formData.date}
          onChange={handleChange}
          max={new Date().toISOString().split('T')[0]}
          required
          className="w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent"
        />
      </div>

      {/* Total */}
      {formData.quantity && formData.price && (
        <div className="bg-purple-500/10 border border-purple-500/30 rounded-xl p-4">
          <div className="flex items-center justify-between">
            <span className="text-white/60 text-sm font-semibold">Valor Total</span>
            <span className="text-2xl font-bold text-white">
              {formatCurrency(calculateTotal())}
            </span>
          </div>
        </div>
      )}

      {/* Actions */}
      <div className="flex gap-4 pt-4">
        <button
          type="button"
          onClick={onClose}
          disabled={loading}
          className="flex-1 px-6 py-3 bg-white/5 hover:bg-white/10 text-white rounded-xl font-semibold border border-white/10 transition-all disabled:opacity-50"
        >
          Cancelar
        </button>
        <button
          type="submit"
          disabled={loading}
          className="flex-1 px-6 py-3 bg-gradient-to-r from-purple-500 to-indigo-600 hover:from-purple-600 hover:to-indigo-700 text-white rounded-xl font-semibold transition-all disabled:opacity-50 shadow-lg shadow-purple-500/50"
        >
          {loading ? 'Salvando...' : '✅ Adicionar Transação'}
        </button>
      </div>
    </form>
  );
}

export default AddTransactionModal;