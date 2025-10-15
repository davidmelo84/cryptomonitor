import React, { useState } from 'react';
import { Zap } from 'lucide-react';
import CryptoCard from './CryptoCard';

function CryptocurrenciesCard({
  availableCryptos,
  selectedCryptos,
  onToggleSelection,
  onClearSelection
}) {
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
    <div className="bg-white p-8 rounded-[20px] shadow-md">
      <div className="flex justify-between items-center mb-6 flex-wrap gap-4">
        <div>
          <h2 className="flex items-center gap-3 m-0 text-2xl font-bold">
            <Zap size={28} color="#667eea" />
            Criptomoedas Disponíveis
          </h2>
          <p className="mt-2 mb-0 text-gray-600 text-sm">
            {selectedCryptos.length} de {filteredCryptos.length} selecionadas
          </p>
        </div>
        
        <div className="flex gap-3 flex-wrap">
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="p-3 border-2 border-gray-200 rounded-lg text-sm min-w-[200px] focus:border-indigo-500 focus:outline-none"
            placeholder="🔍 Buscar moeda..."
          />
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="p-3 border-2 border-gray-200 rounded-lg text-sm cursor-pointer focus:border-indigo-500 focus:outline-none"
          >
            <option value="marketCap">Market Cap</option>
            <option value="price">Preço</option>
            <option value="change">Variação</option>
          </select>
          {selectedCryptos.length > 0 && (
            <button
              onClick={onClearSelection}
              className="bg-red-500 text-white border-none px-5 py-3 rounded-lg cursor-pointer font-bold text-sm hover:bg-red-600"
            >
              ✕ Limpar
            </button>
          )}
        </div>
      </div>
      
      <div className="grid grid-cols-[repeat(auto-fill,minmax(280px,1fr))] gap-5">
        {filteredCryptos.map((crypto, index) => (
          <CryptoCard
            key={crypto.coinId || crypto.name || index}
            crypto={crypto}
            isSelected={selectedCryptos.some(c => {
              const selectedId = c.coinId || c.name || c.symbol;
              const identifier = crypto.coinId || crypto.name || crypto.symbol;
              return selectedId === identifier;
            })}
            onToggle={() => onToggleSelection(crypto)}
          />
        ))}
      </div>
      
      {filteredCryptos.length === 0 && (
        <div className="text-center py-16 px-5 text-gray-400">
          <p className="text-lg mb-3">Nenhuma criptomoeda encontrada</p>
          <p className="text-sm">Tente buscar por outro termo</p>
        </div>
      )}
    </div>
  );
}

export default CryptocurrenciesCard;