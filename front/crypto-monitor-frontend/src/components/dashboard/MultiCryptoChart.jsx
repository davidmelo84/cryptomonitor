// front/crypto-monitor-frontend/src/components/dashboard/MultiCryptoChart.jsx

import React, { useState, useEffect, useCallback } from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend
} from 'recharts';
import { GitCompare } from 'lucide-react';
import { API_BASE_URL } from '../../utils/constants';

function MultiCryptoChart({ selectedCryptos }) {
  const [chartData, setChartData] = useState([]);
  const [selectedPeriod, setSelectedPeriod] = useState(7);
  const [loading, setLoading] = useState(false);

  const colors = [
    '#667eea', // Roxo
    '#10b981', // Verde
    '#ef4444', // Vermelho
    '#f59e0b', // Laranja
    '#06b6d4', // Ciano
    '#ec4899'  // Rosa
  ];

  const periods = [
    { value: 1, label: '24h' },
    { value: 7, label: '7d' },
    { value: 30, label: '30d' }
  ];

  const mergeCryptoData = (results, cryptos) => {
    if (!results || results.length === 0) return [];

    const baseData = results[0]?.data || [];
    
    return baseData.map((point, index) => {
      const dataPoint = { date: point.date };
      
      results.forEach((result, cryptoIndex) => {
        if (result && result.data && result.data[index]) {
          const cryptoSymbol = cryptos[cryptoIndex].symbol;
          dataPoint[cryptoSymbol] = result.data[index].price;
        }
      });
      
      return dataPoint;
    });
  };

  const formatPrice = (value) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      notation: 'compact',
      minimumFractionDigits: 0
    }).format(value);
  };

  const CustomTooltip = ({ active, payload, label }) => {
    if (!active || !payload || !payload.length) return null;

    return (
      <div className="bg-white p-4 rounded-lg shadow-xl border-2 border-indigo-200">
        <p className="text-sm font-bold text-gray-800 mb-3">{label}</p>
        {payload.map((entry, index) => (
          <div key={index} className="flex items-center justify-between gap-4 mb-1">
            <span className="text-sm font-semibold" >
              {entry.name}:
            </span>
            <span className="text-sm font-bold">
              {formatPrice(entry.value)}
            </span>
          </div>
        ))}
      </div>
    );
  };

  // ✅ useCallback para evitar warning
  const fetchAllData = useCallback(async () => {
    setLoading(true);
    
    try {
      const promises = selectedCryptos.map(crypto =>
        fetch(`${API_BASE_URL}/crypto/history/${crypto.coinId}?days=${selectedPeriod}`)
          .then(res => res.json())
          .catch(() => null)
      );

      const results = await Promise.all(promises);
      const combinedData = mergeCryptoData(results, selectedCryptos);
      setChartData(combinedData);

    } catch (error) {
      console.error('Erro ao buscar dados:', error);
    } finally {
      setLoading(false);
    }
  }, [selectedCryptos, selectedPeriod]);

  useEffect(() => {
    if (selectedCryptos.length > 0) {
      fetchAllData();
    }
  }, [fetchAllData]);

  if (selectedCryptos.length === 0) {
    return (
      <div className="bg-white p-16 rounded-[20px] shadow-md text-center">
        <GitCompare size={48} className="mx-auto mb-4 text-gray-300" />
        <p className="text-gray-400 text-lg">
          Selecione pelo menos 2 criptomoedas para comparar
        </p>
      </div>
    );
  }

  return (
    <div className="bg-white p-8 rounded-[20px] shadow-md">
      {/* Header */}
      <div className="flex justify-between items-center mb-6 flex-wrap gap-4">
        <div>
          <h2 className="text-2xl font-bold text-gray-800 flex items-center gap-3">
            <GitCompare size={28} color="#667eea" />
            Comparação de Criptomoedas
          </h2>
          <p className="text-gray-600 text-sm mt-2">
            {selectedCryptos.length} moeda(s) selecionada(s)
          </p>
        </div>

        {/* Period Selector */}
        <div className="flex gap-2">
          {periods.map((period) => (
            <button
              key={period.value}
              onClick={() => setSelectedPeriod(period.value)}
              className={`px-4 py-2 rounded-lg font-bold text-sm transition-all ${
                selectedPeriod === period.value
                  ? 'bg-indigo-500 text-white shadow-lg'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              {period.label}
            </button>
          ))}
        </div>
      </div>

      {/* Legend Cards */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3 mb-6">
        {selectedCryptos.map((crypto, index) => (
          <div
            key={crypto.coinId}
            className="flex items-center gap-2 p-3 rounded-lg border-2"
            style={{ borderColor: colors[index % colors.length] }}
          >
            <div
              className="w-3 h-3 rounded-full"
              style={{ backgroundColor: colors[index % colors.length] }}
            />
            <span className="text-sm font-bold text-gray-800">
              {crypto.symbol?.toUpperCase()}
            </span>
          </div>
        ))}
      </div>

      {/* Chart */}
      {loading ? (
        <div className="flex items-center justify-center h-[400px]">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-500 mx-auto mb-4"></div>
            <p className="text-gray-600">Carregando comparação...</p>
          </div>
        </div>
      ) : (
        <div style={{ width: '100%', height: '400px' }}>
          <ResponsiveContainer>
            <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis 
                dataKey="date" 
                stroke="#666"
                tick={{ fontSize: 12 }}
              />
              <YAxis 
                stroke="#666"
                tick={{ fontSize: 12 }}
                tickFormatter={formatPrice}
              />
              <Tooltip content={<CustomTooltip />} />
              <Legend />
              
              {selectedCryptos.map((crypto, index) => (
                <Line
                  key={crypto.coinId}
                  type="monotone"
                  dataKey={crypto.symbol}
                  stroke={colors[index % colors.length]}
                  strokeWidth={3}
                  dot={false}
                  name={crypto.name}
                  activeDot={{ r: 6 }}
                />
              ))}
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
}

export default MultiCryptoChart;
