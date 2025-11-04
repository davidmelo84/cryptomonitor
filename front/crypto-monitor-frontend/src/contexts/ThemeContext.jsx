import React, { createContext, useContext, useState, useEffect } from 'react';

const ThemeContext = createContext();

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme deve ser usado dentro de ThemeProvider');
  }
  return context;
};

export const ThemeProvider = ({ children }) => {
  const [isDark, setIsDark] = useState(() => {
    const saved = localStorage.getItem('cryptoMonitorTheme');
    return saved ? saved === 'dark' : false;
  });

  useEffect(() => {
    localStorage.setItem('cryptoMonitorTheme', isDark ? 'dark' : 'light');
  }, [isDark]);

  const toggleTheme = () => setIsDark(!isDark);

  const theme = {
    isDark,
    toggleTheme,
    colors: {
      bg: isDark ? '#111827' : '#f8f9fa',
      bgGradient: isDark 
        ? 'linear-gradient(to bottom, #111827, #1f2937)'
        : 'linear-gradient(to bottom, #f8f9fa, #e9ecef)',
      
      cardBg: isDark ? '#1f2937' : 'white',
      cardBgSecondary: isDark ? '#374151' : '#f0f4ff',
      
      text: isDark ? '#f3f4f6' : '#333',
      textSecondary: isDark ? '#9ca3af' : '#666',
      textTertiary: isDark ? '#6b7280' : '#999',
      
      border: isDark ? '#374151' : '#e0e0e0',
      borderHover: isDark ? '#4b5563' : '#667eea',
      
      inputBg: isDark ? '#374151' : 'white',
      inputBorder: isDark ? '#4b5563' : '#e0e0e0',
      inputText: isDark ? '#f3f4f6' : '#333',
      
      success: '#10b981',
      error: '#ef4444',
      info: '#667eea',
      
      headerBg: isDark ? '#1f2937' : 'white',
      shadowColor: isDark ? 'rgba(0,0,0,0.3)' : 'rgba(0,0,0,0.1)',
      
      authGradient: isDark 
        ? 'linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)'
        : 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      
      registerGradient: isDark
        ? 'linear-gradient(135deg, #0f4c42 0%, #1a5c4f 100%)'
        : 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)'
    }
  };

  return (
    <ThemeContext.Provider value={theme}>
      {children}
    </ThemeContext.Provider>
  );
};

export default ThemeContext;