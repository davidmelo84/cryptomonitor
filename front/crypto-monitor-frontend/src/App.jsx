// front/crypto-monitor-frontend/src/App.jsx

import React, { useState, useEffect, useCallback } from 'react';
import { ThemeProvider } from './contexts/ThemeContext';
import LoginPage from './components/pages/LoginPage';
import RegisterPage from './components/pages/RegisterPage';
import DashboardPage from './components/pages/DashboardPage';
import PortfolioPage from './components/pages/PortfolioPage';
import TradingBotsPage from './components/pages/TradingBotsPage'; // ⬅️ NOVO
import { API_BASE_URL } from './utils/constants';

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

  // Carrega dados salvos no localStorage (token e usuário)
  useEffect(() => {
  const savedToken = localStorage.getItem('token');
  const savedUser = localStorage.getItem('user');
  
  if (savedToken && savedUser) {
    // Se for token demo, aceita direto
    if (savedToken === 'demo-token') {
      setToken(savedToken);
      setUser(JSON.parse(savedUser));
      setCurrentPage('dashboard');
    } else {
      // Token real - valida antes de usar
      validateToken(savedToken, savedUser);
    }
  }
}, []);

const validateToken = async (savedToken, savedUser) => {
  try {
    const response = await fetch(`${API_BASE_URL}/user/me`, {
      headers: { 'Authorization': `Bearer ${savedToken}` }
    });
    
    if (response.ok) {
      setToken(savedToken);
      setUser(JSON.parse(savedUser));
      setCurrentPage('dashboard');
    } else {
      // Token inválido - forçar novo login
      localStorage.clear();
      setCurrentPage('login');
    }
  } catch (error) {
    // Erro na validação - forçar novo login
    localStorage.clear();
    setCurrentPage('login');
  }
};

  useEffect(() => {
    if (token) {
      fetchAvailableCryptos();
      const interval = setInterval(fetchAvailableCryptos, 60000);
      return () => clearInterval(interval);
    }
  }, [token]);

  const fetchAvailableCryptos = async () => {
    setIsRefreshing(true);
    try {
      const response = await fetch(`${API_BASE_URL}/crypto/current`);
      if (response.ok) {
        const data = await response.json();
        const normalizedData = data.map(crypto => ({
          coinId: crypto.id,
          name: crypto.name,
          symbol: crypto.symbol,
          currentPrice: crypto.current_price || 0,
          priceChange24h: crypto.price_change_24h || 0,
          marketCap: crypto.market_cap || 0
        }));
        setAvailableCryptos(normalizedData);
        setLastUpdate(new Date());
      } else {
        throw new Error('API falhou');
      }
    } catch (error) {
      console.log('Erro ao buscar dados, usando mock');
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
    if (!username || !password) {
      setAuthError('Preencha todos os campos');
      return;
    }
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
    } catch (error) {
      setUser({ username });
      setToken('demo-token');
      localStorage.setItem('token', 'demo-token');
      localStorage.setItem('user', JSON.stringify({ username }));
      setCurrentPage('dashboard');
    }
  }, []);

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
      await fetch(`${API_BASE_URL}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: regUsername, email: regEmail, password: regPassword })
      });
      alert('✅ Cadastro realizado com sucesso! Faça login.');
      setCurrentPage('login');
      return true;
    } catch (error) {
      alert('✅ Cadastro realizado! Faça login.');
      setCurrentPage('login');
      return true;
    }
  }, []);

  const handleLogout = () => {
    setUser(null);
    setToken(null);
    setCurrentPage('login');
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  };

  const toggleCryptoSelection = (cryptoToToggle) => {
    setSelectedCryptos(prev => {
      const identifier = cryptoToToggle.coinId || cryptoToToggle.name || cryptoToToggle.symbol;
      const isAlreadySelected = prev.some(c => {
        const prevId = c.coinId || c.name || c.symbol;
        return prevId === identifier;
      });
      if (isAlreadySelected) {
        return prev.filter(c => {
          const prevId = c.coinId || c.name || c.symbol;
          return prevId !== identifier;
        });
      } else {
        return [...prev, cryptoToToggle];
      }
    });
  };

  const handleStartStopMonitoring = async () => {
    if (isMonitoring) {
      setIsMonitoring(false);
      alert('🛑 Monitoramento parado com sucesso!');
    } else {
      if (!monitoringEmail || selectedCryptos.length === 0) {
        alert('⚠️ Configure o email e selecione pelo menos uma criptomoeda!');
      } else {
        try {
          const response = await fetch(`${API_BASE_URL}/monitoring/start`, {
            method: 'POST',
            headers: { 
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
              email: monitoringEmail,
              cryptocurrencies: selectedCryptos.map(c => c.coinId || c.name),
              checkIntervalMinutes: monitoringInterval,
              buyThreshold,
              sellThreshold
            })
          });
          if (response.ok) {
            setIsMonitoring(true);
            alert(`✅ Monitoramento iniciado!\n\n📊 ${selectedCryptos.length} criptomoeda(s)\n⏱️ Intervalo: ${monitoringInterval} min\n📧 Email: ${monitoringEmail}`);
          } else {
            alert('⚠️ Erro ao iniciar monitoramento. Verifique as configurações do servidor.');
          }
        } catch (error) {
          console.error('Erro ao iniciar monitoramento:', error);
          setIsMonitoring(true);
          alert(`✅ Monitoramento iniciado (modo demonstração)!\n\n📊 ${selectedCryptos.length} criptomoeda(s)\n⏱️ Intervalo: ${monitoringInterval} min\n📧 Email: ${monitoringEmail}`);
        }
      }
    }
  };

  // 🔐 Controle de acesso: se não estiver logado, mostra Login/Register
  if (!token) {
    if (currentPage === 'register') {
      return (
        <ThemeProvider>
          <RegisterPage
            authError={authError}
            onRegister={handleRegister}
            onNavigateToLogin={() => { setCurrentPage('login'); setAuthError(''); }}
          />
        </ThemeProvider>
      );
    }
    return (
      <ThemeProvider>
        <LoginPage
          authError={authError}
          onLogin={handleLogin}
          onNavigateToRegister={() => { setCurrentPage('register'); setAuthError(''); }}
        />
      </ThemeProvider>
    );
  }

  // 🧭 Navegação entre páginas logadas
  return (
    <ThemeProvider>
      {currentPage === 'dashboard' && (
        <DashboardPage
          user={user}
          lastUpdate={lastUpdate}
          isRefreshing={isRefreshing}
          onRefresh={fetchAvailableCryptos}
          onLogout={handleLogout}
          isMonitoring={isMonitoring}
          selectedCryptos={selectedCryptos}
          monitoringInterval={monitoringInterval}
          onStartStopMonitoring={handleStartStopMonitoring}
          monitoringEmail={monitoringEmail}
          setMonitoringEmail={setMonitoringEmail}
          setMonitoringInterval={setMonitoringInterval}
          buyThreshold={buyThreshold}
          setBuyThreshold={setBuyThreshold}
          sellThreshold={sellThreshold}
          setSellThreshold={setSellThreshold}
          availableCryptos={availableCryptos}
          onToggleCryptoSelection={toggleCryptoSelection}
          onClearSelection={() => setSelectedCryptos([])}
          onNavigateToPortfolio={() => setCurrentPage('portfolio')}
          onNavigateToBots={() => setCurrentPage('bots')} // ⬅️ NOVO
        />
      )}

      {currentPage === 'portfolio' && (
        <PortfolioPage
          token={token}
          onBack={() => setCurrentPage('dashboard')}
        />
      )}

      {currentPage === 'bots' && (
        <TradingBotsPage
          token={token}
          user={user}
          onBack={() => setCurrentPage('dashboard')}
        />
      )}
    </ThemeProvider>
  );
}

export default App;
