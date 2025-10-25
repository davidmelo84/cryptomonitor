// front/crypto-monitor-frontend/src/components/portfolio/PortfolioChart.jsx
import React, { useMemo } from 'react';
import {
  LineChart,
  Line,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer
} from 'recharts';

function PortfolioChart({ portfolio }) {
  // ✅ Normaliza os dados para garantir que não haja undefined ou null
  const chartData = useMemo(() => {
    if (!Array.isArray(portfolio)) return [];
    return portfolio.map(item => ({
      name: item.name || 'Desconhecido',
      currentValue: item.currentValue != null ? item.currentValue : 0,
      profitLoss: item.profitLoss != null ? item.profitLoss : 0
    }));
  }, [portfolio]);

  if (chartData.length === 0) {
    return (
      <div className="text-center text-gray-500 my-6">
        Nenhum dado disponível para exibir o gráfico
      </div>
    );
  }

  return (
    <div className="w-full h-96 mb-8">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={chartData}>
          <CartesianGrid stroke="#e0e0e0" strokeDasharray="5 5" />
          <XAxis dataKey="name" />
          <YAxis />
          <Tooltip
            formatter={(value) =>
              new Intl.NumberFormat('en-US', {
                style: 'currency',
                currency: 'USD'
              }).format(value)
            }
          />
          <Line
            type="monotone"
            dataKey="currentValue"
            stroke="#667eea"
            strokeWidth={2}
            dot={{ r: 3 }}
            activeDot={{ r: 5 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}

export default PortfolioChart;
