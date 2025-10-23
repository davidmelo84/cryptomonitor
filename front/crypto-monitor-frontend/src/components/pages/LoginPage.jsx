import React, { useState } from 'react';
import { TrendingUp, Eye, EyeOff, AlertCircle } from 'lucide-react';
import { useTheme } from '../../contexts/ThemeContext';
import ThemeToggle from '../common/ThemeToggle';

function LoginPage({ authError, onLogin, onNavigateToRegister }) {
  const { isDark } = useTheme();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const handleSubmit = () => onLogin(username, password);

  return (
    <div className={`auth-container ${isDark ? 'dark' : ''}`}>
      {/* Theme Toggle */}
      <div className="theme-toggle-wrapper">
        <ThemeToggle />
      </div>

      {/* Floating Circles */}
      <div className="floating-circle large" />
      <div className="floating-circle small" />
      
      <div className={`auth-card ${isDark ? 'dark' : ''}`}>
        {/* Logo */}
        <div className="auth-logo-wrapper">
          <div className="auth-logo">
            <TrendingUp size={40} color="white" />
          </div>
        </div>
        
        <h1 className="auth-title">Crypto Monitor</h1>
        <p className="auth-subtitle">Sistema de Monitoramento Inteligente</p>
        
        {/* Username Input */}
        <div className="auth-input-wrapper">
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="auth-input"
            placeholder="Usuário"
          />
        </div>
        
        {/* Password Input */}
        <div className="auth-input-wrapper">
          <input
            type={showPassword ? 'text' : 'password'}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
            className="auth-input"
            placeholder="Senha"
          />
          <button 
            className="auth-icon" 
            onClick={() => setShowPassword(!showPassword)}
          >
            {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
          </button>
        </div>
        
        {/* Error Alert */}
        {authError && (
          <div className="alert alert-error">
            <AlertCircle size={20} />
            {authError}
          </div>
        )}
        
        {/* Submit Button */}
        <button onClick={handleSubmit} className="auth-button">
          Entrar
        </button>
        
        {/* Register Link */}
        <div className="auth-link" onClick={onNavigateToRegister}>
          Não tem conta? <span>Cadastre-se</span>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;