// front/crypto-monitor-frontend/src/components/common/Toast.jsx
// ✅ COPIE ESTE ARQUIVO COMPLETO

import React, { useState, useEffect } from 'react';
import { CheckCircle, AlertCircle, Info, X } from 'lucide-react';
import { useTheme } from '../../contexts/ThemeContext';

const Toast = ({ message, type = 'info', duration = 3000, onClose }) => {
  const { isDark } = useTheme();
  const [isVisible, setIsVisible] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setIsVisible(false);
      setTimeout(onClose, 300);
    }, duration);

    return () => clearTimeout(timer);
  }, [duration, onClose]);

  const icons = {
    success: <CheckCircle size={20} />,
    error: <AlertCircle size={20} />,
    info: <Info size={20} />
  };

  const colors = {
    success: {
      bg: isDark ? '#064e3b' : '#d1fae5',
      text: isDark ? '#6ee7b7' : '#065f46',
      border: isDark ? '#059669' : '#10b981'
    },
    error: {
      bg: isDark ? '#7f1d1d' : '#fee2e2',
      text: isDark ? '#fca5a5' : '#991b1b',
      border: isDark ? '#dc2626' : '#ef4444'
    },
    info: {
      bg: isDark ? '#1e3a8a' : '#dbeafe',
      text: isDark ? '#93c5fd' : '#1e40af',
      border: isDark ? '#3b82f6' : '#60a5fa'
    }
  };

  const style = colors[type];

  return (
    <div
      style={{
        position: 'fixed',
        bottom: '20px',
        right: '20px',
        zIndex: 9999,
        minWidth: '300px',
        maxWidth: '400px',
        background: style.bg,
        color: style.text,
        borderLeft: `4px solid ${style.border}`,
        borderRadius: '8px',
        padding: '16px',
        boxShadow: '0 10px 30px rgba(0, 0, 0, 0.3)',
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        animation: isVisible ? 'slideInRight 0.3s ease' : 'slideOutRight 0.3s ease',
        transform: isVisible ? 'translateX(0)' : 'translateX(400px)',
        transition: 'all 0.3s ease'
      }}
    >
      <div style={{ flexShrink: 0 }}>
        {icons[type]}
      </div>
      
      <div style={{ flex: 1, fontSize: '14px', fontWeight: '500' }}>
        {message}
      </div>
      
      <button
        onClick={() => {
          setIsVisible(false);
          setTimeout(onClose, 300);
        }}
        style={{
          background: 'transparent',
          border: 'none',
          color: style.text,
          cursor: 'pointer',
          padding: '4px',
          display: 'flex',
          alignItems: 'center',
          opacity: 0.7,
          transition: 'opacity 0.2s'
        }}
        onMouseEnter={(e) => e.currentTarget.style.opacity = '1'}
        onMouseLeave={(e) => e.currentTarget.style.opacity = '0.7'}
      >
        <X size={18} />
      </button>

      <style>{`
        @keyframes slideInRight {
          from {
            transform: translateX(400px);
            opacity: 0;
          }
          to {
            transform: translateX(0);
            opacity: 1;
          }
        }
        
        @keyframes slideOutRight {
          from {
            transform: translateX(0);
            opacity: 1;
          }
          to {
            transform: translateX(400px);
            opacity: 0;
          }
        }
      `}</style>
    </div>
  );
};

// ✅ Hook customizado para usar Toast
export const useToast = () => {
  const [toasts, setToasts] = React.useState([]);

  const showToast = (message, type = 'info', duration = 3000) => {
    const id = Date.now();
    setToasts(prev => [...prev, { id, message, type, duration }]);
  };

  const removeToast = (id) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  };

  const ToastContainer = () => (
    <>
      {toasts.map((toast, index) => (
        <div
          key={toast.id}
          style={{
            position: 'fixed',
            bottom: `${20 + (index * 80)}px`,
            right: '20px',
            zIndex: 9999 - index
          }}
        >
          <Toast
            message={toast.message}
            type={toast.type}
            duration={toast.duration}
            onClose={() => removeToast(toast.id)}
          />
        </div>
      ))}
    </>
  );

  return { showToast, ToastContainer };
};

export default Toast;