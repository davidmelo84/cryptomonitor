import React from 'react';

function PasswordStrength({ password }) {
  const getStrength = () => {
    if (password.length < 6) return { label: 'Fraca', color: '#f00', width: '33%' };
    if (password.length < 10) return { label: 'Média', color: '#fa0', width: '66%' };
    return { label: 'Forte', color: '#0f0', width: '100%' };
  };

  const strength = getStrength();

  return (
    <div className="mb-4 text-xs">
      <span>Força da senha: </span>
      <span className="font-bold" style={{ color: strength.color }}>
        {strength.label}
      </span>
      <div className="h-1 bg-gray-200 rounded-sm mt-1">
        <div
          className="h-full rounded-sm transition-all duration-300"
          style={{ width: strength.width, backgroundColor: strength.color }}
        />
      </div>
    </div>
  );
}

export default PasswordStrength;