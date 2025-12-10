// front/crypto-monitor-frontend/src/components/pages/LoginPage.jsx
// ✅ CORRIGIDO — sem título/subtítulo duplicados, apenas AnimatedLogo controla texto

import React, { useState } from 'react';
import { LogIn, Mail, Lock } from 'lucide-react';
import { useTheme } from '../../contexts/ThemeContext';
import { useFormValidation, commonValidations } from '../../hooks/useFormValidation';
import ThemeToggle from '../common/ThemeToggle';
import AnimatedLogo from '../common/AnimatedLogo';
import '../../styles/components/auth.css';

function LoginPage({ onLogin, onNavigateToRegister, authError }) {
  const { isDark } = useTheme();
  const variant = "login";

  // ======================================================
  // Remember Me
  // ======================================================
  const [rememberMe, setRememberMe] = useState(() => {
    return localStorage.getItem('rememberMe') === 'true';
  });

  // ======================================================
  // Hook de validação
  // ======================================================
  const {
    values,
    errors,
    touched,
    isSubmitting,
    handleChange,
    handleBlur,
    handleSubmit
  } = useFormValidation(
    { username: '', password: '' },
    {
      username: commonValidations.username,
      password: commonValidations.password
    }
  );

  const onSubmit = async () => {
    await onLogin(values.username, values.password, rememberMe);
  };

  return (
    <div className={`auth-container ${isDark ? 'dark' : ''}`}>
      
      {/* Theme Toggle */}
      <div className="theme-toggle-wrapper">
        <ThemeToggle />
      </div>

      {/* Círculos decorativos */}
      <div className="floating-circle large"></div>
      <div className="floating-circle medium"></div>
      <div className="floating-circle small"></div>

      {/* Card */}
      <div className={`auth-card ${isDark ? 'dark' : ''}`}>
        
        {/* 🔥 Apenas o LOGO animado */}
        <div className="auth-logo-wrapper">
          <AnimatedLogo variant={variant} />
        </div>

        {/* ❌ REMOVIDO — texto duplicado
        <h1 className="auth-title">Crypto Monitor</h1>
        <p className="auth-subtitle">Monitore suas criptomoedas em tempo real</p>
        */}

        {/* Mensagem de erro */}
        {authError && (
          <div className="alert alert-error">
            ⚠️ {authError}
          </div>
        )}

        {/* Formulário */}
        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleSubmit(onSubmit);
          }}
        >

          {/* Usuário */}
          <div className="auth-input-wrapper">
            <input
              type="text"
              value={values.username}
              onChange={(e) => handleChange('username', e.target.value)}
              onBlur={() => handleBlur('username')}
              placeholder="Digite seu usuário"
              className="auth-input"
              disabled={isSubmitting}
              style={{
                borderColor:
                  touched.username && errors.username ? '#ef4444' : undefined
              }}
            />
            <button type="button" className="auth-icon" tabIndex="-1">
              <Mail size={20} />
            </button>
          </div>

          {touched.username && errors.username && (
            <p
              style={{
                color: '#ef4444',
                fontSize: '0.875rem',
                marginTop: '-0.5rem',
                marginBottom: '0.5rem',
                marginLeft: '0.25rem'
              }}
            >
              ⚠️ {errors.username}
            </p>
          )}

          {/* Senha */}
          <div className="auth-input-wrapper">
            <input
              type="password"
              value={values.password}
              onChange={(e) => handleChange('password', e.target.value)}
              onBlur={() => handleBlur('password')}
              placeholder="Digite sua senha"
              className="auth-input"
              disabled={isSubmitting}
              style={{
                borderColor:
                  touched.password && errors.password ? '#ef4444' : undefined
              }}
            />
            <button type="button" className="auth-icon" tabIndex="-1">
              <Lock size={20} />
            </button>
          </div>

          {touched.password && errors.password && (
            <p
              style={{
                color: '#ef4444',
                fontSize: '0.875rem',
                marginTop: '-0.5rem',
                marginBottom: '0.5rem',
                marginLeft: '0.25rem'
              }}
            >
              ⚠️ {errors.password}
            </p>
          )}

          {/* Remember Me */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              marginTop: '-0.5rem',
              marginBottom: '0.5rem',
              cursor: 'pointer',
              userSelect: 'none'
            }}
            onClick={() => setRememberMe(!rememberMe)}
          >
            <input
              type="checkbox"
              id="rememberMe"
              checked={rememberMe}
              onChange={(e) => setRememberMe(e.target.checked)}
              style={{
                width: '18px',
                height: '18px',
                cursor: 'pointer',
                accentColor: '#667eea'
              }}
            />
            <label
              htmlFor="rememberMe"
              style={{
                fontSize: '0.875rem',
                color: 'var(--text-secondary)',
                cursor: 'pointer'
              }}
            >
              Lembrar de mim neste dispositivo
            </label>
          </div>

          {/* Botão Entrar */}
          <button
            type="submit"
            className="auth-button"
            disabled={
              isSubmitting ||
              (touched.username && errors.username) ||
              (touched.password && errors.password)
            }
          >
            {isSubmitting ? (
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
