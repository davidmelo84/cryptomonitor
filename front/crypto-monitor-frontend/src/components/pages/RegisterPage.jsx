// front/crypto-monitor-frontend/src/components/pages/RegisterPage.jsx
// ✅ VERSÃO COMPLETA — Com redirecionamento para verificação e visual verde

import React, { useState } from 'react';
import { UserPlus, Mail, Lock, User, TrendingUp } from 'lucide-react';
import { useTheme } from '../../contexts/ThemeContext';
import ThemeToggle from '../common/ThemeToggle';
import PasswordStrength from '../auth/PasswordStrength';
import VerifyEmailPage from '../auth/VerifyEmailPage'; // ✅ Import da tela de verificação
import '../../styles/components/auth.css';

function RegisterPage({ onRegister, onNavigateToLogin, authError }) {
  const { isDark } = useTheme();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showVerification, setShowVerification] = useState(false); // ✅ novo estado

  // ✅ Lógica do formulário
  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);

    const success = await onRegister(username, email, password, confirmPassword);

    if (success) {
      // ✅ Redirecionar para verificação de email
      setShowVerification(true);
    }

    setIsLoading(false);
  };

  // ✅ Renderiza a tela de verificação caso necessário
  if (showVerification) {
    return (
      <VerifyEmailPage
        email={email}
        onVerified={() => {
          alert('✅ Email verificado! Você já pode fazer login.');
          onNavigateToLogin();
        }}
        onBack={() => setShowVerification(false)}
      />
    );
  }

  // ✅ Layout padrão do registro (idêntico ao anterior)
  return (
    <div className={`auth-container register ${isDark ? 'dark' : ''}`}>
      {/* Theme Toggle */}
      <div className="theme-toggle-wrapper">
        <ThemeToggle />
      </div>

      {/* Círculos flutuantes (verde) */}
      <div className="floating-circle large"></div>
      <div className="floating-circle medium"></div>

      {/* Card de Registro */}
      <div className={`auth-card ${isDark ? 'dark' : ''}`}>
        {/* Logo Verde */}
        <div className="auth-logo-wrapper">
          <div className="auth-logo register">
            <TrendingUp size={40} color="white" />
          </div>
        </div>

        {/* Título */}
        <h1 className="auth-title register">Criar Conta</h1>
        <p className="auth-subtitle">
          Cadastre-se para começar a monitorar criptomoedas
        </p>

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
              placeholder="Escolha um nome de usuário"
              className="auth-input"
              disabled={isLoading}
              required
              minLength={3}
            />
            <button type="button" className="auth-icon" tabIndex="-1">
              <User size={20} />
            </button>
          </div>

          {/* Email */}
          <div className="auth-input-wrapper">
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Digite seu email"
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
              placeholder="Crie uma senha (mín. 6 caracteres)"
              className="auth-input"
              disabled={isLoading}
              required
              minLength={6}
            />
            <button type="button" className="auth-icon" tabIndex="-1">
              <Lock size={20} />
            </button>
          </div>

          {/* Password Strength */}
          {password && <PasswordStrength password={password} />}

          {/* Confirmar Senha */}
          <div className="auth-input-wrapper">
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Confirme sua senha"
              className="auth-input"
              disabled={isLoading}
              required
            />
            <button type="button" className="auth-icon" tabIndex="-1">
              <Lock size={20} />
            </button>
          </div>

          {/* Validação visual de senhas */}
          {confirmPassword && password !== confirmPassword && (
            <p
              style={{
                color: '#ef4444',
                fontSize: '0.875rem',
                marginTop: '-0.5rem',
                marginBottom: '0.5rem',
              }}
            >
              ⚠️ As senhas não coincidem
            </p>
          )}

          {/* Botão Cadastrar (VERDE) */}
          <button
            type="submit"
            className="auth-button register"
            disabled={
              isLoading ||
              (password && confirmPassword && password !== confirmPassword)
            }
          >
            {isLoading ? (
              <>
                <div className="spinner" style={{ width: '20px', height: '20px' }}></div>
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

        {/* Link para Login */}
        <div
          className="auth-link"
          onClick={isLoading ? undefined : onNavigateToLogin}
        >
          Já tem uma conta? <span>Faça login</span>
        </div>
      </div>
    </div>
  );
}

export default RegisterPage;
