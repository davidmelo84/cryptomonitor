// front/crypto-monitor-frontend/src/components/common/ThemeToggle.jsx
// ✅ REFATORADO - SEM ESTILOS INLINE

import React from 'react';
import { Moon, Sun } from 'lucide-react';
import { useTheme } from '../../contexts/ThemeContext';
import '../../styles/theme-toggle.css';

function ThemeToggle({ 
  className = '', 
  showTooltip = true,
  floating = false 
}) {
  const { isDark, toggleTheme } = useTheme();
  
  // Combinar classes
  const buttonClasses = [
    'theme-toggle-button',
    isDark ? 'dark-mode' : 'light-mode',
    floating && 'theme-toggle-floating',
    className
  ].filter(Boolean).join(' ');

  return (
    <button
      onClick={toggleTheme}
      className={buttonClasses}
      data-tooltip={showTooltip ? (isDark ? 'Ativar modo claro' : 'Ativar modo escuro') : undefined}
      aria-label={isDark ? 'Ativar modo claro' : 'Ativar modo escuro'}
      title={isDark ? 'Ativar modo claro' : 'Ativar modo escuro'}
    >
      <span className="theme-toggle-icon">
        {isDark ? <Sun size={20} /> : <Moon size={20} />}
      </span>
    </button>
  );
}

export default ThemeToggle;