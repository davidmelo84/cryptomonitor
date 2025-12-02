// front/crypto-monitor-frontend/src/App.jsx

import React, { useState, useEffect, useCallback, lazy, Suspense } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { ThemeProvider } from './contexts/ThemeContext';
import { TelegramProvider } from './contexts/TelegramContext';
import { API_BASE_URL } from './utils/constants';
import ErrorBoundary from './components/ErrorBoundary';

import { saveAuthData, loadAuthData, clearAuthData } from './utils/storage';

// ===========================================================
// LAZY LOADING + PRELOAD
// ===========================================================
const LoginPage = lazy(() => import('./components/pages/LoginPage'));
LoginPage.preload = () => import('./components/pages/LoginPage');

const RegisterPage = lazy(() => import('./components/pages/RegisterPage'));
RegisterPage.preload = () => import('./components/pages/RegisterPage');

const DashboardPage = lazy(() => import('./components/pages/DashboardPage'));
DashboardPage.preload = () => import('./components/pages/DashboardPage');

const PortfolioPage = lazy(() => import('./components/pages/PortfolioPage'));
PortfolioPage.preload = () => import('./components/pages/PortfolioPage');

const TradingBotsPage = lazy(() => import('./components/pages/TradingBotsPage'));
TradingBotsPage.preload = () => import('./components/pages/TradingBotsPage');

