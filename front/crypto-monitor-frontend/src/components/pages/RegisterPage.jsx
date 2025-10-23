import React, { useState } from 'react';
import { User, Eye, EyeOff, AlertCircle } from 'lucide-react';
import { useTheme } from '../../contexts/ThemeContext';
import ThemeToggle from '../common/ThemeToggle';

function RegisterPage({ authError, onRegister, onNavigateToLogin }) {
  const { isDark } = useTheme();
  const [regUsername, setRegUsername] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regConfirmPassword, setRegConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const handleSubmit = async () => {
    const success = await onRegister(regUsername, regEmail, regPassword, regConfirmPassword);
    if (success) {
      setRegUsername('');
      setRegEmail('');
      setRegPassword('');
      setRegConfirmPassword('');
    }
  };

  const getPasswordStrength = () => {
    if (regPassword.length < 6) return { label: 'Fraca', color: '#ef4444', width: '33%' };
    if (regPassword.length < 10) return { label: 'Média', color: '#f59e0b', width: '66%' };
    return { label: 'Forte', color: '#10b981', width: '100%' };
  };

  const strength = getPasswordStrength();

  return (
    <div className={`auth-container ${isDark ? 'dark' : ''}`}>
      {/* Theme Toggle */}
      <div className="theme-toggle-wrapper">
        <ThemeToggle />
      </div>

      {/* Floating Circle */}
      <div className="floating-circle medium" />

      <div className={`auth-card ${isDark ? 'dark' : ''}`}>
        {/* Logo */}
        <div className="auth-logo-wrapper">
          <div className="auth-logo register">
            <User size={40} color="white" />
          </div>
        </div>

        <h1 className="auth-title register">Criar Conta</h1>
        <p className="auth-subtitle">Junte-se a milhares de investidores</p>

        {/* Username */}
        <div className="auth-input-wrapper">
          <input
            type="text"
            value={regUsername}
            onChange={(e) => setRegUsername(e.target.value)}
            className="auth-input"
            placeholder="Usuário"
          />
        </div>

        {/* Email */}
        <div className="auth-input-wrapper">
          <input
            type="email"
            value={regEmail}
            onChange={(e) => setRegEmail(e.target.value)}
            className="auth-input"
            placeholder="E-mail"
          />
        </div>

        {/* Password */}
        <div className="auth-input-wrapper">
          <input
            type={showPassword ? 'text' : 'password'}
            value={regPassword}
            onChange={(e) => setRegPassword(e.target.value)}
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

        {/* Password Strength */}
        {regPassword && (
          <div className="password-strength">
            <div className="password-strength-label">
              <span>Força da senha: </span>
              <span className="strength" style={{ color: strength.color }}>
                {strength.label}
              </span>
            </div>
            <div className="password-strength-bar">
              <div
                className="password-strength-fill"
                style={{
                  width: strength.width,
                  backgroundColor: strength.color
                }}
              />
            </div>
          </div>
        )}

        {/* Confirm Password */}
        <div className="auth-input-wrapper">
          <input
            type={showPassword ? 'text' : 'password'}
            value={regConfirmPassword}
            onChange={(e) => setRegConfirmPassword(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
            className="auth-input"
            placeholder="Confirmar Senha"
          />
        </div>

        {/* Error */}
        {authError && (
          <div className="alert alert-error">
            <AlertCircle size={20} />
            {authError}
          </div>
        )}

        {/* Submit */}
        <button onClick={handleSubmit} className="auth-button register">
          Cadastrar
        </button>

        {/* Login Link */}
        <div className="auth-link" onClick={onNavigateToLogin}>
          Já tem conta? <span>Faça login</span>
        </div>
      </div>
    </div>
  );
}

export default RegisterPage;