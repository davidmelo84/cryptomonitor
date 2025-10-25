// front/crypto-monitor-frontend/src/components/dashboard/CryptocurrenciesCard.jsx
// ✅ VERSÃO CORRIGIDA - Lista de Criptomoedas Funcionando

import React, { useState, useMemo } from 'react';
import { Coins } from 'lucide-react';
import { useTheme } from '../../contexts/ThemeContext';
import CryptoCard from './CryptoCard';

function CryptocurrenciesCard({
  availableCryptos,
  selectedCryptos,
  onToggleSelection,
  onClearSelection
}) {
  const { isDark } = useTheme();
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState('marketCap');

  // ✅ Filtrar e ordenar cryptos
  const filteredCryptos = useMemo(() => {
    let filtered = availableCryptos;

    // Aplicar busca
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(crypto =>
        crypto.name?.toLowerCase().includes(term) ||
        crypto.symbol?.toLowerCase().includes(term)
      );
    }

    // Aplicar ordenação
    filtered = [...filtered].sort((a, b) => {
      switch (sortBy) {
        case 'marketCap':
          return (b.marketCap || 0) - (a.marketCap || 0);
        case 'price':
          return (b.currentPrice || 0) - (a.currentPrice || 0);
        case 'change':
          return (b.priceChange24h || 0) - (a.priceChange24h || 0);
        case 'name':
          return (a.name || '').localeCompare(b.name || '');
        default:
          return 0;
      }
    });

    return filtered;
  }, [availableCryptos, searchTerm, sortBy]);

  return (
    <div className={`cryptocurrencies-card ${isDark ? 'dark' : ''}`}>
      {/* Header */}
      <div className="cryptocurrencies-header">
        <div>
          <h2 className="cryptocurrencies-title">
            <Coins size={28} color="#667eea" />
            Criptomoedas Disponíveis
          </h2>
          <p className="cryptocurrencies-subtitle">
            {filteredCryptos.length} de {availableCryptos.length} moedas
            {selectedCryptos.length > 0 && ` • ${selectedCryptos.length} selecionadas`}
          </p>
        </div>

        {/* Actions */}
        <div className="cryptocurrencies-actions">
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="🔍 Buscar moeda..."
            className="search-input"
          />

          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="sort-select"
          >
            <option value="marketCap">Market Cap</option>
            <option value="price">Preço</option>
            <option value="change">Variação 24h</option>
            <option value="name">Nome</option>
          </select>

          {selectedCryptos.length > 0 && (
            <button
              onClick={onClearSelection}
              className="clear-button"
            >
              Limpar ({selectedCryptos.length})
            </button>
          )}
        </div>
      </div>

      {/* Grid de Cryptos */}
      {filteredCryptos.length === 0 ? (
        <div className="cryptocurrencies-empty">
          <p>Nenhuma criptomoeda encontrada</p>
        </div>
      ) : (
        <div className="cryptocurrencies-grid">
          {filteredCryptos.map((crypto) => {
            const identifier = crypto.coinId || crypto.name || crypto.symbol;
            const isSelected = selectedCryptos.some(c => {
              const selectedId = c.coinId || c.name || c.symbol;
              return selectedId === identifier;
            });

            return (
              <CryptoCard
                key={identifier}
                crypto={crypto}
                isSelected={isSelected}
                onToggle={() => onToggleSelection(crypto)}
              />
            );
          })}
        </div>
      )}
    </div>
  );
}

export default CryptocurrenciesCard;