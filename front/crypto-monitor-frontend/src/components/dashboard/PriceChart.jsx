// front/crypto-monitor-frontend/src/components/dashboard/PriceChart.jsx
// ✅ VERSÃO FINAL - SEM MOCK, APENAS DADOS REAIS

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

  const fetchChartData = useCallback(async () => {
    setLoading(true);
    setError(null);
    
    try {
      console.log(`📊 Buscando histórico de ${coinId} (${selectedPeriod}d)`);
      
      const response = await fetch(
        `${API_BASE_URL}/crypto/history/${coinId}?days=${selectedPeriod}`
      );
      
      if (!response.ok) {
        throw new Error(`Erro HTTP: ${response.status}`);
      }
      
      const data = await response.json();
      
      console.log('✅ Dados recebidos:', data);
      
      // ✅ CRÍTICO: Validar estrutura dos dados
      if (!data.data || !Array.isArray(data.data) || data.data.length === 0) {
        throw new Error('Dados vazios ou formato inválido');
      }
      
      setChartData(data.data);
      setStats(data.stats);
      
    } catch (err) {
      console.error('❌ Erro ao buscar histórico:', err);
      setError(err.message);
      setChartData([]);
      setStats(null);
    } finally {
      setLoading(false);
    }
  }, [coinId, selectedPeriod]);

  useEffect(() => {
    if (coinId) {
      fetchChartData();
    }
  }, [coinId, selectedPeriod, fetchChartData]);

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

  // ✅ LOADING STATE
  if (loading) {
    return (
      <div className="bg-white p-8 rounded-[20px] shadow-md flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <Activity className="animate-spin mx-auto mb-4" size={40} color="#667eea" />
          <p className="text-gray-600">Carregando histórico de {coinName}...</p>
        </div>
      </div>
    );
  }

  // ✅ ERROR STATE - SEM FALLBACK PARA MOCK
  if (error || chartData.length === 0) {
    return (
      <div className="bg-white p-8 rounded-[20px] shadow-md">
        <div className="text-center py-12">
          <Activity size={48} className="mx-auto mb-4 text-red-400" />
          <h3 className="text-xl font-bold text-gray-800 mb-2">
            Erro ao Carregar Histórico
          </h3>
          <p className="text-gray-600 mb-4">
            {error || 'Não foi possível obter dados históricos desta criptomoeda.'}
          </p>
          <p className="text-sm text-gray-500 mb-4">
            CoinId: <code className="bg-gray-100 px-2 py-1 rounded">{coinId}</code>
          </p>
          <button
            onClick={fetchChartData}
            className="px-6 py-3 bg-indigo-500 text-white rounded-lg font-bold hover:bg-indigo-600 transition-all"
          >
            🔄 Tentar Novamente
          </button>
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
      <div style={{ width: '100%', height: '400px' }}>
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

      {/* Debug Info */}
      <div className="mt-4 p-3 bg-gray-50 rounded-lg text-xs text-gray-600">
        <p>📊 {chartData.length} pontos de dados • Período: {selectedPeriod} dias</p>
        <p>🔄 Última atualização: {new Date().toLocaleTimeString('pt-BR')}</p>
      </div>
    </div>
  );
}

export default PriceChart;