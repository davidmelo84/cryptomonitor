  // front/crypto-monitor-frontend/src/components/portfolio/PortfolioChart.jsx
  // ✅ VERSÃO CORRIGIDA - PieChart funcionando

  import React, { useMemo } from 'react';
  import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
  import '../../styles/portfolio.css';

  const COLORS = [
    '#667eea', '#10b981', '#ef4444', '#f59e0b',
    '#06b6d4', '#ec4899', '#8b5cf6', '#14b8a6'
  ];

  function PortfolioChart({ portfolio }) {
    // ✅ Formatador de moeda
    const formatCurrency = (value) => {
      return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
        notation: 'compact',
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
      }).format(value);
    };

    // ✅ Dados do gráfico memoizados
    const chartData = useMemo(() => {
      if (!Array.isArray(portfolio) || portfolio.length === 0) return [];
      
      const total = portfolio.reduce((sum, item) => sum + parseFloat(item.currentValue || 0), 0);
      
      return portfolio.map((item) => {
        const value = parseFloat(item.currentValue || 0);
        const percentage = total > 0 ? ((value / total) * 100).toFixed(2) : 0;
        
        return {
          name: item.coinSymbol || 'Unknown',
          value: value,
          percentage: percentage
        };
      }).sort((a, b) => b.value - a.value);
    }, [portfolio]);

    // ✅ Custom Tooltip
    const CustomTooltip = ({ active, payload }) => {
      if (!active || !payload || !payload.length) return null;

      const data = payload[0].payload;
      
      return (
        <div className="bg-white p-4 rounded-lg shadow-xl border-2 border-indigo-200">
          <p className="font-bold text-gray-800 mb-2">{data.name}</p>
          <p className="text-lg font-bold text-indigo-600">
            {formatCurrency(data.value)}
          </p>
          <p className="text-sm text-gray-600">
            {data.percentage}% do portfolio
          </p>
        </div>
      );
    };

    // ✅ Empty state
    if (chartData.length === 0) {
      return null;
    }

    return (
      <div className="portfolio-chart-container">
        <h3 className="portfolio-chart-header">
          📊 Distribuição do Portfolio
        </h3>

        <div className="portfolio-chart-grid">
          {/* PIE CHART */}
          <div >
            <ResponsiveContainer>
              <PieChart>
                <Pie
                  data={chartData}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ percentage }) => `${percentage}%`}
                  outerRadius={100}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {chartData.map((entry, index) => (
                    <Cell 
                      key={`cell-${index}`} 
                      fill={COLORS[index % COLORS.length]} 
                    />
                  ))}
                </Pie>
                <Tooltip content={<CustomTooltip />} />
              </PieChart>
            </ResponsiveContainer>
          </div>

          {/* LEGEND */}
          <div className="portfolio-chart-legend">
            {chartData.map((item, index) => (
              <LegendItem
                key={item.name}
                item={item}
                color={COLORS[index % COLORS.length]}
                formatCurrency={formatCurrency}
              />
            ))}
          </div>
        </div>
      </div>
    );
  }

  // ✅ Legend Item Component
  function LegendItem({ item, color, formatCurrency }) {
    return (
      <div className="portfolio-chart-legend-item">
        <div className="portfolio-chart-legend-left">
          <div
            className="portfolio-chart-legend-color"
            
          />
          <span className="portfolio-chart-legend-symbol">
            {item.name}
          </span>
        </div>

        <div className="portfolio-chart-legend-right">
          <p className="portfolio-chart-legend-value">
            {formatCurrency(item.value)}
          </p>
          <p className="portfolio-chart-legend-percent">
            {item.percentage}%
          </p>
        </div>
      </div>
    );
  }

  export default PortfolioChart;