// Função que pre-carrega o componente quando o mouse passa por cima
const preloadComponent = (component) => {
  component?.preload?.();
};

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

  const [selectedCryptos, setSelectedCryptos] = useState([]);
  const [monitoringEmail, setMonitoringEmail] = useState('');
  const [monitoringInterval, setMonitoringInterval] = useState(5);
  const [buyThreshold, setBuyThreshold] = useState(5.0);
  const [sellThreshold, setSellThreshold] = useState(10.0);

  // ============================================================
  // Carregamento de configurações
  // ============================================================
  useEffect(() => {
    try {
      const savedEmail = localStorage.getItem('monitoring_email');
      const savedInterval = localStorage.getItem('monitoring_interval');
      const savedBuyThreshold = localStorage.getItem('buy_threshold');
      const savedSellThreshold = localStorage.getItem('sell_threshold');

      if (savedEmail) setMonitoringEmail(savedEmail);
      if (savedInterval) setMonitoringInterval(parseInt(savedInterval));
      if (savedBuyThreshold) setBuyThreshold(parseFloat(savedBuyThreshold));
      if (savedSellThreshold) setSellThreshold(parseFloat(savedSellThreshold));
    } catch (error) {
      console.error('❌ Erro ao carregar configurações:', error);
    }
  }, []);

  useEffect(() => localStorage.setItem('monitoring_email', monitoringEmail), [monitoringEmail]);
  useEffect(() => localStorage.setItem('monitoring_interval', monitoringInterval), [monitoringInterval]);
  useEffect(() => localStorage.setItem('buy_threshold', buyThreshold), [buyThreshold]);
  useEffect(() => localStorage.setItem('sell_threshold', sellThreshold), [sellThreshold]);

  // ============================================================
  // Restaurar sessão
  // ============================================================
  useEffect(() => {
    const authData = loadAuthData();
    if (!authData) {
      setCurrentPage('login');
      return;
    }
    setToken(authData.token);
    setUser(authData.user);
    setCurrentPage('dashboard');
  }, []);

  // ============================================================
  // LOGIN
  // ============================================================
  const handleLogin = useCallback(async (username, password, rememberMe = false) => {
    setAuthError('');

    if (!username || !password) {
      setAuthError('Preencha todos os campos');
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });

      if (!response.ok) {
        const text = await response.text();
        try {
          const err = JSON.parse(text);
          throw new Error(err.error || err.message || 'Credenciais inválidas');
        } catch {
          throw new Error('Credenciais inválidas');
        }
      }

      const data = await response.json();
      if (!data.token) throw new Error('Token não recebido do servidor');

      saveAuthData(data.token, { username }, rememberMe);

      setToken(data.token);
      setUser({ username });
      localStorage.setItem('last_username', username);

      setCurrentPage('dashboard');

    } catch (error) {
      setAuthError(error.message || 'Erro ao conectar com o servidor');
    }
  }, []);

  // ============================================================
  // REGISTRO
  // ============================================================
  const handleRegister = useCallback(async (u, e, p, cp) => {
    setAuthError('');

    if (!u || !e || !p || !cp) {
      setAuthError('Preencha todos os campos');
      return false;
    }

    if (p !== cp) {
      setAuthError('As senhas não coincidem');
      return false;
    }

    if (p.length < 6) {
      setAuthError('A senha deve ter pelo menos 6 caracteres');
      return false;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: u, email: e, password: p }),
      });

      const text = await response.text();
      let data;

      try {
        data = JSON.parse(text);
      } catch {
        throw new Error('Resposta inválida do servidor');
      }

      if (!response.ok) {
        throw new Error(data.error || data.message || 'Falha no registro');
      }

      if (data.requiresVerification) {
        alert(`📧 Código enviado para ${e}`);
      } else {
        alert('✅ Conta criada! Faça login.');
      }

      return true;

    } catch (error) {
      setAuthError(error.message || 'Erro ao criar conta');
      return false;
    }
  }, []);

  // ============================================================
  // LOGOUT
  // ============================================================
  const handleLogout = () => {
    clearAuthData();
    setUser(null);
    setToken(null);
    setCurrentPage('login');
    queryClient.clear();
  };

  // ============================================================
  // Seleção de criptos
  // ============================================================
  const toggleCryptoSelection = (crypto) => {
    setSelectedCryptos((prev) => {
      const id = crypto.coinId || crypto.name || crypto.symbol;
      return prev.some((c) => (c.coinId || c.name || c.symbol) === id)
        ? prev.filter((c) => (c.coinId || c.name || c.symbol) !== id)
        : [...prev, crypto];
    });
  };

  // ============================================================
  // PROPS COMPARTILHADAS
  // ============================================================
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

    onLogin: handleLogin,
    onRegister: handleRegister,

    onNavigateToPortfolio: () => setCurrentPage('portfolio'),
    onNavigateToBots: () => setCurrentPage('bots'),
    onNavigateToLogin: () => { setCurrentPage('login'); setAuthError(''); },
    onNavigateToRegister: () => { setCurrentPage('register'); setAuthError(''); },
    onBack: () => setCurrentPage('dashboard'),
  };

  // ============================================================
  // RENDER
  // ============================================================
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider>
          <TelegramProvider>
            <Suspense fallback={<PageLoader />}>

              {/* PRELOAD NA NAVEGAÇÃO */}
              <nav className="hidden">
                <div onMouseEnter={() => preloadComponent(LoginPage)} />
                <div onMouseEnter={() => preloadComponent(RegisterPage)} />
                <div onMouseEnter={() => preloadComponent(DashboardPage)} />
                <div onMouseEnter={() => preloadComponent(PortfolioPage)} />
                <div onMouseEnter={() => preloadComponent(TradingBotsPage)} />
              </nav>

              {currentPage === 'login' && <LoginPage {...sharedProps} />}
              {currentPage === 'register' && <RegisterPage {...sharedProps} />}
              {currentPage === 'dashboard' && <DashboardPage {...sharedProps} />}
              {currentPage === 'portfolio' && <PortfolioPage {...sharedProps} />}
              {currentPage === 'bots' && <TradingBotsPage {...sharedProps} />}
            </Suspense>
          </TelegramProvider>
        </ThemeProvider>

        {process.env.NODE_ENV === 'development' && (
          <ReactQueryDevtools initialIsOpen={false} />
        )}
      </QueryClientProvider>
    </ErrorBoundary>
  );
}

export default App;
