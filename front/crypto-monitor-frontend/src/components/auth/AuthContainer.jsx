// front/crypto-monitor-frontend/src/components/auth/AuthContainer.jsx
// ✅ REFATORADO - SEM CSS INLINE

import React from 'react';
import '../../styles/components.css';

function AuthContainer({ children, variant = 'login' }) {
  // Configurações dos círculos flutuantes baseado na variante
  const floatingCircles = variant === 'login' ? [
    { className: 'floating-circle large' },
    { className: 'floating-circle small' }
  ] : [
    { className: 'floating-circle medium' }
  ];

  return (
    <div className="auth-container">
      {/* Círculos flutuantes decorativos */}
      {floatingCircles.map((circle, index) => (
        <div
          key={index}
          className={circle.className}
        />
      ))}
      
      {/* Card de autenticação */}
      <div className="auth-card">
        {children}
      </div>
    </div>
  );
}

export default AuthContainer;