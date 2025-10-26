// front/crypto-monitor-frontend/src/components/pages/RegisterPage.jsx
// ✅ VERSÃO CORRIGIDA - Com Dark Mode e Theme Toggle

import React, { useState } from 'react';
import { UserPlus, Mail, Lock, User, TrendingUp, ArrowLeft } from 'lucide-react';
import { useTheme } from '../../contexts/ThemeContext';
import ThemeToggle from '../common/ThemeToggle';

function RegisterPage({ onRegister, onNavigateToLogin, authError }) {
  const { isDark } = useTheme();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setSuccessMessage('');

    const success = await onRegister(username, email, password, confirmPassword);

    if (success) {
      setSuccessMessage('Conta criada com sucesso! Redirecionando...');
      setTimeout(() => {
        onNavigateToLogin();
      }, 2000);
    }

    setIsLoading(false);
  };

  return (
    <div className={`auth-page ${isDark ? 'dark' : ''}`}>
      {/* Theme Toggle no canto superior direito */}
      <div className="auth-theme-toggle">
        <ThemeToggle />
      </div>

      <div className="auth-container">
        {/* Logo Section */}
        <div className="auth-logo-section">
          <div className="auth-logo">
            <TrendingUp size={48} />
          </div>
          <h1 className="auth-logo-title">Crypto Monitor</h1>
          <p className="auth-logo-subtitle">
            Crie sua conta e comece a monitorar
          </p>
        </div>

        {/* Register Form */}
        <div className="auth-card">
          <div className="auth-card-header">
            <UserPlus size={32} className="auth-card-icon" />
            <h2 className="auth-card-title">Criar nova conta</h2>
            <p className="auth-card-subtitle">Preencha os dados abaixo</p>
          </div>

          {authError && (
            <div className="auth-error">
              <span>⚠️</span>
              {authError}
            </div>
          )}

          {successMessage && (
            <div className="auth-success">
              <span>✓</span>
              {successMessage}
            </div>
          )}

          <form onSubmit={handleSubmit} className="auth-form">
            <div className="auth-input-group">
              <label htmlFor="username" className="auth-label">
                <User size={18} />
                Usuário
              </label>
              <input
                id="username"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Escolha um nome de usuário"
                className="auth-input"
                disabled={isLoading}
                required
              />
            </div>

            <div className="auth-input-group">
              <label htmlFor="email" className="auth-label">
                <Mail size={18} />
                Email
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="seu@email.com"
                className="auth-input"
                disabled={isLoading}
                required
              />
            </div>

            <div className="auth-input-group">
              <label htmlFor="password" className="auth-label">
                <Lock size={18} />
                Senha
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Mínimo 6 caracteres"
                className="auth-input"
                disabled={isLoading}
                required
                minLength={6}
              />
            </div>

            <div className="auth-input-group">
              <label htmlFor="confirmPassword" className="auth-label">
                <Lock size={18} />
                Confirmar Senha
              </label>
              <input
                id="confirmPassword"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Digite a senha novamente"
                className="auth-input"
                disabled={isLoading}
                required
              />
            </div>

            <button
              type="submit"
              className="auth-submit-button"
              disabled={isLoading}
            >
              {isLoading ? (
                <>
                  <span className="spinner-small"></span>
                  Criando conta...
                </>
              ) : (
                <>
                  <UserPlus size={20} />
                  Criar Conta
                </>
              )}
            </button>
          </form>

          <div className="auth-footer">
            <p>Já tem uma conta?</p>
            <button
              onClick={onNavigateToLogin}
              className="auth-link-button"
              disabled={isLoading}
            >
              <ArrowLeft size={16} />
              Voltar ao login
            </button>
          </div>
        </div>

        {/* Footer Info */}
        <div className="auth-info">
          <p>🔒 Seus dados estão seguros e criptografados</p>
        </div>
      </div>
    </div>
  );
}

export default RegisterPage;