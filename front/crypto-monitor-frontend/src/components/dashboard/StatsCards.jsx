import React from 'react';
import { BarChart3, TrendingUp, Bell } from 'lucide-react';
import { useTheme } from '../../contexts/ThemeContext';
import '../../styles/components/dashboard.css';


function StatsCards({ selectedCryptos, isMonitoring }) {
  const { isDark } = useTheme();
  
  const averageChange = selectedCryptos.length > 0
    ? selectedCryptos.reduce((sum, crypto) => sum + (crypto.priceChange24h || 0), 0) / selectedCryptos.length
    : 0;

  const isPositive = averageChange >= 0;

  return (
    <div className="stats-grid">
      {/* Selecionadas */}
      <div className={`stat-card ${isDark ? 'dark' : ''}`}>
        <div className="stat-icon">
          <BarChart3 size={24} color="#667eea" />
          <span className="stat-label">Selecionadas</span>
        </div>
        <p className="stat-value">{selectedCryptos.length}</p>
      </div>
      
      {/* Variação Média */}
      <div className={`stat-card ${isDark ? 'dark' : ''}`}>
        <div className="stat-icon">
          <TrendingUp size={24} color="#667eea" />
          <span className="stat-label">Variação Média</span>
        </div>
        <p className={`stat-value ${isPositive ? 'positive' : 'negative'}`}>
          {averageChange >= 0 ? '+' : ''}{averageChange.toFixed(2)}%
        </p>
      </div>
      
      {/* Alertas Ativos */}
      <div className={`stat-card ${isDark ? 'dark' : ''}`}>
        <div className="stat-icon">
          <Bell size={24} color="#667eea" />
          <span className="stat-label">Alertas Ativos</span>
        </div>
        <p className="stat-value">{isMonitoring ? selectedCryptos.length * 2 : 0}</p>
      </div>
    </div>
  );
}

export default StatsCards;