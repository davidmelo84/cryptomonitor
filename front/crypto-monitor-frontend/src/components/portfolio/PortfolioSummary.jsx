// front/crypto-monitor-frontend/src/components/portfolio/PortfolioSummary.jsx
import React, { useMemo } from 'react';
import { DollarSign, TrendingUp, TrendingDown, Percent } from 'lucide-react';

function PortfolioSummary({ portfolio }) {
  // Memoiza os cálculos do resumo do portfólio
  const summary = useMemo(() => {
    const totalInvested = portfolio.reduce(
      (sum, item) => sum + parseFloat(item.totalInvested || 0), 0
    );
    const totalCurrentValue = portfolio.reduce(
      (sum, item) => sum + parseFloat(item.currentValue || 0), 0
    );
    const totalProfitLoss = totalCurrentValue - totalInvested;
    const totalProfitLossPercent = totalInvested > 0
      ? ((totalProfitLoss / totalInvested) * 100).toFixed(2)
      : 0;

    return { totalInvested, totalCurrentValue, totalProfitLoss, totalProfitLossPercent };
  }, [portfolio]);

  // Funções de formatação
  const formatCurrency = (value) =>
    new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(value);

  const formatPercent = (value) => parseFloat(value).toFixed(2);

  const isPositive = summary.totalProfitLoss >= 0;

  // Configuração dos cards
  const cards = [
    {
      icon: <DollarSign size={24} color="#667eea" />,
      label: 'Total Investido',
      value: formatCurrency(summary.totalInvested),
      color: 'text-gray-800'
    },
    {
      icon: <TrendingUp size={24} color="#667eea" />,
      label: 'Valor Atual',
      value: formatCurrency(summary.totalCurrentValue),
      color: 'text-gray-800'
    },
    {
      icon: isPositive
        ? <TrendingUp size={24} color="#10b981" />
        : <TrendingDown size={24} color="#ef4444" />,
      label: 'Lucro/Prejuízo',
      value: (isPositive ? '+' : '') + formatCurrency(summary.totalProfitLoss),
      color: isPositive ? 'text-green-600' : 'text-red-600'
    },
    {
      icon: <Percent size={24} color="#667eea" />,
      label: 'Rentabilidade',
      value: (isPositive ? '+' : '') + formatPercent(summary.totalProfitLossPercent) + '%',
      color: isPositive ? 'text-green-600' : 'text-red-600'
    }
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
      {cards.map((card, index) => (
        <div key={index} className="bg-white p-6 rounded-xl shadow-md">
          <div className="flex items-center gap-3 mb-3">
            {card.icon}
            <span className="text-gray-600 font-semibold">{card.label}</span>
          </div>
          <p className={`text-3xl font-bold ${card.color}`}>
            {card.value}
          </p>
        </div>
      ))}
    </div>
  );
}

export default PortfolioSummary;
