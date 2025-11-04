// front/crypto-monitor-frontend/src/components/dashboard/ChartTabs.jsx

import React, { useState, useEffect } from 'react';
import { TrendingUp, GitCompare } from 'lucide-react';
import PriceChart from './PriceChart';
import MultiCryptoChart from './MultiCryptoChart';

function ChartTabs({ selectedCryptos }) {
  const [activeTab, setActiveTab] = useState('individual');
  const [selectedCryptoForChart, setSelectedCryptoForChart] = useState(null);

  // Auto-selecionar primeira crypto
  useEffect(() => {
    if (selectedCryptos.length > 0 && !selectedCryptoForChart) {
      setSelectedCryptoForChart(selectedCryptos[0]);
    } else if (selectedCryptos.length === 0) {
      setSelectedCryptoForChart(null);
    }
  }, [selectedCryptos, selectedCryptoForChart]);

  if (selectedCryptos.length === 0) {
    return (
      <div className="bg-white p-16 rounded-[20px] shadow-md text-center">
        <TrendingUp size={48} className="mx-auto mb-4 text-gray-300" />
        <p className="text-gray-400 text-lg">
          Selecione criptomoedas para visualizar gráficos
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Tab Buttons */}
      <div className="flex gap-3 border-b-2 border-gray-200 pb-2">
        <button
          onClick={() => setActiveTab('individual')}
          className={`flex items-center gap-2 px-6 py-3 font-bold rounded-t-lg transition-all ${
            activeTab === 'individual'
              ? 'bg-indigo-500 text-white shadow-lg'
              : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
          }`}
        >
          <TrendingUp size={20} />
          Gráfico Individual
        </button>
        
        <button
          onClick={() => setActiveTab('comparison')}
          className={`flex items-center gap-2 px-6 py-3 font-bold rounded-t-lg transition-all ${
            activeTab === 'comparison'
              ? 'bg-indigo-500 text-white shadow-lg'
              : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
          }`}
        >
          <GitCompare size={20} />
          Comparação
          {selectedCryptos.length > 1 && (
            <span className="bg-white text-indigo-600 rounded-full w-6 h-6 flex items-center justify-center text-xs font-bold">
              {selectedCryptos.length}
            </span>
          )}
        </button>
      </div>

      {/* Tab Content */}
      {activeTab === 'individual' ? (
        <>
          {/* Crypto Selector */}
          <div className="flex gap-4 overflow-x-auto pb-2">
            {selectedCryptos.map((crypto) => (
              <button
                key={crypto.coinId || crypto.symbol}
                onClick={() => setSelectedCryptoForChart(crypto)}
                className={`px-6 py-3 rounded-lg font-bold whitespace-nowrap transition-all ${
                  selectedCryptoForChart?.coinId === crypto.coinId
                    ? 'bg-indigo-500 text-white shadow-lg'
                    : 'bg-white text-gray-700 border-2 border-gray-200 hover:border-indigo-300'
                }`}
              >
                {crypto.name} ({crypto.symbol?.toUpperCase()})
              </button>
            ))}
          </div>

          {/* Individual Chart */}
          {selectedCryptoForChart && (
            <PriceChart
              coinId={selectedCryptoForChart.coinId || selectedCryptoForChart.name?.toLowerCase()}
              coinName={selectedCryptoForChart.name}
              coinSymbol={selectedCryptoForChart.symbol}
            />
          )}
        </>
      ) : (
        <MultiCryptoChart selectedCryptos={selectedCryptos} />
      )}
    </div>
  );
}

export default ChartTabs;