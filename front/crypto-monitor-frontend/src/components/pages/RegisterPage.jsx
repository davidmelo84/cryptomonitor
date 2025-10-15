import React, { useState } from 'react';
import { User, Eye, EyeOff, AlertCircle } from 'lucide-react';
import { useTheme } from '../../contexts/ThemeContext';
import ThemeToggle from '../common/ThemeToggle'; // ✅ mesmo botão usado no Login

function RegisterPage({ authError, onRegister, onNavigateToLogin }) {
  const { colors, isDark } = useTheme();
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

  const styles = {
    container: {
      minHeight: '100vh',
      background: colors.authGradient,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '20px',
      position: 'relative',
      overflow: 'hidden',
      transition: 'background 0.4s ease'
    },
    floatingCircle: {
      position: 'absolute',
      borderRadius: '50%',
      background: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(255,255,255,0.15)',
      animation: 'float 6s ease-in-out infinite'
    },
    themeToggleContainer: {
      position: 'absolute',
      top: '20px',
      right: '20px',
      zIndex: 1000
    },
    card: {
      background: colors.cardBg,
      color: colors.text,
      borderRadius: '20px',
      padding: '40px',
      boxShadow: `0 20px 60px ${colors.shadowColor}`,
      maxWidth: '450px',
      width: '100%',
      position: 'relative',
      zIndex: 1,
      transition: 'background 0.3s, color 0.3s'
    },
    title: {
      fontSize: '32px',
      fontWeight: 'bold',
      textAlign: 'center',
      marginBottom: '10px',
      background: colors.registerGradient,
      WebkitBackgroundClip: 'text',
      WebkitTextFillColor: 'transparent'
    },
    subtitle: {
      textAlign: 'center',
      color: colors.textSecondary,
      marginBottom: '30px',
      fontSize: '14px'
    },
    input: {
      width: '100%',
      padding: '15px',
      border: `2px solid ${colors.border}`,
      background: colors.inputBg,
      color: colors.inputText,
      borderRadius: '10px',
      fontSize: '16px',
      boxSizing: 'border-box',
      marginBottom: '15px',
      transition: 'border-color 0.3s, background 0.3s'
    },
    inputContainer: {
      position: 'relative',
      marginBottom: '15px'
    },
    eyeIcon: {
      position: 'absolute',
      right: '15px',
      top: '50%',
      transform: 'translateY(-50%)',
      cursor: 'pointer',
      color: colors.textTertiary
    },
    button: {
      width: '100%',
      padding: '15px',
      background: colors.registerGradient,
      color: 'white',
      border: 'none',
      borderRadius: '10px',
      fontSize: '18px',
      fontWeight: 'bold',
      cursor: 'pointer',
      marginTop: '10px',
      transition: 'transform 0.2s',
      boxShadow: `0 4px 15px rgba(17, 153, 142, 0.4)`
    },
    error: {
      background: '#fee',
      color: '#c00',
      padding: '15px',
      borderRadius: '10px',
      marginBottom: '15px',
      display: 'flex',
      alignItems: 'center',
      gap: '10px'
    },
    link: {
      textAlign: 'center',
      marginTop: '20px',
      color: colors.info,
      cursor: 'pointer',
      fontWeight: 'bold',
      fontSize: '14px'
    }
  };

  return (
    <div style={styles.container}>
      {/* ✅ Theme Toggle igual ao Login */}
      <div style={styles.themeToggleContainer}>
        <ThemeToggle />
      </div>

      <div style={{ ...styles.floatingCircle, width: '250px', height: '250px', top: '10%', right: '10%' }}></div>

      <div style={styles.card}>
        <div style={{ textAlign: 'center', marginBottom: '20px' }}>
          <div style={{
            width: '80px',
            height: '80px',
            margin: '0 auto 20px',
            background: colors.registerGradient,
            borderRadius: '20px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 10px 30px rgba(17, 153, 142, 0.3)'
          }}>
            <User size={40} color="white" />
          </div>
        </div>

        <h1 style={styles.title}>Criar Conta</h1>
        <p style={styles.subtitle}>Junte-se a milhares de investidores</p>

        <input
          type="text"
          value={regUsername}
          onChange={(e) => setRegUsername(e.target.value)}
          style={styles.input}
          placeholder="Usuário"
        />

        <input
          type="email"
          value={regEmail}
          onChange={(e) => setRegEmail(e.target.value)}
          style={styles.input}
          placeholder="E-mail"
        />

        <div style={styles.inputContainer}>
          <input
            type={showPassword ? 'text' : 'password'}
            value={regPassword}
            onChange={(e) => setRegPassword(e.target.value)}
            style={{ ...styles.input, marginBottom: 0, paddingRight: '45px' }}
            placeholder="Senha"
          />
          <div style={styles.eyeIcon} onClick={() => setShowPassword(!showPassword)}>
            {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
          </div>
        </div>

        {regPassword && (
          <div style={{ marginBottom: '15px', fontSize: '12px' }}>
            <span>Força da senha: </span>
            <span style={{ color: strength.color, fontWeight: 'bold' }}>{strength.label}</span>
            <div style={{ height: '4px', background: colors.border, borderRadius: '2px', marginTop: '5px' }}>
              <div style={{
                width: strength.width,
                height: '100%',
                background: strength.color,
                borderRadius: '2px',
                transition: 'width 0.3s'
              }}></div>
            </div>
          </div>
        )}

        <input
          type={showPassword ? 'text' : 'password'}
          value={regConfirmPassword}
          onChange={(e) => setRegConfirmPassword(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
          style={styles.input}
          placeholder="Confirmar Senha"
        />

        {authError && (
          <div style={styles.error}>
            <AlertCircle size={20} />
            {authError}
          </div>
        )}

        <button
          onClick={handleSubmit}
          style={styles.button}
          onMouseOver={(e) => e.target.style.transform = 'scale(1.02)'}
          onMouseOut={(e) => e.target.style.transform = 'scale(1)'}
        >
          Cadastrar
        </button>

        <div style={styles.link} onClick={onNavigateToLogin}>
          Já tem conta? <span style={{ textDecoration: 'underline' }}>Faça login</span>
        </div>
      </div>

      <style>{`
        @keyframes float {
          0%, 100% { transform: translateY(0px); }
          50% { transform: translateY(-20px); }
        }
      `}</style>
    </div>
  );
}

export default RegisterPage;
