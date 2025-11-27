// front/crypto-monitor-frontend/src/components/auth/VerifyEmailPage.jsx
// ✅ CORRIGIDO - Envia email junto com código de verificação

import React, { useState, useRef } from 'react';
import { Mail, ArrowLeft, RefreshCw, CheckCircle } from 'lucide-react';
import { useTheme } from '../../contexts/ThemeContext';
import { API_BASE_URL } from '../../utils/constants';
import '../../styles/components/auth.css';

function VerifyEmailPage({ email, onVerified, onBack }) {
  const { isDark } = useTheme();
  const [code, setCode] = useState(['', '', '', '', '', '']);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [resending, setResending] = useState(false);
  const inputRefs = useRef([]);

  // Manipular mudança nos inputs
  const handleChange = (index, value) => {
    if (!/^\d*$/.test(value)) return; // Apenas números
    const newCode = [...code];
    newCode[index] = value;
    setCode(newCode);
    setError('');
    if (value && index < 5) inputRefs.current[index + 1]?.focus();
  };

  // Manipular backspace
  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !code[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  // Colar código completo
  const handlePaste = (e) => {
    e.preventDefault();
    const pastedData = e.clipboardData.getData('text').slice(0, 6);
    if (!/^\d+$/.test(pastedData)) return;
    const newCode = pastedData.split('');
    while (newCode.length < 6) newCode.push('');
    setCode(newCode);
    inputRefs.current[5]?.focus();
  };

  // ✅ CORRIGIDO - Verificar código com email
  const handleVerify = async () => {
    const fullCode = code.join('');
    if (fullCode.length !== 6) {
      setError('Digite o código completo');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      const response = await fetch(`${API_BASE_URL}/auth/verify`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 
          email: email,  // ✅ ADICIONADO
          code: fullCode 
        }),
      });

      const text = await response.text();
      const data = text ? JSON.parse(text) : {};

      if (response.ok) {
        setSuccess(true);
        setTimeout(() => onVerified(), 1500);
      } else {
        setError(data.error || data.message || 'Código inválido');
        setCode(['', '', '', '', '', '']);
        inputRefs.current[0]?.focus();
      }
    } catch (err) {
      console.error('Erro ao verificar:', err);
      setError('Erro ao conectar com o servidor');
    } finally {
      setIsLoading(false);
    }
  };

  // Reenviar código
  const handleResend = async () => {
    setResending(true);
    setError('');

    try {
      const response = await fetch(`${API_BASE_URL}/auth/resend-code?email=${encodeURIComponent(email)}`, {
        method: 'POST',
      });

      const text = await response.text();
      const data = text ? JSON.parse(text) : {};

      if (response.ok) {
        alert('✅ Novo código enviado para seu email!');
        setCode(['', '', '', '', '', '']);
        inputRefs.current[0]?.focus();
      } else {
        setError(data.error || 'Erro ao reenviar código');
      }
    } catch (err) {
      console.error('Erro ao reenviar:', err);
      setError('Erro ao conectar com o servidor');
    } finally {
      setResending(false);
    }
  };

  return (
    <div className={`auth-container ${isDark ? 'dark' : ''}`}>
      <div className="floating-circle large"></div>
      <div className="floating-circle medium"></div>

      <div className={`auth-card ${isDark ? 'dark' : ''}`} style={{ maxWidth: '450px' }}>
        <div className="auth-logo-wrapper">
          <div className="auth-logo" style={{ background: success ? '#10b981' : '#667eea' }}>
            {success ? <CheckCircle size={40} color="white" /> : <Mail size={40} color="white" />}
          </div>
        </div>

        <h1 className="auth-title">{success ? 'Email Verificado!' : 'Verifique seu Email'}</h1>
        <p className="auth-subtitle" style={{ marginBottom: '1.5rem' }}>
          {success ? '✅ Sua conta foi ativada com sucesso!' : `Enviamos um código de 6 dígitos para ${email}`}
        </p>

        {!success && (
          <>
            {error && (
              <div className="alert alert-error" style={{ marginBottom: '1rem' }}>
                ⚠️ {error}
              </div>
            )}

            <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'center', marginBottom: '1.5rem' }}>
              {code.map((digit, index) => (
                <input
                  key={index}
                  ref={(el) => (inputRefs.current[index] = el)}
                  type="text"
                  inputMode="numeric"
                  maxLength={1}
                  value={digit}
                  onChange={(e) => handleChange(index, e.target.value)}
                  onKeyDown={(e) => handleKeyDown(index, e)}
                  onPaste={handlePaste}
                  disabled={isLoading}
                  style={{
                    width: '50px',
                    height: '60px',
                    fontSize: '1.5rem',
                    textAlign: 'center',
                    border: `2px solid ${isDark ? '#4b5563' : '#e0e0e0'}`,
                    borderRadius: '8px',
                    background: isDark ? '#374151' : 'white',
                    color: isDark ? '#f3f4f6' : '#333',
                    outline: 'none',
                    transition: 'all 0.2s',
                  }}
                  onFocus={(e) => {
                    e.target.style.borderColor = '#667eea';
                    e.target.style.boxShadow = '0 0 0 3px rgba(102, 126, 234, 0.1)';
                  }}
                  onBlur={(e) => {
                    e.target.style.borderColor = isDark ? '#4b5563' : '#e0e0e0';
                    e.target.style.boxShadow = 'none';
                  }}
                />
              ))}
            </div>

            <button
              onClick={handleVerify}
              className="auth-button"
              disabled={isLoading || code.join('').length !== 6}
              style={{ marginBottom: '1rem' }}
            >
              {isLoading ? (
                <>
                  <div className="spinner" style={{ width: '20px', height: '20px' }}></div>
                  Verificando...
                </>
              ) : (
                <>
                  <CheckCircle size={20} />
                  Verificar Código
                </>
              )}
            </button>

            <button
              onClick={handleResend}
              disabled={resending}
              style={{
                width: '100%',
                padding: '0.75rem',
                background: 'transparent',
                border: `2px solid ${isDark ? '#4b5563' : '#e0e0e0'}`,
                borderRadius: '10px',
                color: isDark ? '#f3f4f6' : '#333',
                fontSize: '1rem',
                fontWeight: '500',
                cursor: resending ? 'not-allowed' : 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.5rem',
                transition: 'all 0.2s',
                opacity: resending ? 0.6 : 1,
              }}
              onMouseEnter={(e) => !resending && (e.target.style.borderColor = '#667eea')}
              onMouseLeave={(e) => (e.target.style.borderColor = isDark ? '#4b5563' : '#e0e0e0')}
            >
              <RefreshCw size={18} className={resending ? 'animate-spin' : ''} />
              {resending ? 'Reenviando...' : 'Reenviar Código'}
            </button>
          </>
        )}

        <div
          className="auth-link"
          onClick={success ? undefined : onBack}
          style={{
            marginTop: '1rem',
            cursor: success ? 'default' : 'pointer',
            opacity: success ? 0.5 : 1,
          }}
        >
          <ArrowLeft size={16} style={{ marginRight: '0.25rem' }} />
          {success ? 'Redirecionando...' : 'Voltar ao registro'}
        </div>
      </div>
    </div>
  );
}

export default VerifyEmailPage;