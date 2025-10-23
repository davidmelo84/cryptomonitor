// ✅ VERSÃO CORRIGIDA - Com todas as informações visíveis
import React, { useState } from 'react';
import { useTheme } from '../../contexts/ThemeContext';

function CryptoCard({ crypto, isSelected, onToggle }) {
  const { isDark } = useTheme();
  const [isHovered, setIsHovered] = useState(false);
  const isPriceUp = (crypto.priceChange24h || 0) >= 0;

  const cardClasses = [
    'crypto-card',
    isDark && 'dark',
    isSelected && 'selected'
  ].filter(Boolean).join(' ');

  return (
    <div
      onClick={onToggle}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      className={cardClasses}
      style={{
        transform: !isSelected && isHovered ? 'translateY(-5px)' : 'translateY(0)'
      }}
    >
      {/* Header com nome e símbolo */}
      <div className="flex justify-between items-start mb-4">
        <div className="flex-1">
          <h3 className="m-0 text-lg font-bold" style={{ color: isDark ? '#f3f4f6' : '#1f2937' }}>
            {crypto.name}
          </h3>
          <p className="mt-1 mb-0 text-xs font-semibold" style={{ color: isDark ? '#9ca3af' : '#6b7280' }}>
            {(crypto.symbol || '').toUpperCase()}
          </p>
        </div>

        {/* Checkbox de seleção */}
        <div
          style={{
            width: '28px',
            height: '28px',
            borderRadius: '8px',
            border: `2px solid ${isSelected ? '#667eea' : (isDark ? '#4b5563' : '#d1d5db')}`,
            background: isSelected ? '#667eea' : (isDark ? '#374151' : 'white'),
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'white',
            fontWeight: 'bold',
            fontSize: '16px',
            flexShrink: 0,
            transition: 'all 0.3s'
          }}
        >
          {isSelected && '✓'}
        </div>
      </div>
      
      {/* Preço atual */}
      <div className="mb-3">
        <p className="m-0 text-3xl font-bold" style={{ color: isDark ? '#f3f4f6' : '#1f2937' }}>
          ${(crypto.currentPrice || 0).toLocaleString('en-US', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
          })}
        </p>
      </div>
      
      {/* Footer com variação e market cap */}
      <div className="flex justify-between items-center">
        <div
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            padding: '6px 12px',
            borderRadius: '8px',
            fontSize: '14px',
            fontWeight: 'bold',
            background: isPriceUp ? '#dcfce7' : '#fee2e2',
            color: isPriceUp ? '#166534' : '#991b1b'
          }}
        >
          <span style={{ fontSize: '16px' }}>{isPriceUp ? '▲' : '▼'}</span>
          <span>{Math.abs(crypto.priceChange24h || 0).toFixed(2)}%</span>
        </div>
        
        {crypto.marketCap && (
          <div style={{
            fontSize: '12px',
            fontWeight: '600',
            color: isDark ? '#6b7280' : '#9ca3af'
          }}>
            ${(crypto.marketCap / 1000000000).toFixed(1)}B
          </div>
        )}
      </div>
    </div>
  );
}

export default CryptoCard;