// front/crypto-monitor-frontend/src/components/common/Skeleton.jsx
// ✅ COPIE ESTE ARQUIVO COMPLETO

import React from 'react';
import { useTheme } from '../../contexts/ThemeContext';

const Skeleton = ({ 
  width = '100%', 
  height = '20px', 
  borderRadius = '8px',
  className = '',
  count = 1,
  spacing = '12px'
}) => {
  const { isDark } = useTheme();

  const baseStyle = {
    width,
    height,
    borderRadius,
    background: isDark 
      ? 'linear-gradient(90deg, #1f2937 0%, #374151 50%, #1f2937 100%)'
      : 'linear-gradient(90deg, #f3f4f6 0%, #e5e7eb 50%, #f3f4f6 100%)',
    backgroundSize: '200% 100%',
    animation: 'shimmer 1.5s infinite',
    display: 'block'
  };

  if (count === 1) {
    return (
      <>
        <div style={baseStyle} className={className} />
        <style>{`
          @keyframes shimmer {
            0% { background-position: 200% 0; }
            100% { background-position: -200% 0; }
          }
        `}</style>
      </>
    );
  }

  return (
    <>
      {Array.from({ length: count }).map((_, i) => (
        <div 
          key={i} 
          style={{ 
            ...baseStyle, 
            marginBottom: i < count - 1 ? spacing : 0 
          }} 
          className={className}
        />
      ))}
      <style>{`
        @keyframes shimmer {
          0% { background-position: 200% 0; }
          100% { background-position: -200% 0; }
        }
      `}</style>
    </>
  );
};

// ✅ Skeleton para CryptoCard
export const CryptoCardSkeleton = () => {
  const { isDark } = useTheme();
  
  return (
    <div 
      style={{
        padding: '20px',
        borderRadius: '15px',
        border: `2px solid ${isDark ? '#374151' : '#e5e7eb'}`,
        background: isDark ? '#1f2937' : 'white',
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.05)'
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '16px' }}>
        <div style={{ flex: 1 }}>
          <Skeleton width="60%" height="20px" />
          <div style={{ marginTop: '8px' }}>
            <Skeleton width="40%" height="14px" />
          </div>
        </div>
        <Skeleton width="28px" height="28px" borderRadius="8px" />
      </div>
      
      <Skeleton width="80%" height="32px" />
      
      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '16px' }}>
        <Skeleton width="30%" height="28px" borderRadius="8px" />
        <Skeleton width="25%" height="14px" />
      </div>
    </div>
  );
};

// ✅ Skeleton para StatCard
export const StatCardSkeleton = () => {
  const { isDark } = useTheme();
  
  return (
    <div 
      style={{
        background: isDark ? '#1f2937' : 'white',
        padding: '20px',
        borderRadius: '15px',
        boxShadow: '0 4px 15px rgba(0, 0, 0, 0.1)'
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '12px' }}>
        <Skeleton width="40px" height="40px" borderRadius="50%" />
        <Skeleton width="120px" height="14px" />
      </div>
      <Skeleton width="70%" height="32px" />
    </div>
  );
};

// ✅ Skeleton para Gráfico
export const ChartSkeleton = () => {
  const { isDark } = useTheme();
  
  return (
    <div 
      style={{
        background: isDark ? '#1f2937' : 'white',
        padding: '32px',
        borderRadius: '20px',
        boxShadow: '0 4px 15px rgba(0, 0, 0, 0.1)'
      }}
    >
      <div style={{ marginBottom: '24px' }}>
        <Skeleton width="200px" height="28px" />
        <div style={{ marginTop: '12px' }}>
          <Skeleton width="150px" height="20px" />
        </div>
      </div>
      
      <Skeleton width="100%" height="400px" borderRadius="12px" />
    </div>
  );
};

export default Skeleton;