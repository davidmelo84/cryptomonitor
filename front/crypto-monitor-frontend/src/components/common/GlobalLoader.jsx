import React from 'react';
import { useTheme } from '../../contexts/ThemeContext';

function GlobalLoader() {
  const { isDark } = useTheme();
  
  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      background: isDark ? '#111827' : '#f8f9fa',
      zIndex: 9999
    }}>
      <div style={{
        width: '60px',
        height: '60px',
        border: '4px solid rgba(102, 126, 234, 0.1)',
        borderTop: '4px solid #667eea',
        borderRadius: '50%',
        animation: 'spin 0.8s linear infinite'
      }} />
      <p style={{
        marginTop: '20px',
        color: isDark ? '#9ca3af' : '#6b7280',
        fontSize: '16px'
      }}>
        Carregando Crypto Monitor...
      </p>
      <style>{`
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
}

export default GlobalLoader;