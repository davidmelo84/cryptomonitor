// front/crypto-monitor-frontend/src/components/auth/PasswordStrength.jsx
// ✅ REFATORADO - SEM CSS INLINE

import React from 'react';
import '../../styles/components.css';

function PasswordStrength({ password }) {
  // Calcula a força da senha
  const getStrength = () => {
    if (password.length < 6) {
      return { 
        label: 'Fraca', 
        level: 'weak'
      };
    }
    if (password.length < 10) {
      return { 
        label: 'Média', 
        level: 'medium'
      };
    }
    return { 
      label: 'Forte', 
      level: 'strong'
    };
  };

  const strength = getStrength();

  return (
    <div className="password-strength">
      {/* Label com indicador de força */}
      <div className="password-strength-label">
        <span className="password-strength-label-text">
          Força da senha:
        </span>
        <span className={`password-strength-value ${strength.level}`}>
          {strength.label}
        </span>
      </div>

      {/* Barra de progresso */}
      <div className="password-strength-bar">
        <div className={`password-strength-fill ${strength.level}`} />
      </div>
    </div>
  );
}

export default PasswordStrength;