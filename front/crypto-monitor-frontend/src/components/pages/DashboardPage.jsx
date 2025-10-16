import React, { useState, useEffect } from 'react';
import PriceChart from '../dashboard/PriceChart';
import { 
  TrendingUp, 
  User, 
  RefreshCw, 
  LogOut,
  Activity,
  XCircle,
  BarChart3,
  Bell,
  Settings,
  Zap,
  Copy,
  Check
} from 'lucide-react';
import { useTheme } from '../../contexts/ThemeContext';
import ThemeToggle from '../common/ThemeToggle';

function DashboardPage({
  user,
  lastUpdate,
  isRefreshing,
  onRefresh,
  onLogout,
  isMonitoring,
  selectedCryptos,
  monitoringInterval,
  onStartStopMonitoring,
  monitoringEmail,
  setMonitoringEmail,
  setMonitoringInterval,
  buyThreshold,
  setBuyThreshold,
  sellThreshold,
  setSellThreshold,
  availableCryptos,
  onToggleCryptoSelection,
  onClearSelection
}) {
  const { colors } = useTheme();
  const [copiedEmail, setCopiedEmail] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState('marketCap');
  const [hoveredCrypto, setHoveredCrypto] = useState(null);
  const [selectedCryptoForChart, setSelectedCryptoForChart] = useState(null);

  // Auto-selecionar primeira crypto para o gráfico
  useEffect(() => {
    if (selectedCryptos.length > 0 && !selectedCryptoForChart) {
      setSelectedCryptoForChart(selectedCryptos[0]);
    } else if (selectedCryptos.length === 0) {
      setSelectedCryptoForChart(null);
    }
  }, [selectedCryptos, selectedCryptoForChart]);

  const copyEmailToClipboard = () => {
    navigator.clipboard.writeText(monitoringEmail);
    setCopiedEmail(true);
    setTimeout(() => setCopiedEmail(false), 2000);
  };

  const filteredCryptos = availableCryptos
    .filter(crypto =>
      crypto.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      crypto.symbol.toLowerCase().includes(searchTerm.toLowerCase())
    )
    .sort((a, b) => {
      if (sortBy === 'marketCap') return (b.marketCap || 0) - (a.marketCap || 0);
      if (sortBy === 'price') return (b.currentPrice || 0) - (a.currentPrice || 0);
      if (sortBy === 'change') return (b.priceChange24h || 0) - (a.priceChange24h || 0);
      return 0;
    });

  const averageChange = selectedCryptos.length > 0
    ? selectedCryptos.reduce((sum, crypto) => sum + (crypto.priceChange24h || 0), 0) / selectedCryptos.length
    : 0;

  const styles = {
    container: {
      minHeight: '100vh',
      background: colors.bgGradient,
      transition: 'background 0.5s ease'
    },
    header: {
      background: colors.headerBg,
      borderBottom: '3px solid #667eea',
      padding: '20px',
      boxShadow: `0 2px 10px ${colors.shadowColor}`,
      transition: 'all 0.5s ease'
    },
    headerContent: {
      maxWidth: '1400px',
      margin: '0 auto',
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      flexWrap: 'wrap',
      gap: '15px'
    },
    logoContainer: {
      display: 'flex',
      alignItems: 'center',
      gap: '15px'
    },
    logo: {
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      padding: '12px',
      borderRadius: '12px'
    },
    headerButtons: {
      display: 'flex',
      gap: '12px',
      alignItems: 'center',
      flexWrap: 'wrap'
    },
    userBadge: {
      background: colors.cardBgSecondary,
      padding: '8px 16px',
      borderRadius: '8px',
      fontWeight: 'bold',
      fontSize: '14px',
      display: 'flex',
      alignItems: 'center',
      gap: '8px',
      color: colors.text
    },
    button: {
      padding: '8px 16px',
      borderRadius: '8px',
      cursor: 'pointer',
      fontWeight: 'bold',
      display: 'flex',
      alignItems: 'center',
      gap: '8px',
      fontSize: '14px',
      border: 'none',
      transition: 'transform 0.2s'
    },
    content: {
      maxWidth: '1400px',
      margin: '30px auto',
      padding: '0 20px'
    },
    statusCard: {
      padding: '30px',
      borderRadius: '20px',
      marginBottom: '30px',
      color: 'white',
      boxShadow: '0 10px 30px rgba(0,0,0,0.2)',
      position: 'relative',
      overflow: 'hidden',
      background: isMonitoring
        ? 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)'
        : 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
    },
    card: {
      background: colors.cardBg,
      padding: '30px',
      borderRadius: '20px',
      marginBottom: '30px',
      boxShadow: `0 4px 15px ${colors.shadowColor}`,
      color: colors.text
    },
    statsGrid: {
      display: 'grid',
      gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
      gap: '20px',
      marginBottom: '30px'
    },
    statCard: {
      background: colors.cardBg,
      padding: '20px',
      borderRadius: '15px',
      boxShadow: `0 4px 15px ${colors.shadowColor}`
    },
    input: {
      width: '100%',
      padding: '12px',
      border: '2px solid #e0e0e0',
      borderRadius: '10px',
      fontSize: '14px',
      boxSizing: 'border-box',
      transition: 'border-color 0.3s',
      background: colors.cardBg,
      color: colors.text
    },
    select: {
      width: '100%',
      padding: '12px',
      border: '2px solid #e0e0e0',
      borderRadius: '10px',
      fontSize: '14px',
      cursor: 'pointer',
      boxSizing: 'border-box',
      background: colors.cardBg,
      color: colors.text
    },
    cryptoGrid: {
      display: 'grid',
      gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
      gap: '20px'
    },
    cryptoCard: (isSelected, isHovered) => ({
      padding: '20px',
      borderRadius: '15px',
      cursor: 'pointer',
      transition: 'all 0.3s',
      userSelect: 'none',
      border: isSelected ? '3px solid #667eea' : '2px solid #e0e0e0',
      background: isSelected 
        ? 'linear-gradient(135deg, #f0f4ff 0%, #e8edff 100%)' 
        : colors.cardBg,
      boxShadow: isSelected 
        ? '0 8px 20px rgba(102, 126, 234, 0.2)' 
        : isHovered 
        ? '0 8px 20px rgba(0,0,0,0.1)' 
        : `0 2px 8px ${colors.shadowColor}`,
      transform: !isSelected && isHovered ? 'translateY(-5px)' : 'translateY(0)'
    })
  };

  return (
    <div style={styles.container}>
      {/* Header */}
      <div style={styles.header}>
        <div style={styles.headerContent}>
          <div style={styles.logoContainer}>
            <div style={styles.logo}>
              <TrendingUp size={32} color="white" />
            </div>
            <div>
              <h1 style={{ margin: 0, fontSize: '24px', fontWeight: 'bold', color: colors.text }}>
                Crypto Monitor
              </h1>
              <p style={{ margin: 0, color: colors.textSecondary, fontSize: '13px' }}>
                {lastUpdate && `Atualizado ${lastUpdate.toLocaleTimeString()}`}
              </p>
            </div>
          </div>
          
          <div style={styles.headerButtons}>
            <ThemeToggle />
            <div style={styles.userBadge}>
              <User size={18} color="#667eea" />
              {user?.username}
            </div>
            
            <button
              onClick={onRefresh}
              disabled={isRefreshing}
              style={{
                ...styles.button,
                background: '#667eea',
                color: 'white',
                opacity: isRefreshing ? 0.5 : 1
              }}
            >
              <RefreshCw 
                size={16} 
                style={{ animation: isRefreshing ? 'spin 1s linear infinite' : 'none' }} 
              />
              Atualizar
            </button>
            
            <button
              onClick={onLogout}
              style={{
                ...styles.button,
                background: '#ff4444',
                color: 'white'
              }}
            >
              <LogOut size={16} />
              Sair
            </button>
          </div>
        </div>
      </div>

      <div style={styles.content}>
        {/* Status Card */}
        <div style={styles.statusCard}>
          <div style={{
            position: 'absolute',
            top: '-50px',
            right: '-50px',
            width: '200px',
            height: '200px',
            background: 'rgba(255,255,255,0.1)',
            borderRadius: '50%'
          }} />
          
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '20px',
            position: 'relative'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
              <div style={{
                background: 'rgba(255,255,255,0.2)',
                padding: '15px',
                borderRadius: '15px'
              }}>
                {isMonitoring ? <Activity size={40} /> : <XCircle size={40} />}
              </div>
              <div>
                <h2 style={{ margin: 0, fontSize: '28px', fontWeight: 'bold' }}>
                  {isMonitoring ? '✓ Monitoramento Ativo' : '○ Monitoramento Inativo'}
                </h2>
                <p style={{ margin: '8px 0 0 0', fontSize: '16px', opacity: 0.9 }}>
                  {isMonitoring
                    ? `${selectedCryptos.length} moeda(s) • Verificação a cada ${monitoringInterval} min`
                    : 'Configure e inicie para receber alertas em tempo real'}
                </p>
              </div>
            </div>
            
            <button
              onClick={onStartStopMonitoring}
              style={{
                background: 'white',
                padding: '18px 45px',
                borderRadius: '12px',
                fontSize: '18px',
                fontWeight: 'bold',
                cursor: 'pointer',
                boxShadow: '0 4px 15px rgba(0,0,0,0.2)',
                transition: 'transform 0.2s',
                border: 'none',
                color: isMonitoring ? '#f5576c' : '#11998e'
              }}
            >
              {isMonitoring ? '■ Parar' : '▶ Iniciar'}
            </button>
          </div>
        </div>

        {/* Stats Cards */}
        {selectedCryptos.length > 0 && (
          <div style={styles.statsGrid}>
            <div style={styles.statCard}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
                <BarChart3 size={24} color="#667eea" />
                <span style={{ fontSize: '14px', color: colors.textSecondary }}>Selecionadas</span>
              </div>
              <p style={{ margin: 0, fontSize: '32px', fontWeight: 'bold', color: colors.text }}>
                {selectedCryptos.length}
              </p>
            </div>
            
            <div style={styles.statCard}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
                <TrendingUp size={24} color="#667eea" />
                <span style={{ fontSize: '14px', color: colors.textSecondary }}>Variação Média</span>
              </div>
              <p style={{
                margin: 0,
                fontSize: '32px',
                fontWeight: 'bold',
                color: averageChange >= 0 ? '#10b981' : '#ef4444'
              }}>
                {averageChange >= 0 ? '+' : ''}{averageChange.toFixed(2)}%
              </p>
            </div>
            
            <div style={styles.statCard}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
                <Bell size={24} color="#667eea" />
                <span style={{ fontSize: '14px', color: colors.textSecondary }}>Alertas Ativos</span>
              </div>
              <p style={{ margin: 0, fontSize: '32px', fontWeight: 'bold', color: colors.text }}>
                {isMonitoring ? selectedCryptos.length * 2 : 0}
              </p>
            </div>
          </div>
        )}

        {/* Seção de Gráfico */}
        {selectedCryptos.length > 0 && (
          <div style={styles.card}>
            <h2 style={{ 
              display: 'flex', 
              alignItems: 'center', 
              gap: '12px', 
              marginBottom: '20px', 
              fontSize: '22px', 
              fontWeight: 'bold',
              color: colors.text
            }}>
              📊 Análise de Preços
            </h2>

            <div style={{
              display: 'flex',
              gap: '12px',
              marginBottom: '20px',
              overflowX: 'auto',
              paddingBottom: '8px'
            }}>
              {selectedCryptos.map((crypto) => {
                const isSelected = selectedCryptoForChart?.coinId === crypto.coinId;
                return (
                  <button
                    key={crypto.coinId || crypto.symbol}
                    onClick={() => setSelectedCryptoForChart(crypto)}
                    style={{
                      padding: '12px 20px',
                      borderRadius: '10px',
                      fontWeight: 'bold',
                      whiteSpace: 'nowrap',
                      cursor: 'pointer',
                      border: isSelected ? 'none' : '2px solid #ddd',
                      background: isSelected ? '#667eea' : '#ffffff',
                      color: isSelected ? '#ffffff' : '#333333',
                      boxShadow: isSelected ? '0 4px 10px rgba(102, 126, 234, 0.5)' : 'none',
                      transition: 'all 0.25s ease'
                    }}
                  >
                    {crypto.name} ({crypto.symbol?.toUpperCase()})
                  </button>
                );
              })}
            </div>

            {selectedCryptoForChart ? (
              <PriceChart
                coinId={selectedCryptoForChart.coinId || selectedCryptoForChart.name?.toLowerCase()}
                coinName={selectedCryptoForChart.name}
                coinSymbol={selectedCryptoForChart.symbol}
              />
            ) : (
              <div style={{
                background: '#ffffff',
                padding: '60px',
                borderRadius: '20px',
                boxShadow: '0 4px 10px rgba(0,0,0,0.05)',
                textAlign: 'center',
                color: '#999999',
                fontSize: '16px'
              }}>
                📊 Selecione uma criptomoeda acima para ver o gráfico
              </div>
            )}
          </div>
        )}

        {/* Settings Card */}
        <div style={styles.card}>
          <h2 style={{
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            marginBottom: '25px',
            fontSize: '22px',
            fontWeight: 'bold',
            color: colors.text
          }}>
            <Settings size={28} color="#667eea" />
            Configurações de Monitoramento
          </h2>
          
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
            gap: '20px'
          }}>
            <div>
              <label style={{
                display: 'block',
                marginBottom: '10px',
                fontWeight: 'bold',
                fontSize: '14px',
                color: colors.text
              }}>
                📧 Email para Alertas
              </label>
              <div style={{ display: 'flex', gap: '10px' }}>
                <input
                  type="email"
                  value={monitoringEmail}
                  onChange={(e) => setMonitoringEmail(e.target.value)}
                  style={{ ...styles.input, flex: 1 }}
                  placeholder="seu@email.com"
                />
                {monitoringEmail && (
                  <button
                    onClick={copyEmailToClipboard}
                    style={{
                      padding: '12px',
                      background: copiedEmail ? '#10b981' : '#667eea',
                      color: 'white',
                      border: 'none',
                      borderRadius: '10px',
                      cursor: 'pointer'
                    }}
                  >
                    {copiedEmail ? <Check size={20} /> : <Copy size={20} />}
                  </button>
                )}
              </div>
            </div>
            
            <div>
              <label style={{
                display: 'block',
                marginBottom: '10px',
                fontWeight: 'bold',
                fontSize: '14px',
                color: colors.text
              }}>
                ⏱️ Intervalo de Verificação
              </label>
              <select
                value={monitoringInterval}
                onChange={(e) => setMonitoringInterval(parseInt(e.target.value))}
                style={styles.select}
              >
                <option value={1}>1 minuto</option>
                <option value={5}>5 minutos ⭐ Recomendado</option>
                <option value={10}>10 minutos</option>
                <option value={15}>15 minutos</option>
                <option value={30}>30 minutos</option>
                <option value={60}>1 hora</option>
              </select>
            </div>
            
            <div>
              <label style={{
                display: 'block',
                marginBottom: '10px',
                fontWeight: 'bold',
                fontSize: '14px',
                color: colors.text
              }}>
                📉 Alerta de Compra
              </label>
              <input
                type="number"
                value={buyThreshold}
                onChange={(e) => setBuyThreshold(parseFloat(e.target.value) || 0)}
                style={styles.input}
                step="0.5"
                min="0"
                placeholder="% de queda"
              />
            </div>
            
            <div>
              <label style={{
                display: 'block',
                marginBottom: '10px',
                fontWeight: 'bold',
                fontSize: '14px',
                color: colors.text
              }}>
                📈 Alerta de Venda
              </label>
              <input
                type="number"
                value={sellThreshold}
                onChange={(e) => setSellThreshold(parseFloat(e.target.value) || 0)}
                style={styles.input}
                step="0.5"
                min="0"
                placeholder="% de alta"
              />
            </div>
          </div>
        </div>

        {/* Cryptocurrencies Card */}
        <div style={styles.card}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '25px',
            flexWrap: 'wrap',
            gap: '15px'
          }}>
            <div>
              <h2 style={{
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                margin: 0,
                fontSize: '22px',
                fontWeight: 'bold',
                color: colors.text
              }}>
                <Zap size={28} color="#667eea" />
                Criptomoedas Disponíveis
              </h2>
              <p style={{
                margin: '8px 0 0 0',
                color: colors.textSecondary,
                fontSize: '14px'
              }}>
                {selectedCryptos.length} de {filteredCryptos.length} selecionadas
              </p>
            </div>
            
            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                style={{ ...styles.input, minWidth: '200px', margin: 0 }}
                placeholder="🔍 Buscar moeda..."
              />
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                style={styles.select}
              >
                <option value="marketCap">Market Cap</option>
                <option value="price">Preço</option>
                <option value="change">Variação</option>
              </select>
              {selectedCryptos.length > 0 && (
                <button
                  onClick={onClearSelection}
                  style={{
                    background: '#ff4444',
                    color: 'white',
                    border: 'none',
                    padding: '10px 20px',
                    borderRadius: '10px',
                    cursor: 'pointer',
                    fontWeight: 'bold',
                    fontSize: '14px'
                  }}
                >
                  ✕ Limpar
                </button>
              )}
            </div>
          </div>
          
          <div style={styles.cryptoGrid}>
            {filteredCryptos.map((crypto, index) => {
              const identifier = crypto.coinId || crypto.name || crypto.symbol || index;
              const isSelected = selectedCryptos.some(c => {
                const selectedId = c.coinId || c.name || c.symbol;
                return selectedId === identifier;
              });
              const isPriceUp = (crypto.priceChange24h || 0) >= 0;
              const isHovered = hoveredCrypto === identifier;
              
              return (
                <div
                  key={identifier}
                  onClick={() => onToggleCryptoSelection(crypto)}
                  onMouseEnter={() => setHoveredCrypto(identifier)}
                  onMouseLeave={() => setHoveredCrypto(null)}
                  style={styles.cryptoCard(isSelected, isHovered)}
                >
                  <div style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'flex-start',
                    marginBottom: '15px'
                  }}>
                    <div style={{ flex: 1 }}>
                      <h3 style={{
                        margin: 0,
                        fontSize: '18px',
                        fontWeight: 'bold',
                        color: colors.text
                      }}>
                        {crypto.name}
                      </h3>
                      <p style={{
                        margin: '5px 0 0 0',
                        color: colors.textSecondary,
                        fontSize: '13px',
                        fontWeight: '600'
                      }}>
                        {(crypto.symbol || '').toUpperCase()}
                      </p>
                    </div>
                    <div style={{
                      width: '28px',
                      height: '28px',
                      borderRadius: '8px',
                      border: `2px solid ${isSelected ? '#667eea' : '#ccc'}`,
                      background: isSelected ? '#667eea' : 'white',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: 'white',
                      fontWeight: 'bold',
                      fontSize: '16px',
                      flexShrink: 0
                    }}>
                      {isSelected && '✓'}
                    </div>
                  </div>
                  
                  <div style={{ marginBottom: '12px' }}>
                    <p style={{
                      margin: 0,
                      fontSize: '28px',
                      fontWeight: 'bold',
                      color: colors.text
                    }}>
                      ${(crypto.currentPrice || 0).toLocaleString('en-US', {
                        minimumFractionDigits: 2,
                        maximumFractionDigits: 2
                      })}
                    </p>
                  </div>
                  
                  <div style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                  }}>
                    <div style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '6px',
                      padding: '6px 12px',
                      borderRadius: '8px',
                      background: isPriceUp ? '#dcfce7' : '#fee2e2',
                      color: isPriceUp ? '#166534' : '#991b1b',
                      fontSize: '14px',
                      fontWeight: 'bold'
                    }}>
                      <span style={{ fontSize: '16px' }}>{isPriceUp ? '▲' : '▼'}</span>
                      <span>{Math.abs(crypto.priceChange24h || 0).toFixed(2)}%</span>
                    </div>
                    
                    {crypto.marketCap && (
                      <div style={{
                        fontSize: '11px',
                        color: colors.textSecondary,
                        fontWeight: '600'
                      }}>
                        ${(crypto.marketCap / 1000000000).toFixed(1)}B
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
          
          {filteredCryptos.length === 0 && (
            <div style={{
              textAlign: 'center',
              padding: '60px 20px',
              color: colors.textSecondary
            }}>
              <p style={{ fontSize: '18px', marginBottom: '10px' }}>
                Nenhuma criptomoeda encontrada
              </p>
              <p style={{ fontSize: '14px' }}>Tente buscar por outro termo</p>
            </div>
          )}
        </div>
      </div>

      <style>{`
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
}

export default DashboardPage;