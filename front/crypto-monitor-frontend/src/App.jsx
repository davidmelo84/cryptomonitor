// front/crypto-monitor-frontend/src/App.jsx
// ✅ VERSÃO FINAL — Registro com verificação de e-mail + TelegramProvider + React Query

import React, { useState, useEffect, useCallback, lazy, Suspense } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { ThemeProvider } from './contexts/ThemeContext';
import { TelegramProvider } from './contexts/TelegramContext'; // ✅ NOVO
import { API_BASE_URL } from './utils/constants';

// ✅ Lazy loading das páginas
const LoginPage = lazy(() => import('./components/pages/LoginPage'));
const RegisterPage = lazy(() => import('./components/pages/RegisterPage'));
const DashboardPage = lazy(() => import('./components/pages/DashboardPage'));
const PortfolioPage = lazy(() => import('./components/pages/PortfolioPage'));
const TradingBotsPage = lazy(() => import('./components/pages/TradingBotsPage'));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 60 * 1000,
    },
  },
});

const PageLoader = () => (
  <div className="min-h-screen flex items-center justify-center">
    <div className="text-center">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-500 mx-auto mb-4" />
      <p className="text-gray-600">Carregando...</p>
    </div>
  </div>
);

function App() {
  const [currentPage, setCurrentPage] = useState('login');
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [authError, setAuthError] = useState('');

  // Configurações gerais
  const [selectedCryptos, setSelectedCryptos] = useState([]);
  const [monitoringEmail, setMonitoringEmail] = useState('');
  const [monitoringInterval, setMonitoringInterval] = useState(5);
  const [buyThreshold, setBuyThreshold] = useState(5.0);
  const [sellThreshold, setSellThreshold] = useState(10.0);

  // ✅ Restaurar sessão
  useEffect(() => {
    const savedToken = localStorage.getItem('token');
    const savedUser = localStorage.getItem('user');

    if (!savedToken || !savedUser) {
      setCurrentPage('login');
      return;
    }

    try {
      const parsedUser = JSON.parse(savedUser);
      setToken(savedToken);
      setUser(parsedUser);
      setCurrentPage('dashboard');
    } catch (error) {
      console.error('Erro ao restaurar sessão:', error);
      localStorage.clear();
      setCurrentPage('login');
    }
  }, []);

  // ✅ Login com tratamento de erros melhorado
  const handleLogin = useCallback(async (username, password) => {
    setAuthError('');

    if (!username || !password) {
      setAuthError('Preencha todos os campos');
      return;
    }

    try {
      console.log('🔑 Tentando login...', { username });
      
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });

      console.log('📡 Response status:', response.status);

      if (!response.ok) {
        const errorText = await response.text();
        console.error('❌ Erro no login:', errorText);
        
        try {
          const errorData = JSON.parse(errorText);
          throw new Error(errorData.error || errorData.message || 'Credenciais inválidas');
        } catch (e) {
          if (response.status === 500) {
            throw new Error('Erro no servidor. Tente novamente mais tarde.');
          }
          throw new Error('Credenciais inválidas');
        }
      }

      const data = await response.json();
      console.log('✅ Login bem-sucedido:', data);

      if (!data.token) {
        throw new Error('Token não recebido do servidor');
      }

      setToken(data.token);
      setUser({ username });
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify({ username }));
      setCurrentPage('dashboard');
      
    } catch (error) {
      console.error('❌ Erro no login:', error);
      setAuthError(error.message || 'Erro ao conectar com o servidor');
    }
  }, []);

  // ✅ Registro com tratamento de erros melhorado
  const handleRegister = useCallback(async (regUsername, regEmail, regPassword, regConfirmPassword) => {
    setAuthError('');

    if (!regUsername || !regEmail || !regPassword || !regConfirmPassword) {
      setAuthError('Preencha todos os campos');
      return false;
    }

    if (regPassword !== regConfirmPassword) {
      setAuthError('As senhas não coincidem');
      return false;
    }

    if (regPassword.length < 6) {
      setAuthError('A senha deve ter pelo menos 6 caracteres');
      return false;
    }

    try {
      console.log('📝 Tentando registrar...', { username: regUsername, email: regEmail });
      
      const response = await fetch(`${API_BASE_URL}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: regUsername,
          email: regEmail,
          password: regPassword,
        }),
      });

      console.log('📡 Response status:', response.status);

      const text = await response.text();
      console.log('📄 Response body:', text);
      
      let data;
      try {
        data = text ? JSON.parse(text) : {};
      } catch {
        throw new Error('Resposta inválida do servidor');
      }

      if (!response.ok) {
        // ✅ TRATAR ERRO 400 (Email/Username já existe)
        if (response.status === 400) {
          if (data.error?.includes('Email') || data.message?.includes('Email')) {
            throw new Error('Este email já está cadastrado. Use outro ou faça login.');
          }
          if (data.error?.includes('Username') || data.message?.includes('Username')) {
            throw new Error('Este username já está em uso. Escolha outro.');
          }
        }
        
        throw new Error(data.error || data.message || 'Falha no registro');
      }

      console.log('✅ Registro bem-sucedido:', data);

      if (data.requiresVerification) {
        alert(`📧 Código de verificação enviado para ${regEmail}!\n\nVerifique sua caixa de entrada.`);
      } else {
        alert('✅ Conta criada com sucesso! Faça login para continuar.');
      }

      return true;
      
    } catch (error) {
      console.error('❌ Erro no registro:', error);
      setAuthError(error.message || 'Erro ao criar conta');
      return false;
    }
  }, []);
  // ✅ Logout
  const handleLogout = () => {
    setUser(null);
    setToken(null);
    setCurrentPage('login');
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    queryClient.clear();
  };

  // ✅ Seleção de criptos
  const toggleCryptoSelection = (crypto) => {
    setSelectedCryptos((prev) => {
      const id = crypto.coinId || crypto.name || crypto.symbol;
      return prev.some((c) => (c.coinId || c.name || c.symbol) === id)
        ? prev.filter((c) => (c.coinId || c.name || c.symbol) !== id)
        : [...prev, crypto];
    });
  };

  // ✅ Props compartilhadas
  const sharedProps = {
    user,
    token,
    authError,
    selectedCryptos,
    monitoringEmail,
    monitoringInterval,
    buyThreshold,
    sellThreshold,
    setMonitoringEmail,
    setMonitoringInterval,
    setBuyThreshold,
    setSellThreshold,
    onToggleCryptoSelection: toggleCryptoSelection,
    onLogout: handleLogout,
    onClearSelection: () => setSelectedCryptos([]),
    onNavigateToPortfolio: () => setCurrentPage('portfolio'),
    onNavigateToBots: () => setCurrentPage('bots'),
    onLogin: handleLogin,
    onRegister: handleRegister,
    onNavigateToLogin: () => {
      setCurrentPage('login');
      setAuthError('');
    },
    onNavigateToRegister: () => {
      setCurrentPage('register');
      setAuthError('');
    },
    onBack: () => setCurrentPage('dashboard'),
  };

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        {/* ✅ Adicionado TelegramProvider */}
        <TelegramProvider>
          <Suspense fallback={<PageLoader />}>
            {currentPage === 'login' && <LoginPage {...sharedProps} />}
            {currentPage === 'register' && <RegisterPage {...sharedProps} />}
            {currentPage === 'dashboard' && <DashboardPage {...sharedProps} />}
            {currentPage === 'portfolio' && <PortfolioPage {...sharedProps} />}
            {currentPage === 'bots' && <TradingBotsPage {...sharedProps} />}
          </Suspense>
        </TelegramProvider>
      </ThemeProvider>

      {/* DevTools React Query */}
      {process.env.NODE_ENV === 'development' && (
        <ReactQueryDevtools initialIsOpen={false} />
      )}
    </QueryClientProvider>
  );
}

export default App;
