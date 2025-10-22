// front/crypto-monitor-frontend/src/App.jsx

import React, { useState, useEffect, useCallback } from 'react';
import { ThemeProvider } from './contexts/ThemeContext';
import LoginPage from './components/pages/LoginPage';
import RegisterPage from './components/pages/RegisterPage';
import DashboardPage from './components/pages/DashboardPage';
import PortfolioPage from './components/pages/PortfolioPage';
import TradingBotsPage from './components/pages/TradingBotsPage';
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

  const validateToken = useCallback(async (savedToken, savedUser) => {
    try {
      const response = await fetch(`${API_BASE_URL}/user/me`, {
        headers: { 'Authorization': `Bearer ${savedToken}` }
      });

      if (response.ok) {
        console.log('✅ Token válido, restaurando sessão');
        setToken(savedToken);
        setUser(JSON.parse(savedUser));
        setCurrentPage('dashboard');
      } else {
        console.warn('⚠️ Token inválido, redirecionando para login');
        localStorage.clear();
        setCurrentPage('login');
        setToken(null);
        setUser(null);
      }
    } catch (error) {
      console.error('❌ Erro ao validar token:', error);
      localStorage.clear();
      setCurrentPage('login');
      setToken(null);
      setUser(null);
    }
  }, []);

  // ✅ USEEFFECT CORRIGIDO - Validação robusta
  useEffect(() => {
    const savedToken = localStorage.getItem('token');
    const savedUser = localStorage.getItem('user');

    // ✅ SE NÃO HOUVER TOKEN OU USER, VAI DIRETO PARA LOGIN
    if (!savedToken || !savedUser) {
      console.log('🔐 Sem credenciais armazenadas, redirecionando para login');
      setCurrentPage('login');
      setToken(null);
      setUser(null);
      return; // ✅ IMPORTANTE: Parar aqui
    }

    // ✅ SE HOUVER TOKEN, VALIDAR ANTES DE RESTAURAR SESSÃO
    try {
      const parsedUser = JSON.parse(savedUser);

      if (savedToken === 'demo-token') {
        // ═══════════════════════════════════════════════════
        // DEMO TOKEN - Validação simplificada
        // ═══════════════════════════════════════════════════
        if (parsedUser && parsedUser.username) {
          console.log('✅ Restaurando sessão demo:', parsedUser.username);
          setToken(savedToken);
          setUser(parsedUser);
          setCurrentPage('dashboard');
        } else {
          console.warn('⚠️ Token demo inválido, redirecionando para login');
          localStorage.clear();
          setCurrentPage('login');
          setToken(null);
          setUser(null);
        }
      } else {
        // ═══════════════════════════════════════════════════
        // TOKEN REAL - Validação com backend
        // ═══════════════════════════════════════════════════
        console.log('🔍 Validando token JWT com backend...');
        validateToken(savedToken, savedUser);
      }
    } catch (error) {
      // ✅ ERRO AO PARSEAR savedUser → Limpar e ir para login
      console.error('❌ Erro ao validar sessão:', error);
      localStorage.clear();
      setCurrentPage('login');
      setToken(null);
      setUser(null);
    }
  }, [validateToken]);

  // ✅ Verificar status do monitoramento ao carregar
  useEffect(() => {
    if (token) {
      checkMonitoringStatus();
    }
  }, [token]);

  useEffect(() => {
    if (token) {
      fetchAvailableCryptos();
      const interval = setInterval(fetchAvailableCryptos, 60000);
      return () => clearInterval(interval);
    }
  }, [token]);

  // ✅ Verificar se há monitoramento ativo
  const checkMonitoringStatus = async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/monitoring/status`, {
        headers: { 
          'Authorization': `Bearer ${token}`
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        console.log('🔍 Status do monitoramento:', data);
        
        if (data.active) {
          console.log('✅ Monitoramento ativo detectado, sincronizando frontend...');
          setIsMonitoring(true);
        } else {
          console.log('⚪ Nenhum monitoramento ativo');
          setIsMonitoring(false);
        }
      }
    } catch (error) {
      console.error('❌ Erro ao verificar status:', error);
    }
  };

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
        console.log('✅ Login realizado com sucesso');
      } else {
        setAuthError('Credenciais inválidas');
      }
    } catch (error) {
      console.warn('⚠️ Backend indisponível, usando modo demo');
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
    console.log('🔓 Fazendo logout...');
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
      try {
        console.log('🛑 Tentando parar monitoramento...');
        
        const response = await fetch(`${API_BASE_URL}/monitoring/stop`, {
          method: 'POST',
          headers: { 
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          }
        });
        
        if (response.ok) {
          const data = await response.json();
          console.log('✅ Monitoramento parado:', data);
          setIsMonitoring(false);
          alert('🛑 Monitoramento parado com sucesso!');
        } else {
          const errorData = await response.json();
          console.error('❌ Erro ao parar:', errorData);
          alert(`⚠️ Erro ao parar: ${errorData.message || 'Erro desconhecido'}`);
        }
      } catch (error) {
        console.error('❌ Erro na requisição de parar:', error);
        alert('⚠️ Erro ao conectar com o servidor para parar o monitoramento');
      }
      
    } else {
      if (!monitoringEmail || selectedCryptos.length === 0) {
        alert('⚠️ Configure o email e selecione pelo menos uma criptomoeda!');
        return;
      }
      
      try {
        console.log('▶️ Tentando iniciar monitoramento...');
        
        const cryptoIds = selectedCryptos.map(c => c.coinId || c.name);
        const payload = {
          email: monitoringEmail,
          cryptocurrencies: cryptoIds,
          checkIntervalMinutes: monitoringInterval,
          buyThreshold,
          sellThreshold
        };
        
        console.log('📤 Payload enviado:', JSON.stringify(payload, null, 2));
        
        const response = await fetch(`${API_BASE_URL}/monitoring/start`, {
          method: 'POST',
          headers: { 
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify(payload)
        });
        
        if (response.ok) {
          const data = await response.json();
          console.log('✅ Monitoramento iniciado:', data);
          setIsMonitoring(true);
          alert(`✅ Monitoramento iniciado!\n\n📊 ${cryptoIds.length} criptomoeda(s)\n⏱️ Intervalo: ${monitoringInterval} min\n📧 Email: ${monitoringEmail}`);
        } else {
          const errorData = await response.json();
          console.error('❌ Erro ao iniciar:', errorData);
          
          if (errorData.error === 'Monitoramento já está ativo') {
            console.log('⚠️ Monitoramento já ativo detectado, sincronizando...');
            setIsMonitoring(true);
            alert('⚠️ Você já tem um monitoramento ativo. Clique em "Parar" para cancelá-lo.');
          } else {
            alert(`⚠️ Erro ao iniciar: ${errorData.message || 'Erro desconhecido'}`);
          }
        }
      } catch (error) {
        console.error('❌ Erro na requisição de iniciar:', error);
        alert('⚠️ Erro ao conectar com o servidor para iniciar o monitoramento');
      }
    }
  };

  if (!token || !user) {
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
          onNavigateToBots={() => setCurrentPage('bots')}
        />
      )}

      {currentPage === 'portfolio' && (
        <PortfolioPage token={token} onBack={() => setCurrentPage('dashboard')} />
      )}

      {currentPage === 'bots' && (
        <TradingBotsPage token={token} user={user} onBack={() => setCurrentPage('dashboard')} />
      )}
    </ThemeProvider>
  );
}

export default App;