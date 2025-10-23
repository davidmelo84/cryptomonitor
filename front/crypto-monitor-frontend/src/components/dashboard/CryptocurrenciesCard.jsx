import React, { useState } from 'react';
import { Zap } from 'lucide-react';
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

  const filteredCryptos = availableCryptos
    .filter(crypto =>
      crypto.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      crypto.symbol.toLowerCase().includes(searchTerm.toLowerCase())
    )
    .sort((a, b) => {
      if (sortBy === 'marketCap') return (b.marketCap || 0) - (a.marketCap || 0);
      if (sortBy === 'price') return (b.currentPrice || 0) - (a.currentPrice || 0);
      if (sortBy === 'change') return (b.priceChange24h || 0) - (a.priceChange24h || 0);
      return 0;
    });

  return (
    <div className={`cryptocurrencies-card ${isDark ? 'dark' : ''}`}>
      <div className="cryptocurrencies-header">
        <div>
          <h2 className="cryptocurrencies-title">
            <Zap size={28} color="#667eea" />
            Criptomoedas Disponíveis
          </h2>
          <p className="cryptocurrencies-subtitle">
            {selectedCryptos.length} de {filteredCryptos.length} selecionadas
          </p>
        </div>
        
        <div className="cryptocurrencies-actions">
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="search-input"
            placeholder="🔍 Buscar moeda..."
          />
          
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="sort-select"
          >
            <option value="marketCap">Market Cap</option>
            <option value="price">Preço</option>
            <option value="change">Variação</option>
          </select>
          
          {selectedCryptos.length > 0 && (
            <button onClick={onClearSelection} className="clear-button">
              ✕ Limpar
            </button>
          )}
        </div>
      </div>
      
      <div className="cryptocurrencies-grid">
        {filteredCryptos.map((crypto, index) => {
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
      
      {filteredCryptos.length === 0 && (
        <div className="cryptocurrencies-empty">
          <p>Nenhuma criptomoeda encontrada</p>
          <p>Tente buscar por outro termo</p>
        </div>
      )}
    </div>
  );
}

export default CryptocurrenciesCard;