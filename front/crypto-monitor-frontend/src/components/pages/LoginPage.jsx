// front/crypto-monitor-frontend/src/components/pages/LoginPage.jsx
// ✅ VERSÃO CORRIGIDA - Com visual original do projeto

import React, { useState } from 'react';
import { LogIn, TrendingUp, Mail, Lock } from 'lucide-react';
import { useTheme } from '../../contexts/ThemeContext';
import ThemeToggle from '../common/ThemeToggle';
import '../../styles/components.css';

function LoginPage({ onLogin, onNavigateToRegister, authError }) {
  const { isDark } = useTheme();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    await onLogin(username, password);
    setIsLoading(false);
  };

  return (
    <div className={`auth-container ${isDark ? 'dark' : ''}`}>
      {/* Theme Toggle */}
      <div className="theme-toggle-wrapper">
        <ThemeToggle />
      </div>

      {/* Círculos flutuantes */}
      <div className="floating-circle large"></div>
      <div className="floating-circle medium"></div>
      <div className="floating-circle small"></div>

      {/* Card de Login */}
      <div className={`auth-card ${isDark ? 'dark' : ''}`}>
        {/* Logo */}
        <div className="auth-logo-wrapper">
          <div className="auth-logo">
            <TrendingUp size={40} color="white" />
          </div>
        </div>

        {/* Título */}
        <h1 className="auth-title">Crypto Monitor</h1>
        <p className="auth-subtitle">Monitore suas criptomoedas em tempo real</p>

        {/* Erro */}
        {authError && (
          <div className="alert alert-error">
            ⚠️ {authError}
          </div>
        )}

        {/* Formulário */}
        <form onSubmit={handleSubmit}>
          {/* Usuário */}
          <div className="auth-input-wrapper">
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Digite seu usuário"
              className="auth-input"
              disabled={isLoading}
              required
            />
            <button type="button" className="auth-icon" tabIndex="-1">
              <Mail size={20} />
            </button>
          </div>

          {/* Senha */}
          <div className="auth-input-wrapper">
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Digite sua senha"
              className="auth-input"
              disabled={isLoading}
              required
            />
            <button type="button" className="auth-icon" tabIndex="-1">
              <Lock size={20} />
            </button>
          </div>

          {/* Botão Entrar */}
          <button
            type="submit"
            className="auth-button"
            disabled={isLoading}
          >
            {isLoading ? (
              <>
                <div className="spinner" style={{ width: '20px', height: '20px' }}></div>
                Entrando...
              </>
            ) : (
              <>
                <LogIn size={20} />
                Entrar
              </>
            )}
          </button>
        </form>

        {/* Link para Registro */}
        <div className="auth-link" onClick={onNavigateToRegister}>
          Não tem uma conta? <span>Cadastre-se agora</span>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;