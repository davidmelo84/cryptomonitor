// front/crypto-monitor-frontend/src/components/portfolio/PortfolioChart.jsx

import React from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts';

function PortfolioChart({ portfolio }) {
  const COLORS = [
    '#667eea', '#10b981', '#ef4444', '#f59e0b', 
    '#06b6d4', '#ec4899', '#8b5cf6', '#14b8a6'
  ];

  const chartData = portfolio.map((item) => ({
    name: item.coinSymbol,
    value: parseFloat(item.currentValue),
    percentage: 0 // será calculado abaixo
  }));

  // Calcular percentagens
  const total = chartData.reduce((sum, item) => sum + item.value, 0);
  chartData.forEach(item => {
    item.percentage = ((item.value / total) * 100).toFixed(2);
  });

  const formatCurrency = (value) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      notation: 'compact',
      minimumFractionDigits: 0,
      maximumFractionDigits: 2
    }).format(value);
  };

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

  return (
    <div className="bg-white rounded-xl shadow-md p-6 mb-8">
      <h3 className="text-xl font-bold text-gray-800 mb-6 flex items-center gap-2">
        📊 Distribuição do Portfolio
      </h3>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Chart */}
        <div style={{ width: '100%', height: '300px' }}>
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
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip content={<CustomTooltip />} />
            </PieChart>
          </ResponsiveContainer>
        </div>

        {/* Legend */}
        <div className="flex flex-col justify-center">
          <div className="space-y-3">
            {chartData.map((item, index) => (
              <div 
                key={item.name}
                className="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
              >
                <div className="flex items-center gap-3">
                  <div
                    className="w-4 h-4 rounded-full"
                    style={{ backgroundColor: COLORS[index % COLORS.length] }}
                  />
                  <span className="font-bold text-gray-800">{item.name}</span>
                </div>
                <div className="text-right">
                  <p className="font-bold text-indigo-600">{formatCurrency(item.value)}</p>
                  <p className="text-sm text-gray-500">{item.percentage}%</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

export default PortfolioChart;