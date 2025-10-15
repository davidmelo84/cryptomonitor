import React from 'react';
import { BarChart3, TrendingUp, Bell } from 'lucide-react';
import StatCard from './StatCard';

function StatsCards({ selectedCryptos, isMonitoring }) {
  const averageChange = selectedCryptos.length > 0
    ? selectedCryptos.reduce((sum, crypto) => sum + (crypto.priceChange24h || 0), 0) / selectedCryptos.length
    : 0;

  return (
    <div className="grid grid-cols-[repeat(auto-fit,minmax(200px,1fr))] gap-5 mb-8">
      <StatCard
        icon={<BarChart3 size={24} color="#667eea" />}
        label="Selecionadas"
        value={selectedCryptos.length}
      />
      
      <StatCard
        icon={<TrendingUp size={24} color="#667eea" />}
        label="Variação Média"
        value={`${averageChange >= 0 ? '+' : ''}${averageChange.toFixed(2)}%`}
        valueColor={averageChange >= 0 ? '#10b981' : '#ef4444'}
      />
      
      <StatCard
        icon={<Bell size={24} color="#667eea" />}
        label="Alertas Ativos"
        value={isMonitoring ? selectedCryptos.length * 2 : 0}
      />
    </div>
  );
}

export default StatsCards;