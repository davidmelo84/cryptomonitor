// front/crypto-monitor-frontend/src/components/portfolio/AddTransactionModal.jsx

import React, { useState } from 'react';
import { X, TrendingUp, TrendingDown } from 'lucide-react';

function AddTransactionModal({ onClose, onSubmit }) {
  const [formData, setFormData] = useState({
    coinSymbol: '',
    coinName: '',
    type: 'BUY',
    quantity: '',
    pricePerUnit: '',
    transactionDate: new Date().toISOString().slice(0, 16),
    notes: ''
  });

  const popularCryptos = [
    { symbol: 'BTC', name: 'Bitcoin' },
    { symbol: 'ETH', name: 'Ethereum' },
    { symbol: 'ADA', name: 'Cardano' },
    { symbol: 'DOT', name: 'Polkadot' },
    { symbol: 'LINK', name: 'Chainlink' },
    { symbol: 'SOL', name: 'Solana' }
  ];

  const handleSubmit = (e) => {
    e.preventDefault();
    
    if (!formData.coinSymbol || !formData.quantity || !formData.pricePerUnit) {
      alert('⚠️ Preencha todos os campos obrigatórios');
      return;
    }

    onSubmit({
      ...formData,
      coinSymbol: formData.coinSymbol.toUpperCase(),
      quantity: parseFloat(formData.quantity),
      pricePerUnit: parseFloat(formData.pricePerUnit)
    });
  };

  const selectCrypto = (crypto) => {
    setFormData({
      ...formData,
      coinSymbol: crypto.symbol,
      coinName: crypto.name
    });
  };

  const totalValue = formData.quantity && formData.pricePerUnit
    ? (parseFloat(formData.quantity) * parseFloat(formData.pricePerUnit)).toFixed(2)
    : '0.00';

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="bg-gradient-to-r from-indigo-500 to-purple-600 text-white p-6 rounded-t-2xl flex justify-between items-center">
          <h2 className="text-2xl font-bold">Nova Transação</h2>
          <button
            onClick={onClose}
            className="text-white hover:bg-white/20 rounded-full p-2 transition-colors"
          >
            <X size={24} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6">
          {/* Transaction Type */}
          <div className="mb-6">
            <label className="block text-sm font-bold text-gray-700 mb-3">
              Tipo de Transação
            </label>
            <div className="grid grid-cols-2 gap-4">
              <button
                type="button"
                onClick={() => setFormData({ ...formData, type: 'BUY' })}
                className={`p-4 rounded-lg border-2 font-bold flex items-center justify-center gap-2 transition-all ${
                  formData.type === 'BUY'
                    ? 'border-green-500 bg-green-50 text-green-700'
                    : 'border-gray-300 bg-white text-gray-600 hover:border-green-300'
                }`}
              >
                <TrendingUp size={20} />
                Compra
              </button>
              <button
                type="button"
                onClick={() => setFormData({ ...formData, type: 'SELL' })}
                className={`p-4 rounded-lg border-2 font-bold flex items-center justify-center gap-2 transition-all ${
                  formData.type === 'SELL'
                    ? 'border-red-500 bg-red-50 text-red-700'
                    : 'border-gray-300 bg-white text-gray-600 hover:border-red-300'
                }`}
              >
                <TrendingDown size={20} />
                Venda
              </button>
            </div>
          </div>

          {/* Popular Cryptos */}
          <div className="mb-6">
            <label className="block text-sm font-bold text-gray-700 mb-3">
              Criptomoedas Populares
            </label>
            <div className="grid grid-cols-3 gap-2">
              {popularCryptos.map((crypto) => (
                <button
                  key={crypto.symbol}
                  type="button"
                  onClick={() => selectCrypto(crypto)}
                  className={`p-3 rounded-lg border-2 font-semibold text-sm transition-all ${
                    formData.coinSymbol === crypto.symbol
                      ? 'border-indigo-500 bg-indigo-50 text-indigo-700'
                      : 'border-gray-300 bg-white text-gray-600 hover:border-indigo-300'
                  }`}
                >
                  {crypto.symbol}
                </button>
              ))}
            </div>
          </div>

          {/* Coin Symbol */}
          <div className="mb-4">
            <label className="block text-sm font-bold text-gray-700 mb-2">
              Símbolo da Moeda *
            </label>
            <input
              type="text"
              value={formData.coinSymbol}
              onChange={(e) => setFormData({ ...formData, coinSymbol: e.target.value.toUpperCase() })}
              className="w-full p-3 border-2 border-gray-300 rounded-lg focus:border-indigo-500 focus:outline-none"
              placeholder="Ex: BTC, ETH, ADA"
              required
            />
          </div>

          {/* Coin Name */}
          <div className="mb-4">
            <label className="block text-sm font-bold text-gray-700 mb-2">
              Nome da Moeda *
            </label>
            <input
              type="text"
              value={formData.coinName}
              onChange={(e) => setFormData({ ...formData, coinName: e.target.value })}
              className="w-full p-3 border-2 border-gray-300 rounded-lg focus:border-indigo-500 focus:outline-none"
              placeholder="Ex: Bitcoin, Ethereum"
              required
            />
          </div>

          {/* Quantity */}
          <div className="mb-4">
            <label className="block text-sm font-bold text-gray-700 mb-2">
              Quantidade *
            </label>
            <input
              type="number"
              step="0.00000001"
              value={formData.quantity}
              onChange={(e) => setFormData({ ...formData, quantity: e.target.value })}
              className="w-full p-3 border-2 border-gray-300 rounded-lg focus:border-indigo-500 focus:outline-none"
              placeholder="Ex: 0.5"
              required
            />
          </div>

          {/* Price Per Unit */}
          <div className="mb-4">
            <label className="block text-sm font-bold text-gray-700 mb-2">
              Preço por Unidade (USD) *
            </label>
            <input
              type="number"
              step="0.01"
              value={formData.pricePerUnit}
              onChange={(e) => setFormData({ ...formData, pricePerUnit: e.target.value })}
              className="w-full p-3 border-2 border-gray-300 rounded-lg focus:border-indigo-500 focus:outline-none"
              placeholder="Ex: 43250.50"
              required
            />
          </div>

          {/* Total Value (Calculated) */}
          <div className="mb-4 p-4 bg-indigo-50 rounded-lg border-2 border-indigo-200">
            <div className="flex justify-between items-center">
              <span className="text-sm font-bold text-indigo-700">Valor Total:</span>
              <span className="text-2xl font-bold text-indigo-900">
                ${totalValue}
              </span>
            </div>
          </div>

          {/* Transaction Date */}
          <div className="mb-4">
            <label className="block text-sm font-bold text-gray-700 mb-2">
              Data da Transação
            </label>
            <input
              type="datetime-local"
              value={formData.transactionDate}
              onChange={(e) => setFormData({ ...formData, transactionDate: e.target.value })}
              className="w-full p-3 border-2 border-gray-300 rounded-lg focus:border-indigo-500 focus:outline-none"
            />
          </div>

          {/* Notes */}
          <div className="mb-6">
            <label className="block text-sm font-bold text-gray-700 mb-2">
              Observações (Opcional)
            </label>
            <textarea
              value={formData.notes}
              onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
              className="w-full p-3 border-2 border-gray-300 rounded-lg focus:border-indigo-500 focus:outline-none"
              rows="3"
              placeholder="Ex: Compra mensal, estratégia DCA..."
            />
          </div>

          {/* Buttons */}
          <div className="flex gap-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-6 py-3 bg-gray-200 text-gray-700 rounded-lg font-bold hover:bg-gray-300 transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="flex-1 px-6 py-3 bg-gradient-to-r from-indigo-500 to-purple-600 text-white rounded-lg font-bold hover:scale-105 transition-transform shadow-lg"
            >
              {formData.type === 'BUY' ? '✓ Comprar' : '✓ Vender'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default AddTransactionModal;