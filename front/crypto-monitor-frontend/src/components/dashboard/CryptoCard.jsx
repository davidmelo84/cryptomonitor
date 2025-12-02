// front/crypto-monitor-frontend/src/components/dashboard/CryptoCard.jsx
import React, { memo } from 'react';
import { useTheme } from '../../contexts/ThemeContext';
import { formatCurrency } from '../../utils/formatters';
import '../../styles/crypto-card.css';

const CryptoCard = memo(
  ({
    crypto,
    isSelected,
    onToggle,
    compact = false,
    disabled = false,
  }) => {
    const { isDark } = useTheme();

    if (!crypto) return null;

    const isPriceUp = (crypto.priceChange24h || 0) >= 0;
    const marketCapFormatted = crypto.marketCap
      ? `$${(crypto.marketCap / 1000000000).toFixed(1)}B`
      : null;

    const cardClasses = [
      'crypto-card',
      isDark && 'dark',
      isSelected && 'selected',
      compact && 'compact',
      disabled && 'disabled',
    ]
      .filter(Boolean)
      .join(' ');

    const changeBadgeClasses = [
      'crypto-card-change-badge',
      isPriceUp ? 'positive' : 'negative',
    ]
      .filter(Boolean)
      .join(' ');

    return (
      <div
        onClick={!disabled ? onToggle : undefined}
        className={cardClasses}
        role="button"
        tabIndex={disabled ? -1 : 0}
        aria-pressed={isSelected}
        aria-label={`${crypto.name} - ${formatCurrency(
          crypto.currentPrice
        )} - ${isSelected ? 'Selecionado' : 'Não selecionado'}`}
        onKeyDown={(e) => {
          if (!disabled && (e.key === 'Enter' || e.key === ' ')) {
            e.preventDefault();
            onToggle();
          }
        }}
      >
        {/* Header */}
        <div className="crypto-card-header">
          <div className="crypto-card-info">
            <h3 className="crypto-card-name">{crypto.name}</h3>
            <p className="crypto-card-symbol">
              {(crypto.symbol || '').toUpperCase()}
            </p>
          </div>
          <div className="crypto-card-checkbox">
            <span className="crypto-card-checkbox-icon">✓</span>
          </div>
        </div>

        {/* Price */}
        <div className="crypto-card-price-section">
          <p className="crypto-card-price">
            {formatCurrency(crypto.currentPrice || 0)}
          </p>
        </div>

        {/* Footer */}
        <div className="crypto-card-footer">
          <div className={changeBadgeClasses}>
            <span className="crypto-card-change-arrow">
              {isPriceUp ? '▲' : '▼'}
            </span>
            <span className="crypto-card-change-value">
              {Math.abs(crypto.priceChange24h || 0).toFixed(2)}%
            </span>
          </div>
          {marketCapFormatted && (
            <div className="crypto-card-market-cap">{marketCapFormatted}</div>
          )}
        </div>
      </div>
    );
  },

  // ============================================================
  //   🔥 COMPARAÇÃO MAIS RÁPIDA E EFICIENTE
  //   Evita re-renders desnecessários
  // ============================================================
  (prevProps, nextProps) => {
    if (prevProps.isSelected !== nextProps.isSelected) return false;
    if (prevProps.disabled !== nextProps.disabled) return false;
    if (prevProps.compact !== nextProps.compact) return false;

    const prev = prevProps.crypto;
    const next = nextProps.crypto;

    if (!prev || !next) return false;

    if (prev.currentPrice !== next.currentPrice) return false;
    if (prev.priceChange24h !== next.priceChange24h) return false;
    if (prev.marketCap !== next.marketCap) return false;

    return true; // Não re-renderiza
  }
);

export default CryptoCard;
