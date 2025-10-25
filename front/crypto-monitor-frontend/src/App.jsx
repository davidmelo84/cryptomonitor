// front/crypto-monitor-frontend/src/App.jsx
import React, { useState, useEffect, useCallback, lazy, Suspense } from 'react';
import { ThemeProvider } from './contexts/ThemeContext';
import { API_BASE_URL } from './utils/constants';

// ✅ Lazy loading das páginas
const LoginPage = lazy(() => import('./components/pages/LoginPage'));
const RegisterPage = lazy(() => import('./components/pages/RegisterPage'));
const DashboardPage = lazy(() => import('./components/pages/DashboardPage'));
const PortfolioPage = lazy(() => import('./components/pages/PortfolioPage'));
const TradingBotsPage = lazy(() => import('./components/pages/TradingBotsPage'));

// ✅ Fallback de carregamento
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

  const [availableCryptos, setAvailableCryptos] = useState([]);
  const [selectedCryptos, setSelectedCryptos] = useState([]);
  const [monitoringEmail, setMonitoringEmail] = useState('');
  const [monitoringInterval, setMonitoringInterval] = useState(5);
  const [buyThreshold, setBuyThreshold] = useState(5.0);
  const [sellThreshold, setSellThreshold] = useState(10.0);
  const [isMonitoring, setIsMonitoring] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [lastUpdate, setLastUpdate] = useState(null);

  // ✅ Validar token com backend
  const validateToken = useCallback(async (savedToken, savedUser) => {
    try {
      const response = await fetch(`${API_BASE_URL}/user/me`, {
        headers: { 'Authorization': `Bearer ${savedToken}` }
      });

      if (response.ok) {
        setToken(savedToken);
        setUser(JSON.parse(savedUser));
        setCurrentPage('dashboard');
      } else {
        localStorage.clear();
        setCurrentPage('login');
        setToken(null);
        setUser(null);
      }
    } catch {
      localStorage.clear();
      setCurrentPage('login');
      setToken(null);
      setUser(null);
    }
  }, []);

  // ✅ Restaurar sessão ao iniciar app
  useEffect(() => {
    const savedToken = localStorage.getItem('token');
    const savedUser = localStorage.getItem('user');

    if (!savedToken || !savedUser) {
      setCurrentPage('login');
      setToken(null);
      setUser(null);
      return;
    }

    try {
      const parsedUser = JSON.parse(savedUser);

      if (savedToken === 'demo-token') {
        if (parsedUser?.username) {
          setToken(savedToken);
          setUser(parsedUser);
          setCurrentPage('dashboard');
        } else {
          localStorage.clear();
          setCurrentPage('login');
        }
      } else {
        validateToken(savedToken, savedUser);
      }
    } catch {
      localStorage.clear();
      setCurrentPage('login');
      setToken(null);
      setUser(null);
    }
  }, [validateToken]);

  // ✅ Fetch inicial de monitoramento e criptos
  useEffect(() => { if (token) checkMonitoringStatus(); }, [token]);
  useEffect(() => {
    if (token) {
      fetchAvailableCryptos();
      const interval = setInterval(fetchAvailableCryptos, 60000);
      return () => clearInterval(interval);
    }
  }, [token]);

  // ===================== FUNÇÕES =====================
  const checkMonitoringStatus = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/monitoring/status`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (response.ok) {
        const data = await response.json();
        setIsMonitoring(data.active);
      }
    } catch (error) {
      console.error(error);
    }
  };

  const fetchAvailableCryptos = async () => {
    setIsRefreshing(true);
    try {
      const response = await fetch(`${API_BASE_URL}/crypto/current`);
      if (!response.ok) throw new Error('API falhou');
      const data = await response.json();
      const normalized = data.map(c => ({
        coinId: c.id,
        name: c.name,
        symbol: c.symbol,
        currentPrice: c.current_price || 0,
        priceChange24h: c.price_change_24h || 0,
        marketCap: c.market_cap || 0
      }));
      setAvailableCryptos(normalized);
      setLastUpdate(new Date());
    } catch {
      // fallback mock
      setAvailableCryptos([
        { coinId: 'bitcoin', name: 'Bitcoin', symbol: 'BTC', currentPrice: 43250.50, priceChange24h: 2.5, marketCap: 845000000000 },
        { coinId: 'ethereum', name: 'Ethereum', symbol: 'ETH', currentPrice: 2280.30, priceChange24h: -1.2, marketCap: 274000000000 },
        { coinId: 'cardano', name: 'Cardano', symbol: 'ADA', currentPrice: 0.58, priceChange24h: 3.8, marketCap: 20000000000 },
        { coinId: 'polkadot', name: 'Polkadot', symbol: 'DOT', currentPrice: 7.45, priceChange24h: 1.5, marketCap: 9000000000 },
        { coinId: 'chainlink', name: 'Chainlink', symbol: 'LINK', currentPrice: 14.82, priceChange24h: -0.8, marketCap: 8000000000 },
        { coinId: 'solana', name: 'Solana', symbol: 'SOL', currentPrice: 98.45, priceChange24h: 5.2, marketCap: 42000000000 }
      ]);
      setLastUpdate(new Date());
    }
    setTimeout(() => setIsRefreshing(false), 500);
  };

  const handleLogin = useCallback(async (username, password) => {
    setAuthError('');
    if (!username || !password) { setAuthError('Preencha todos os campos'); return; }
    try {
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      if (response.ok) {
        const data = await response.json();
        setToken(data.token);
        setUser({ username });
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify({ username }));
        setCurrentPage('dashboard');
      } else {
        setAuthError('Credenciais inválidas');
      }
    } catch {
      setToken('demo-token');
      setUser({ username });
      localStorage.setItem('token', 'demo-token');
      localStorage.setItem('user', JSON.stringify({ username }));
      setCurrentPage('dashboard');
    }
  }, []);

  const handleRegister = useCallback(async (regUsername, regEmail, regPassword, regConfirmPassword) => {
    setAuthError('');
    if (!regUsername || !regEmail || !regPassword || !regConfirmPassword) {
      setAuthError('Preencha todos os campos'); return false;
    }
    if (regPassword !== regConfirmPassword) { setAuthError('As senhas não coincidem'); return false; }
    if (regPassword.length < 6) { setAuthError('A senha deve ter pelo menos 6 caracteres'); return false; }
    try {
      await fetch(`${API_BASE_URL}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: regUsername, email: regEmail, password: regPassword })
      });
    } catch {}
    setCurrentPage('login');
    return true;
  }, []);

  const handleLogout = () => {
    setUser(null);
    setToken(null);
    setCurrentPage('login');
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  };

  const toggleCryptoSelection = (crypto) => {
    setSelectedCryptos(prev => {
      const id = crypto.coinId || crypto.name || crypto.symbol;
      return prev.some(c => (c.coinId || c.name || c.symbol) === id)
        ? prev.filter(c => (c.coinId || c.name || c.symbol) !== id)
        : [...prev, crypto];
    });
  };

  const handleStartStopMonitoring = async () => {
    if (isMonitoring) {
      try {
        const response = await fetch(`${API_BASE_URL}/monitoring/stop`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` }
        });
        if (response.ok) setIsMonitoring(false);
      } catch { console.error('Erro ao parar monitoramento'); }
    } else {
      if (!monitoringEmail || selectedCryptos.length === 0) return;
      try {
        const payload = {
          email: monitoringEmail,
          cryptocurrencies: selectedCryptos.map(c => c.coinId || c.name),
          checkIntervalMinutes: monitoringInterval,
          buyThreshold,
          sellThreshold
        };
        const response = await fetch(`${API_BASE_URL}/monitoring/start`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
          body: JSON.stringify(payload)
        });
        if (response.ok) setIsMonitoring(true);
      } catch { console.error('Erro ao iniciar monitoramento'); }
    }
  };

  // ===================== RENDER =====================
  const pageProps = {
    user, token, authError,
    availableCryptos, selectedCryptos, monitoringEmail, monitoringInterval,
    buyThreshold, sellThreshold, isMonitoring, isRefreshing, lastUpdate,
    setMonitoringEmail, setMonitoringInterval, setBuyThreshold, setSellThreshold,
    onToggleCryptoSelection: toggleCryptoSelection,
    onStartStopMonitoring: handleStartStopMonitoring,
    onRefresh: fetchAvailableCryptos,
    onLogout: handleLogout,
    onClearSelection: () => setSelectedCryptos([]),
    onNavigateToPortfolio: () => setCurrentPage('portfolio'),
    onNavigateToBots: () => setCurrentPage('bots'),
    onLogin: handleLogin,
    onRegister: handleRegister,
    onNavigateToLogin: () => { setCurrentPage('login'); setAuthError(''); },
    onNavigateToRegister: () => { setCurrentPage('register'); setAuthError(''); },
    onBack: () => setCurrentPage('dashboard')
  };

  return (
    <ThemeProvider>
      <Suspense fallback={<PageLoader />}>
        {currentPage === 'login' && <LoginPage {...pageProps} />}
        {currentPage === 'register' && <RegisterPage {...pageProps} />}
        {currentPage === 'dashboard' && <DashboardPage {...pageProps} />}
        {currentPage === 'portfolio' && <PortfolioPage {...pageProps} />}
        {currentPage === 'bots' && <TradingBotsPage {...pageProps} />}
      </Suspense>
    </ThemeProvider>
  );
}

export default App;
