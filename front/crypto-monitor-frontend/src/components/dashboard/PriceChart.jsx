// front/crypto-monitor-frontend/src/components/dashboard/PriceChart.jsx
// ✅ REFATORADO - Usando formatters.js

import React, { useState, useEffect, useCallback } from 'react';
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend
} from 'recharts';
import { TrendingUp, TrendingDown, Activity, BarChart2 } from 'lucide-react';
import { API_BASE_URL } from '../../utils/constants';
import { formatCurrency } from '../../utils/formatters';

function PriceChart({ coinId, coinName, coinSymbol }) {
  const [chartData, setChartData] = useState([]);
  const [stats, setStats] = useState(null);
  const [selectedPeriod, setSelectedPeriod] = useState(7);
  const [chartType, setChartType] = useState('line');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const periods = [
    { value: 1, label: '24h' },
    { value: 7, label: '7d' },
    { value: 30, label: '30d' },
    { value: 90, label: '90d' },
    { value: 365, label: '1y' }
  ];

  const generateMockData = useCallback((days) => {
    const data = [];
    const now = Date.now();
    const basePrice = 43000;
    
    for (let i = days; i >= 0; i--) {
      const date = new Date(now - i * 24 * 60 * 60 * 1000);
      const randomChange = (Math.random() - 0.5) * 2000;
      
      data.push({
        date: date.toLocaleDateString('pt-BR'),
        price: basePrice + randomChange,
        volume: Math.random() * 1000000000
      });
    }
    
    return data;
  }, []);

  const fetchChartData = useCallback(async () => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch(
        `${API_BASE_URL}/crypto/history/${coinId}?days=${selectedPeriod}`
      );
      
      if (!response.ok) throw new Error('Falha ao buscar dados');
      
      const data = await response.json();
      
      setChartData(data.data);
      setStats(data.stats);
      
    } catch (err) {
      console.error('Erro ao buscar histórico:', err);
      setError(err.message);
      // Mock data para demonstração
      const mockData = generateMockData(selectedPeriod);
      setChartData(mockData);
      setStats({
        min: 40000,
        max: 45000,
        current: 43250,
        change: 2.5
      });
    } finally {
      setLoading(false);
    }
  }, [coinId, selectedPeriod, generateMockData]);

  useEffect(() => {
    fetchChartData();
  }, [fetchChartData]);

  const formatVolume = (value) => {
    if (value >= 1e9) return `$${(value / 1e9).toFixed(2)}B`;
    if (value >= 1e6) return `$${(value / 1e6).toFixed(2)}M`;
    return `$${(value / 1e3).toFixed(2)}K`;
  };

  const CustomTooltip = ({ active, payload }) => {
    if (!active || !payload || !payload.length) return null;

    return (
      <div className="bg-white p-4 rounded-lg shadow-xl border-2 border-indigo-200">
        <p className="text-sm font-bold text-gray-800 mb-2">
          {payload[0].payload.date}
        </p>
        <p className="text-lg font-bold text-indigo-600 mb-1">
          {formatCurrency(payload[0].value)}
        </p>
        {payload[1] && (
          <p className="text-xs text-gray-600">
            Volume: {formatVolume(payload[1].value)}
          </p>
        )}
      </div>
    );
  };

  if (loading) {
    return (
      <div className="bg-white p-8 rounded-[20px] shadow-md flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <Activity className="animate-spin mx-auto mb-4" size={40} color="#667eea" />
          <p className="text-gray-600">Carregando gráfico...</p>
        </div>
      </div>
    );
  }

  const isPositiveChange = stats?.change >= 0;

  return (
    <div className="bg-white p-8 rounded-[20px] shadow-md">
      {/* Header */}
      <div className="flex justify-between items-start mb-6 flex-wrap gap-4">
        <div>
          <h2 className="text-2xl font-bold text-gray-800 flex items-center gap-3 mb-2">
            <BarChart2 size={28} color="#667eea" />
            {coinName} ({coinSymbol?.toUpperCase()})
          </h2>
          
          {stats && (
            <div className="flex items-center gap-4 flex-wrap">
              <div>
                <p className="text-3xl font-bold text-gray-800">
                  {formatCurrency(stats.current)}
                </p>
              </div>
              
              <div className={`flex items-center gap-2 px-4 py-2 rounded-lg ${
                isPositiveChange ? 'bg-green-100' : 'bg-red-100'
              }`}>
                {isPositiveChange ? (
                  <TrendingUp size={20} color="#10b981" />
                ) : (
                  <TrendingDown size={20} color="#ef4444" />
                )}
                <span className={`font-bold ${
                  isPositiveChange ? 'text-green-700' : 'text-red-700'
                }`}>
                  {isPositiveChange ? '+' : ''}{stats.change.toFixed(2)}%
                </span>
                <span className="text-gray-600 text-sm">
                  ({selectedPeriod}d)
                </span>
              </div>
            </div>
          )}
        </div>

        {/* Controls */}
        <div className="flex gap-3 flex-wrap">
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

          {/* Chart Type Selector */}
          <div className="flex gap-2">
            <button
              onClick={() => setChartType('line')}
              className={`px-4 py-2 rounded-lg font-bold text-sm transition-all ${
                chartType === 'line'
                  ? 'bg-indigo-500 text-white shadow-lg'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              Linha
            </button>
            <button
              onClick={() => setChartType('area')}
              className={`px-4 py-2 rounded-lg font-bold text-sm transition-all ${
                chartType === 'area'
                  ? 'bg-indigo-500 text-white shadow-lg'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              Área
            </button>
          </div>
        </div>
      </div>

      {/* Stats Cards */}
      {stats && (
        <div className="grid grid-cols-3 gap-4 mb-6">
          <div className="bg-gradient-to-br from-blue-50 to-blue-100 p-4 rounded-lg">
            <p className="text-sm text-blue-700 font-semibold mb-1">Mínimo</p>
            <p className="text-xl font-bold text-blue-900">
              {formatCurrency(stats.min)}
            </p>
          </div>
          
          <div className="bg-gradient-to-br from-purple-50 to-purple-100 p-4 rounded-lg">
            <p className="text-sm text-purple-700 font-semibold mb-1">Máximo</p>
            <p className="text-xl font-bold text-purple-900">
              {formatCurrency(stats.max)}
            </p>
          </div>
          
          <div className="bg-gradient-to-br from-green-50 to-green-100 p-4 rounded-lg">
            <p className="text-sm text-green-700 font-semibold mb-1">Variação</p>
            <p className="text-xl font-bold text-green-900">
              {isPositiveChange ? '+' : ''}{stats.change.toFixed(2)}%
            </p>
          </div>
        </div>
      )}

      {/* Chart */}
      <div >
        <ResponsiveContainer>
          {chartType === 'line' ? (
            <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis 
                dataKey="date" 
                stroke="#666"
                tick={{ fontSize: 12 }}
                tickFormatter={(value) => {
                  if (selectedPeriod > 30) {
                    const parts = value.split('/');
                    return `${parts[0]}/${parts[1]}`;
                  }
                  return value;
                }}
              />
              <YAxis 
                stroke="#666"
                tick={{ fontSize: 12 }}
                tickFormatter={formatCurrency}
              />
              <Tooltip content={<CustomTooltip />} />
              <Legend />
              <Line
                type="monotone"
                dataKey="price"
                stroke="#667eea"
                strokeWidth={3}
                dot={false}
                name="Preço"
                activeDot={{ r: 6 }}
              />
            </LineChart>
          ) : (
            <AreaChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
              <defs>
                <linearGradient id="colorPrice" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#667eea" stopOpacity={0.8}/>
                  <stop offset="95%" stopColor="#667eea" stopOpacity={0.1}/>
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis 
                dataKey="date" 
                stroke="#666"
                tick={{ fontSize: 12 }}
              />
              <YAxis 
                stroke="#666"
                tick={{ fontSize: 12 }}
                tickFormatter={formatCurrency}
              />
              <Tooltip content={<CustomTooltip />} />
              <Legend />
              <Area
                type="monotone"
                dataKey="price"
                stroke="#667eea"
                strokeWidth={3}
                fillOpacity={1}
                fill="url(#colorPrice)"
                name="Preço"
              />
            </AreaChart>
          )}
        </ResponsiveContainer>
      </div>

      {error && (
        <div className="mt-4 p-4 bg-yellow-50 border-l-4 border-yellow-400 rounded">
          <p className="text-yellow-700 text-sm">
            ⚠️ Usando dados de demonstração. Configure o backend para dados reais.
          </p>
        </div>
      )}
    </div>
  );
}

export default PriceChart;