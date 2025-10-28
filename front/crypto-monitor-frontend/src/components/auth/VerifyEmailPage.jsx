// front/crypto-monitor-frontend/src/components/auth/VerifyEmailPage.jsx
import React, { useState } from 'react';
import { Mail, Check, AlertCircle } from 'lucide-react';
import { useTheme } from '../../contexts/ThemeContext';
import { API_BASE_URL } from '../../utils/constants';
import '../../styles/components.css';

function VerifyEmailPage({ email, onVerified, onBack }) {
  const { isDark } = useTheme();
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleVerify = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (code.length !== 6) {
      setError('O código deve ter 6 dígitos');
      return;
    }

    setLoading(true);

    try {
      const response = await fetch(`${API_BASE_URL}/user/verify`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code })
      });

      const data = await response.json();

      if (response.ok && data.success) {
        setSuccess(data.message);
        setTimeout(() => onVerified(), 2000);
      } else {
        setError(data.error || data.message || 'Código inválido');
      }
    } catch (err) {
      setError('Erro ao verificar código. Tente novamente.');
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    setError('');
    setSuccess('');

    try {
      const response = await fetch(`${API_BASE_URL}/user/resend-code`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email })
      });

      const data = await response.json();

      if (response.ok && data.success) {
        setSuccess('Novo código enviado para seu email!');
      } else {
        setError('Erro ao reenviar código');
      }
    } catch (err) {
      setError('Erro ao reenviar código');
    }
  };

  return (
    <div className={`auth-container ${isDark ? 'dark' : ''}`}>
      <div className={`auth-card ${isDark ? 'dark' : ''}`}>
        {/* Logo */}
        <div className="auth-logo-wrapper">
          <div className="auth-logo register">
            <Mail size={40} color="white" />
          </div>
        </div>

        {/* Título */}
        <h1 className="auth-title register">Verificar Email</h1>
        <p className="auth-subtitle">
          Enviamos um código de 6 dígitos para<br/>
          <strong>{email}</strong>
        </p>

        {/* Alertas */}
        {error && (
          <div className="alert alert-error">
            <AlertCircle size={20} />
            {error}
          </div>
        )}

        {success && (
          <div className="alert" style={{background: '#d1fae5', color: '#065f46'}}>
            <Check size={20} />
            {success}
          </div>
        )}

        {/* Formulário */}
        <form onSubmit={handleVerify}>
          <div className="auth-input-wrapper">
            <input
              type="text"
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              placeholder="Digite o código de 6 dígitos"
              className="auth-input"
              style={{textAlign: 'center', fontSize: '24px', letterSpacing: '10px'}}
              maxLength={6}
              disabled={loading}
              required
            />
          </div>

          <button
            type="submit"
            className="auth-button register"
            disabled={loading || code.length !== 6}
          >
            {loading ? 'Verificando...' : (
              <>
                <Check size={20} />
                Verificar Código
              </>
            )}
          </button>
        </form>

        {/* Ações adicionais */}
        <div style={{textAlign: 'center', marginTop: '20px'}}>
          <button
            onClick={handleResend}
            className="auth-link"
            style={{background: 'none', border: 'none', cursor: 'pointer'}}
          >
            Não recebeu? <span>Reenviar código</span>
          </button>
        </div>

        <div style={{textAlign: 'center', marginTop: '10px'}}>
          <button
            onClick={onBack}
            className="auth-link"
            style={{background: 'none', border: 'none', cursor: 'pointer'}}
          >
            Voltar ao registro
          </button>
        </div>
      </div>
    </div>
  );
}

export default VerifyEmailPage;