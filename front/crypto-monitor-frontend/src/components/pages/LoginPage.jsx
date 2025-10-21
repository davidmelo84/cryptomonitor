import React, { useState } from 'react';
import { TrendingUp, Eye, EyeOff, AlertCircle } from 'lucide-react';
import { useTheme } from '../../contexts/ThemeContext';
import ThemeToggle from '../common/ThemeToggle';

function LoginPage({ authError, onLogin, onNavigateToRegister }) {
  const { colors } = useTheme();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const handleSubmit = () => {
    onLogin(username, password);
  };

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
      transition: 'background 0.5s ease'
    },
    floatingCircle: {
      position: 'absolute',
      borderRadius: '50%',
      background: 'rgba(255,255,255,0.1)',
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
      borderRadius: '20px',
      padding: '40px',
      boxShadow: `0 20px 60px ${colors.shadowColor}`,
      maxWidth: '450px',
      width: '100%',
      position: 'relative',
      zIndex: 1,
      transition: 'all 0.5s ease'
    },
    title: {
      fontSize: '32px',
      fontWeight: 'bold',
      textAlign: 'center',
      marginBottom: '10px',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      WebkitBackgroundClip: 'text',
      WebkitTextFillColor: 'transparent',
      backgroundClip: 'text',
      transition: 'all 0.3s ease'
    },
    subtitle: {
      textAlign: 'center',
      color: colors.textSecondary,
      marginBottom: '30px',
      fontSize: '14px',
      transition: 'color 0.3s ease'
    },
    inputContainer: {
      position: 'relative',
      marginBottom: '15px'
    },
    input: {
      width: '100%',
      padding: '15px',
      paddingRight: '45px',
      border: `2px solid ${colors.inputBorder}`,
      borderRadius: '10px',
      fontSize: '16px',
      boxSizing: 'border-box',
      background: colors.inputBg,
      color: colors.inputText,
      transition: 'all 0.3s ease'
    },
    eyeIcon: {
      position: 'absolute',
      right: '15px',
      top: '50%',
      transform: 'translateY(-50%)',
      cursor: 'pointer',
      color: colors.textTertiary,
      transition: 'color 0.3s ease'
    },
    button: {
      width: '100%',
      padding: '15px',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      color: 'white',
      border: 'none',
      borderRadius: '10px',
      fontSize: '18px',
      fontWeight: 'bold',
      cursor: 'pointer',
      marginTop: '10px',
      transition: 'transform 0.2s',
      boxShadow: '0 4px 15px rgba(102, 126, 234, 0.4)'
    },
    error: {
      background: colors.isDark ? '#7f1d1d' : '#fee',
      color: colors.isDark ? '#fca5a5' : '#c00',
      padding: '15px',
      borderRadius: '10px',
      marginBottom: '15px',
      display: 'flex',
      alignItems: 'center',
      gap: '10px',
      transition: 'all 0.3s ease'
    },
    link: {
      textAlign: 'center',
      marginTop: '20px',
      color: colors.info,
      cursor: 'pointer',
      fontWeight: 'bold',
      fontSize: '14px',
      transition: 'color 0.3s ease'
    }
  };

  return (
    <div style={styles.container}>
      {/* Theme Toggle */}
      <div style={styles.themeToggleContainer}>
        <ThemeToggle />
      </div>

      {/* Floating Circles */}
      <div style={{...styles.floatingCircle, width: '300px', height: '300px', top: '-150px', left: '-150px'}}></div>
      <div style={{...styles.floatingCircle, width: '200px', height: '200px', bottom: '-100px', right: '-100px'}}></div>
      
      <div style={styles.card}>
        <div style={{textAlign: 'center', marginBottom: '20px'}}>
          <div style={{
            width: '80px',
            height: '80px',
            margin: '0 auto 20px',
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            borderRadius: '20px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 10px 30px rgba(102, 126, 234, 0.3)'
          }}>
            <TrendingUp size={40} color="white" />
          </div>
        </div>
        
        <h1 style={styles.title}>Crypto Monitor</h1>
        <p style={styles.subtitle}>Sistema de Monitoramento Inteligente</p>
        
        <div style={styles.inputContainer}>
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            style={styles.input}
            placeholder="Usuário"
          />
        </div>
        
        <div style={styles.inputContainer}>
          <input
            type={showPassword ? 'text' : 'password'}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
            style={styles.input}
            placeholder="Senha"
          />
          <div style={styles.eyeIcon} onClick={() => setShowPassword(!showPassword)}>
            {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
          </div>
        </div>
        
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
          Entrar
        </button>
        
        <div style={styles.link} onClick={onNavigateToRegister}>
          Não tem conta? <span style={{textDecoration: 'underline'}}>Cadastre-se</span>
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

export default LoginPage;