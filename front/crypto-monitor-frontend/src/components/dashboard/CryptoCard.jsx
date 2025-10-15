import React, { useState } from 'react';

function CryptoCard({ crypto, isSelected, onToggle }) {
  const [isHovered, setIsHovered] = useState(false);
  const isPriceUp = (crypto.priceChange24h || 0) >= 0;

  return (
    <div
      onClick={onToggle}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      className={`p-5 rounded-[15px] cursor-pointer transition-all duration-300 select-none ${
        isSelected
          ? 'border-4 border-indigo-500 bg-gradient-to-br from-indigo-50 to-indigo-100 shadow-lg'
          : 'border-2 border-gray-200 bg-white shadow-sm'
      }`}
      style={{
        transform: !isSelected && isHovered ? 'translateY(-5px)' : 'translateY(0)',
        boxShadow: isSelected
          ? '0 8px 20px rgba(102, 126, 234, 0.2)'
          : isHovered
          ? '0 8px 20px rgba(0,0,0,0.1)'
          : '0 2px 8px rgba(0,0,0,0.05)'
      }}
    >
      <div className="flex justify-between items-start mb-4">
        <div className="flex-1">
          <h3 className="m-0 text-lg font-bold text-gray-800">{crypto.name}</h3>
          <p className="mt-1 mb-0 text-gray-600 text-xs font-semibold">
            {(crypto.symbol || '').toUpperCase()}
          </p>
        </div>
        <div
          className={`w-7 h-7 rounded-lg border-2 flex items-center justify-center text-white font-bold text-base flex-shrink-0 ${
            isSelected ? 'border-indigo-500 bg-indigo-500' : 'border-gray-300 bg-white'
          }`}
        >
          {isSelected && '✓'}
        </div>
      </div>
      
      <div className="mb-3">
        <p className="m-0 text-3xl font-bold text-gray-800">
          ${(crypto.currentPrice || 0).toLocaleString('en-US', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
          })}
        </p>
      </div>
      
      <div className="flex justify-between items-center">
        <div
          className={`inline-flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-bold ${
            isPriceUp
              ? 'bg-green-100 text-green-800'
              : 'bg-red-100 text-red-800'
          }`}
        >
          <span className="text-base">{isPriceUp ? '▲' : '▼'}</span>
          <span>{Math.abs(crypto.priceChange24h || 0).toFixed(2)}%</span>
        </div>
        
        {crypto.marketCap && (
          <div className="text-xs text-gray-400 font-semibold">
            ${(crypto.marketCap / 1000000000).toFixed(1)}B
          </div>
        )}
      </div>
    </div>
  );
}

export default CryptoCard;