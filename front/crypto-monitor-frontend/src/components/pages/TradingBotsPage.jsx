  // front/crypto-monitor-frontend/src/components/pages/TradingBotsPage.jsx

  import React, { useState, useEffect } from 'react';
  import {
    Bot,
    Play,
    Square,
    Trash2,
    PlusCircle,
    TrendingUp,
    TrendingDown,
    Activity,
    DollarSign,
    BarChart3,
    Settings,
    X,
    RefreshCw
  } from 'lucide-react';
  import { API_BASE_URL } from '../../utils/constants';

  function TradingBotsPage({ token, user, onBack }) {
    const [bots, setBots] = useState([]);
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [selectedBot, setSelectedBot] = useState(null);
    const [botTrades, setBotTrades] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isRefreshing, setIsRefreshing] = useState(false);

    // Form state
    const [newBot, setNewBot] = useState({
      name: '',
      coinSymbol: 'BTC',
      strategy: 'GRID_TRADING',
      isSimulation: true,
      gridLowerPrice: '',
      gridUpperPrice: '',
      gridLevels: 10,
      amountPerGrid: '',
      dcaAmount: '',
      dcaIntervalMinutes: 60,
      stopLossPercent: '',
      takeProfitPercent: ''
    });
  useEffect(() => {
    fetchBots();
    
    // Auto-refresh a cada 1 minuto
    const interval = setInterval(() => {
      console.log('🔄 Auto-refresh: Atualizando bots...');
      fetchBots();
      
      if (selectedBot) {
        console.log('🔄 Auto-refresh: Atualizando trades do bot', selectedBot.id);
        fetchBotTrades(selectedBot.id);
      }
    }, 60000); // 60 segundos
    
    return () => clearInterval(interval);
  }, [selectedBot]);

    const fetchBots = async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/bots`, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });

        if (response.ok) {
          const data = await response.json();
          setBots(data.bots || []);
        }
      } catch (error) {
        console.error('Erro ao buscar bots:', error);
      }
    };

    const fetchBotTrades = async (botId) => {
  try {
    console.log('🔍 === BUSCANDO TRADES ===');
    console.log('Bot ID:', botId);
    console.log('Token:', token);
    console.log('URL:', `${API_BASE_URL}/bots/${botId}/trades`);
    
    const response = await fetch(`${API_BASE_URL}/bots/${botId}/trades`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    console.log('Status:', response.status);
    console.log('OK?', response.ok);
    
    if (response.ok) {
      const data = await response.json();
      console.log('✅ Dados recebidos:', data);
      console.log('✅ Trades:', data.trades);
      console.log('✅ Quantidade:', data.trades?.length || 0);
      
      if (data.trades && Array.isArray(data.trades)) {
        console.log('✅ Setando trades:', data.trades);
        setBotTrades(data.trades);
      } else {
        console.warn('⚠️ data.trades não é array');
        setBotTrades([]);
      }
    } else {
      const errorText = await response.text();
      console.error('❌ Erro HTTP:', response.status, errorText);
      setBotTrades([]);
    }
  } catch (error) {
    console.error('❌ Erro de rede:', error);
    setBotTrades([]);
  }
};
    const createBot = async () => {
      setIsLoading(true);
      try {
        const response = await fetch(`${API_BASE_URL}/bots`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify(newBot)
        });

        if (response.ok) {
          alert('✅ Bot criado com sucesso!');
          setShowCreateModal(false);
          fetchBots();
          resetForm();
        } else {
          alert('❌ Erro ao criar bot');
        }
      } catch (error) {
        console.error('Erro ao criar bot:', error);
        alert('❌ Erro ao criar bot');
      }
      setIsLoading(false);
    };

    const startBot = async (botId) => {
      try {
        const response = await fetch(`${API_BASE_URL}/bots/${botId}/start`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });

        if (response.ok) {
          alert('✅ Bot iniciado!');
          fetchBots();
        } else {
          alert('❌ Erro ao iniciar bot');
        }
      } catch (error) {
        console.error('Erro ao iniciar bot:', error);
      }
    };

    const stopBot = async (botId) => {
      try {
        const response = await fetch(`${API_BASE_URL}/bots/${botId}/stop`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });

        if (response.ok) {
          alert('🛑 Bot parado!');
          fetchBots();
        } else {
          alert('❌ Erro ao parar bot');
        }
      } catch (error) {
        console.error('Erro ao parar bot:', error);
      }
    };

    const deleteBot = async (botId) => {
      if (!window.confirm('Tem certeza que deseja deletar este bot?')) return;

      try {
        const response = await fetch(`${API_BASE_URL}/bots/${botId}`, {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });

        if (response.ok) {
          alert('✅ Bot deletado!');
          fetchBots();
          if (selectedBot?.id === botId) {
            setSelectedBot(null);
          }
        } else {
          alert('❌ Erro ao deletar bot');
        }
      } catch (error) {
        console.error('Erro ao deletar bot:', error);
      }
    };

    const resetForm = () => {
      setNewBot({
        name: '',
        coinSymbol: 'BTC',
        strategy: 'GRID_TRADING',
        isSimulation: true,
        gridLowerPrice: '',
        gridUpperPrice: '',
        gridLevels: 10,
        amountPerGrid: '',
        dcaAmount: '',
        dcaIntervalMinutes: 60,
        stopLossPercent: '',
        takeProfitPercent: ''
      });
    };

    const handleRefresh = async () => {
    setIsRefreshing(true);
    await fetchBots();
    
    if (selectedBot) {
      await fetchBotTrades(selectedBot.id);
    }
    
    setTimeout(() => setIsRefreshing(false), 500);
  };

    const styles = {
      container: {
        minHeight: '100vh',
        background: 'linear-gradient(to bottom, #f8f9fa, #e9ecef)',
        padding: '20px'
      },
      header: {
        maxWidth: '1400px',
        margin: '0 auto 30px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: '15px'
      },
      title: {
        display: 'flex',
        alignItems: 'center',
        gap: '12px',
        fontSize: '28px',
        fontWeight: 'bold',
        color: '#333'
      },
      buttonGroup: {
        display: 'flex',
        gap: '10px',
        flexWrap: 'wrap'
      },
      button: {
        padding: '10px 20px',
        borderRadius: '10px',
        border: 'none',
        cursor: 'pointer',
        fontWeight: 'bold',
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        transition: 'all 0.2s'
      },
      primaryButton: {
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        color: 'white'
      },
      content: {
        maxWidth: '1400px',
        margin: '0 auto',
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(350px, 1fr))',
        gap: '20px'
      },
      card: {
        background: 'white',
        borderRadius: '15px',
        padding: '25px',
        boxShadow: '0 4px 15px rgba(0,0,0,0.1)',
        transition: 'transform 0.2s'
      },
      modal: {
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        background: 'rgba(0,0,0,0.5)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
        padding: '20px'
      },
      modalContent: {
        background: 'white',
        borderRadius: '20px',
        padding: '30px',
        maxWidth: '600px',
        width: '100%',
        maxHeight: '90vh',
        overflowY: 'auto'
      },
      input: {
        width: '100%',
        padding: '12px',
        border: '2px solid #e0e0e0',
        borderRadius: '10px',
        fontSize: '14px',
        marginBottom: '15px',
        boxSizing: 'border-box'
      },
      select: {
        width: '100%',
        padding: '12px',
        border: '2px solid #e0e0e0',
        borderRadius: '10px',
        fontSize: '14px',
        marginBottom: '15px',
        cursor: 'pointer',
        boxSizing: 'border-box'
      }
    };

    return (
      <div style={styles.container}>
        {/* Header */}
        <div style={styles.header}>
          <h1 style={styles.title}>
            <Bot size={32} color="#667eea" />
            Trading Bots
          </h1>
          
          <div style={styles.buttonGroup}>
            <button
      onClick={handleRefresh}
      disabled={isRefreshing}
      style={{
        ...styles.button,
        background: '#667eea',
        color: 'white',
        opacity: isRefreshing ? 0.5 : 1
      }}
    >
      <RefreshCw 
        size={20} 
        style={{ animation: isRefreshing ? 'spin 1s linear infinite' : 'none' }}
      />
      Atualizar
    </button>
    
          <button
            onClick={() => setShowCreateModal(true)}
            style={{...styles.button, ...styles.primaryButton}}
            >
            <PlusCircle size={20} />
            Criar Bot
            </button>          
            <button
              onClick={onBack}
              style={{
                ...styles.button,
                background: '#6c757d',
                color: 'white'
              }}
            >
              ← Voltar
            </button>
          </div>
        </div>

        {/* Bots Grid */}
        <div style={styles.content}>
          {bots.length === 0 ? (
            <div style={{
              ...styles.card,
              gridColumn: '1 / -1',
              textAlign: 'center',
              padding: '60px 20px'
            }}>
              <Bot size={64} color="#ccc" style={{margin: '0 auto 20px'}} />
              <h3 style={{color: '#666', marginBottom: '10px'}}>Nenhum bot criado</h3>
              <p style={{color: '#999', fontSize: '14px'}}>
                Crie seu primeiro bot para começar a automatizar seus trades!
              </p>
            </div>
          ) : (
            bots.map(bot => (
              <div
                key={bot.id}
                style={styles.card}
                onClick={() => setSelectedBot(bot)}
              >
                {/* Bot Header */}
                <div style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'flex-start',
                  marginBottom: '20px'
                }}>
                  <div style={{flex: 1}}>
                    <h3 style={{
                      margin: 0,
                      fontSize: '20px',
                      fontWeight: 'bold',
                      color: '#333'
                    }}>
                      {bot.name}
                    </h3>
                    <p style={{
                      margin: '5px 0 0 0',
                      fontSize: '14px',
                      color: '#666'
                    }}>
                      {bot.coinSymbol} • {bot.strategy.replace('_', ' ')}
                    </p>
                  </div>
                  
                  <div style={{
                    padding: '6px 12px',
                    borderRadius: '8px',
                    fontSize: '12px',
                    fontWeight: 'bold',
                    background: bot.status === 'RUNNING'
                      ? 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)'
                      : '#e0e0e0',
                    color: bot.status === 'RUNNING' ? 'white' : '#666'
                  }}>
                    {bot.status === 'RUNNING' ? '● ATIVO' : '○ PARADO'}
                  </div>
                </div>

                {/* Stats */}
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(2, 1fr)',
                  gap: '15px',
                  marginBottom: '20px'
                }}>
                  <div style={{
                    background: '#f8f9fa',
                    padding: '12px',
                    borderRadius: '10px'
                  }}>
                    <div style={{
                      fontSize: '11px',
                      color: '#666',
                      marginBottom: '5px'
                    }}>
                      LUCRO/PREJUÍZO
                    </div>
                    <div style={{
                      fontSize: '18px',
                      fontWeight: 'bold',
                      color: bot.totalProfitLoss >= 0 ? '#10b981' : '#ef4444'
                    }}>
                      ${bot.totalProfitLoss?.toFixed(2) || '0.00'}
                    </div>
                  </div>
                  
                  <div style={{
                    background: '#f8f9fa',
                    padding: '12px',
                    borderRadius: '10px'
                  }}>
                    <div style={{
                      fontSize: '11px',
                      color: '#666',
                      marginBottom: '5px'
                    }}>
                      TRADES
                    </div>
                    <div style={{
                      fontSize: '18px',
                      fontWeight: 'bold',
                      color: '#333'
                    }}>
                      {bot.totalTrades || 0}
                    </div>
                  </div>
                </div>

                {/* Actions */}
                <div style={{
                  display: 'flex',
                  gap: '10px'
                }}>
                  {bot.status === 'RUNNING' ? (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        stopBot(bot.id);
                      }}
                      style={{
                        flex: 1,
                        padding: '10px',
                        background: '#ef4444',
                        color: 'white',
                        border: 'none',
                        borderRadius: '8px',
                        cursor: 'pointer',
                        fontWeight: 'bold',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '6px'
                      }}
                    >
                      <Square size={16} />
                      Parar
                    </button>
                  ) : (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        startBot(bot.id);
                      }}
                      style={{
                        flex: 1,
                        padding: '10px',
                        background: '#10b981',
                        color: 'white',
                        border: 'none',
                        borderRadius: '8px',
                        cursor: 'pointer',
                        fontWeight: 'bold',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '6px'
                      }}
                    >
                      <Play size={16} />
                      Iniciar
                    </button>
                  )}
                  
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      deleteBot(bot.id);
                    }}
                    style={{
                      padding: '10px',
                      background: '#dc3545',
                      color: 'white',
                      border: 'none',
                      borderRadius: '8px',
                      cursor: 'pointer'
                    }}
                  >
                    <Trash2 size={16} />
                  </button>
                </div>

                {bot.isSimulation && (
                  <div style={{
                    marginTop: '15px',
                    padding: '10px',
                    background: '#fff3cd',
                    borderRadius: '8px',
                    fontSize: '12px',
                    color: '#856404',
                    textAlign: 'center',
                    fontWeight: 'bold'
                  }}>
                    🎮 MODO SIMULAÇÃO
                  </div>
                )}
              </div>
            ))
          )}
        </div>

        {/* Modal de Criação */}
        {showCreateModal && (
          <div style={styles.modal} onClick={() => setShowCreateModal(false)}>
            <div style={styles.modalContent} onClick={(e) => e.stopPropagation()}>
              <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: '25px'
              }}>
                <h2 style={{margin: 0, fontSize: '24px', fontWeight: 'bold'}}>
                  Criar Novo Bot
                </h2>
                <button
                  onClick={() => setShowCreateModal(false)}
                  style={{
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    color: '#999'
                  }}
                >
                  <X size={24} />
                </button>
              </div>

              <form onSubmit={(e) => {
                e.preventDefault();
                createBot();
              }}>
                <label style={{
                  display: 'block',
                  marginBottom: '8px',
                  fontWeight: 'bold',
                  fontSize: '14px',
                  color: '#333'
                }}>
                  Nome do Bot
                </label>
                <input
                  type="text"
                  value={newBot.name}
                  onChange={(e) => setNewBot({...newBot, name: e.target.value})}
                  style={styles.input}
                  placeholder="Ex: Bot BTC Grid"
                  required
                />

                <label style={{
                  display: 'block',
                  marginBottom: '8px',
                  fontWeight: 'bold',
                  fontSize: '14px',
                  color: '#333'
                }}>
                  Criptomoeda
                </label>
                <select
                  value={newBot.coinSymbol}
                  onChange={(e) => setNewBot({...newBot, coinSymbol: e.target.value})}
                  style={styles.select}
                >
                  <option value="BTC">Bitcoin (BTC)</option>
                  <option value="ETH">Ethereum (ETH)</option>
                  <option value="ADA">Cardano (ADA)</option>
                  <option value="DOT">Polkadot (DOT)</option>
                  <option value="LINK">Chainlink (LINK)</option>
                </select>

                <label style={{
                  display: 'block',
                  marginBottom: '8px',
                  fontWeight: 'bold',
                  fontSize: '14px',
                  color: '#333'
                }}>
                  Estratégia
                </label>
                <select
                  value={newBot.strategy}
                  onChange={(e) => setNewBot({...newBot, strategy: e.target.value})}
                  style={styles.select}
                >
                  <option value="GRID_TRADING">Grid Trading</option>
                  <option value="DCA">DCA (Dollar Cost Averaging)</option>
                  <option value="STOP_LOSS">Stop Loss / Take Profit</option>
                </select>

                {/* Grid Trading Fields */}
                {newBot.strategy === 'GRID_TRADING' && (
                  <>
                    <label style={{
                      display: 'block',
                      marginBottom: '8px',
                      fontWeight: 'bold',
                      fontSize: '14px',
                      color: '#333'
                    }}>
                      Preço Mínimo do Grid
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      value={newBot.gridLowerPrice}
                      onChange={(e) => setNewBot({...newBot, gridLowerPrice: e.target.value})}
                      style={styles.input}
                      placeholder="Ex: 40000"
                      required
                    />

                    <label style={{
                      display: 'block',
                      marginBottom: '8px',
                      fontWeight: 'bold',
                      fontSize: '14px',
                      color: '#333'
                    }}>
                      Preço Máximo do Grid
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      value={newBot.gridUpperPrice}
                      onChange={(e) => setNewBot({...newBot, gridUpperPrice: e.target.value})}
                      style={styles.input}
                      placeholder="Ex: 50000"
                      required
                    />

                    <label style={{
                      display: 'block',
                      marginBottom: '8px',
                      fontWeight: 'bold',
                      fontSize: '14px',
                      color: '#333'
                    }}>
                      Número de Níveis
                    </label>
                    <input
                      type="number"
                      value={newBot.gridLevels}
                      onChange={(e) => setNewBot({...newBot, gridLevels: parseInt(e.target.value)})}
                      style={styles.input}
                      placeholder="Ex: 10"
                      min="2"
                      max="50"
                      required
                    />

                    <label style={{
                      display: 'block',
                      marginBottom: '8px',
                      fontWeight: 'bold',
                      fontSize: '14px',
                      color: '#333'
                    }}>
                      Quantidade por Grid
                    </label>
                    <input
                      type="number"
                      step="0.0001"
                      value={newBot.amountPerGrid}
                      onChange={(e) => setNewBot({...newBot, amountPerGrid: e.target.value})}
                      style={styles.input}
                      placeholder="Ex: 0.001"
                      required
                    />
                  </>
                )}

                {/* DCA Fields */}
                {newBot.strategy === 'DCA' && (
                  <>
                    <label style={{
                      display: 'block',
                      marginBottom: '8px',
                      fontWeight: 'bold',
                      fontSize: '14px',
                      color: '#333'
                    }}>
                      Valor por Compra (USD)
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      value={newBot.dcaAmount}
                      onChange={(e) => setNewBot({...newBot, dcaAmount: e.target.value})}
                      style={styles.input}
                      placeholder="Ex: 100"
                      required
                    />

                    <label style={{
                      display: 'block',
                      marginBottom: '8px',
                      fontWeight: 'bold',
                      fontSize: '14px',
                      color: '#333'
                    }}>
                      Intervalo (minutos)
                    </label>
                    <input
                      type="number"
                      value={newBot.dcaIntervalMinutes}
                      onChange={(e) => setNewBot({...newBot, dcaIntervalMinutes: parseInt(e.target.value)})}
                      style={styles.input}
                      placeholder="Ex: 60 (1 hora)"
                      min="1"
                      required
                    />
                  </>
                )}

                {/* Stop Loss / Take Profit Fields */}
                {newBot.strategy === 'STOP_LOSS' && (
                  <>
                    <label style={{
                      display: 'block',
                      marginBottom: '8px',
                      fontWeight: 'bold',
                      fontSize: '14px',
                      color: '#333'
                    }}>
                      Stop Loss (%)
                    </label>
                    <input
                      type="number"
                      step="0.1"
                      value={newBot.stopLossPercent}
                      onChange={(e) => setNewBot({...newBot, stopLossPercent: e.target.value})}
                      style={styles.input}
                      placeholder="Ex: -5 (perde 5%)"
                      required
                    />

                    <label style={{
                      display: 'block',
                      marginBottom: '8px',
                      fontWeight: 'bold',
                      fontSize: '14px',
                      color: '#333'
                    }}>
                      Take Profit (%)
                    </label>
                    <input
                      type="number"
                      step="0.1"
                      value={newBot.takeProfitPercent}
                      onChange={(e) => setNewBot({...newBot, takeProfitPercent: e.target.value})}
                      style={styles.input}
                      placeholder="Ex: 10 (ganha 10%)"
                      required
                    />

                    <label style={{
                      display: 'block',
                      marginBottom: '8px',
                      fontWeight: 'bold',
                      fontSize: '14px',
                      color: '#333'
                    }}>
                      Quantidade
                    </label>
                    <input
                      type="number"
                      step="0.0001"
                      value={newBot.amountPerGrid}
                      onChange={(e) => setNewBot({...newBot, amountPerGrid: e.target.value})}
                      style={styles.input}
                      placeholder="Ex: 0.01"
                      required
                    />
                  </>
                )}

                {/* Modo Simulação */}
                <div style={{
                  marginTop: '20px',
                  padding: '15px',
                  background: '#f8f9fa',
                  borderRadius: '10px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px'
                }}>
                  <input
                    type="checkbox"
                    checked={newBot.isSimulation}
                    onChange={(e) => setNewBot({...newBot, isSimulation: e.target.checked})}
                    style={{
                      width: '20px',
                      height: '20px',
                      cursor: 'pointer'
                    }}
                  />
                  <label style={{
                    fontSize: '14px',
                    color: '#666',
                    cursor: 'pointer'
                  }}>
                    Modo Simulação (Paper Trading - Recomendado)
                  </label>
                </div>

                {/* Buttons */}
                <div style={{
                  marginTop: '25px',
                  display: 'flex',
                  gap: '10px'
                }}>
                  <button
                    type="button"
                    onClick={() => {
                      setShowCreateModal(false);
                      resetForm();
                    }}
                    style={{
                      flex: 1,
                      padding: '12px',
                      background: '#e0e0e0',
                      border: 'none',
                      borderRadius: '10px',
                      cursor: 'pointer',
                      fontWeight: 'bold',
                      color: '#666'
                    }}
                    disabled={isLoading}
                  >
                    Cancelar
                  </button>

                  <button
                    type="submit"
                    style={{
                      flex: 1,
                      padding: '12px',
                      background: isLoading
                        ? '#ccc'
                        : 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                      color: 'white',
                      border: 'none',
                      borderRadius: '10px',
                      cursor: isLoading ? 'not-allowed' : 'pointer',
                      fontWeight: 'bold'
                    }}
                    disabled={isLoading}
                  >
                    {isLoading ? 'Criando...' : 'Criar Bot'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Modal de Detalhes do Bot */}
        {selectedBot && (
          <div style={styles.modal} onClick={() => setSelectedBot(null)}>
            <div
              style={{
                ...styles.modalContent,
                maxWidth: '800px'
              }}
              onClick={(e) => e.stopPropagation()}
            >
              <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '25px'
        }}>
          <h2 style={{margin: 0, fontSize: '24px', fontWeight: 'bold'}}>
            {selectedBot.name}
          </h2>
          
          <div style={{display: 'flex', gap: '10px', alignItems: 'center'}}>
            {/* ⬅️ BOTÃO DE REFRESH */}
            <button
              onClick={handleRefresh}
              disabled={isRefreshing}
              style={{
                padding: '10px',
                background: isRefreshing ? '#ccc' : '#667eea',
                color: 'white',
                border: 'none',
                borderRadius: '10px',
                cursor: isRefreshing ? 'not-allowed' : 'pointer',
                display: 'flex',
                alignItems: 'center'
              }}
            >
              <RefreshCw 
                size={18} 
                style={{ animation: isRefreshing ? 'spin 1s linear infinite' : 'none' }}
              />
            </button>
            
            <button
              onClick={() => setSelectedBot(null)}
              style={{
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                color: '#999'
              }}
            >
              <X size={24} />
            </button>
          </div>
        </div>

              {/* Bot Info */}
              <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))',
                gap: '15px',
                marginBottom: '25px'
              }}>
                <div style={{
                  background: '#f8f9fa',
                  padding: '15px',
                  borderRadius: '10px'
                }}>
                  <div style={{fontSize: '12px', color: '#666', marginBottom: '5px'}}>
                    Status
                  </div>
                  <div style={{
                    fontSize: '16px',
                    fontWeight: 'bold',
                    color: selectedBot.status === 'RUNNING' ? '#10b981' : '#666'
                  }}>
                    {selectedBot.status === 'RUNNING' ? '● Ativo' : '○ Parado'}
                  </div>
                </div>

                <div style={{
                  background: '#f8f9fa',
                  padding: '15px',
                  borderRadius: '10px'
                }}>
                  <div style={{fontSize: '12px', color: '#666', marginBottom: '5px'}}>
                    Estratégia
                  </div>
                  <div style={{fontSize: '16px', fontWeight: 'bold', color: '#333'}}>
                    {selectedBot.strategy.replace('_', ' ')}
                  </div>
                </div>

                <div style={{
                  background: '#f8f9fa',
                  padding: '15px',
                  borderRadius: '10px'
                }}>
                  <div style={{fontSize: '12px', color: '#666', marginBottom: '5px'}}>
                    Total Trades
                  </div>
                  <div style={{fontSize: '16px', fontWeight: 'bold', color: '#333'}}>
                    {selectedBot.totalTrades || 0}
                  </div>
                </div>

                <div style={{
                  background: selectedBot.totalProfitLoss >= 0 ? '#d1fae5' : '#fee2e2',
                  padding: '15px',
                  borderRadius: '10px'
                }}>
                  <div style={{fontSize: '12px', color: '#666', marginBottom: '5px'}}>
                    Lucro/Prejuízo
                  </div>
                  <div style={{
                    fontSize: '16px',
                    fontWeight: 'bold',
                    color: selectedBot.totalProfitLoss >= 0 ? '#10b981' : '#ef4444'
                  }}>
                    ${selectedBot.totalProfitLoss?.toFixed(2) || '0.00'}
                  </div>
                </div>
              </div>

              {/* Win Rate */}
              {selectedBot.totalTrades > 0 && (
                <div style={{
                  background: '#f8f9fa',
                  padding: '20px',
                  borderRadius: '10px',
                  marginBottom: '25px'
                }}>
                  <div style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    marginBottom: '15px'
                  }}>
                    <div>
                      <div style={{fontSize: '12px', color: '#666'}}>Win Rate</div>
                      <div style={{fontSize: '24px', fontWeight: 'bold', color: '#10b981'}}>
                        {((selectedBot.winningTrades / selectedBot.totalTrades) * 100).toFixed(1)}%
                      </div>
                    </div>
                    <div style={{textAlign: 'right'}}>
                      <div style={{fontSize: '12px', color: '#10b981'}}>
                        ✓ Ganhos: {selectedBot.winningTrades}
                      </div>
                      <div style={{fontSize: '12px', color: '#ef4444'}}>
                        ✗ Perdas: {selectedBot.losingTrades}
                      </div>
                    </div>
                  </div>

                  <div style={{
                    height: '8px',
                    background: '#e0e0e0',
                    borderRadius: '10px',
                    overflow: 'hidden'
                  }}>
                    <div style={{
                      width: `${(selectedBot.winningTrades / selectedBot.totalTrades) * 100}%`,
                      height: '100%',
                      background: 'linear-gradient(90deg, #10b981 0%, #38ef7d 100%)',
                      transition: 'width 0.3s'
                    }} />
                  </div>
                </div>
              )}

              {/* Histórico de Trades */}
              <h3 style={{
                fontSize: '18px',
                fontWeight: 'bold',
                marginBottom: '15px',
                color: '#333'
              }}>
                Histórico de Trades
              </h3>

              <div style={{
                maxHeight: '300px',
                overflowY: 'auto',
                background: '#f8f9fa',
                borderRadius: '10px',
                padding: '15px'
              }}>
                {botTrades.length === 0 ? (
                  <div style={{
                    textAlign: 'center',
                    padding: '40px 20px',
                    color: '#999'
                  }}>
                    <Activity size={48} color="#ccc" style={{margin: '0 auto 15px'}} />
                    <p>Nenhum trade executado ainda</p>
                  </div>
                ) : (
                  <div style={{display: 'flex', flexDirection: 'column', gap: '10px'}}>
                    {botTrades.map((trade, index) => (
                      <div
                        key={trade.id || index}
                        style={{
                          background: 'white',
                          padding: '15px',
                          borderRadius: '10px',
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          borderLeft: `4px solid ${trade.side === 'BUY' ? '#10b981' : '#ef4444'}`
                        }}
                      >
                        <div style={{flex: 1}}>
                          <div style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '10px',
                            marginBottom: '5px'
                          }}>
                            {trade.side === 'BUY' ? (
                              <TrendingUp size={20} color="#10b981" />
                            ) : (
                              <TrendingDown size={20} color="#ef4444" />
                            )}
                            <span style={{
                              fontWeight: 'bold',
                              color: trade.side === 'BUY' ? '#10b981' : '#ef4444'
                            }}>
                              {trade.side === 'BUY' ? 'COMPRA' : 'VENDA'}
                            </span>
                          </div>
                          <div style={{fontSize: '12px', color: '#666'}}>
                            {trade.quantity} {trade.coinSymbol} @ ${trade.price?.toFixed(2)}
                          </div>
                          {trade.reason && (
                            <div style={{fontSize: '11px', color: '#999', marginTop: '3px'}}>
                              {trade.reason}
                            </div>
                          )}
                        </div>

                        <div style={{textAlign: 'right'}}>
                          <div style={{
                            fontSize: '14px',
                            fontWeight: 'bold',
                            color: '#333'
                          }}>
                            ${trade.totalValue?.toFixed(2)}
                          </div>
                          {trade.profitLoss && (
                            <div style={{
                              fontSize: '12px',
                              fontWeight: 'bold',
                              color: trade.profitLoss >= 0 ? '#10b981' : '#ef4444'
                            }}>
                              {trade.profitLoss >= 0 ? '+' : ''}${trade.profitLoss.toFixed(2)}
                            </div>
                          )}
                          <div style={{fontSize: '10px', color: '#999', marginTop: '3px'}}>
                            {new Date(trade.executedAt).toLocaleString('pt-BR')}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Actions */}
              <div style={{
                marginTop: '25px',
                display: 'flex',
                gap: '10px'
              }}>
                {selectedBot.status === 'RUNNING' ? (
                  <button
                    onClick={() => {
                      stopBot(selectedBot.id);
                      setSelectedBot(null);
                    }}
                    style={{
                      flex: 1,
                      padding: '12px',
                      background: '#ef4444',
                      color: 'white',
                      border: 'none',
                      borderRadius: '10px',
                      cursor: 'pointer',
                      fontWeight: 'bold',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '8px'
                    }}
                  >
                    <Square size={18} />
                    Parar Bot
                  </button>
                ) : (
                  <button
                    onClick={() => {
                      startBot(selectedBot.id);
                      setSelectedBot(null);
                    }}
                    style={{
                      flex: 1,
                      padding: '12px',
                      background: '#10b981',
                      color: 'white',
                      border: 'none',
                      borderRadius: '10px',
                      cursor: 'pointer',
                      fontWeight: 'bold',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '8px'
                    }}
                  >
                    <Play size={18} />
                    Iniciar Bot
                  </button>
                )}

                <button
                  onClick={() => {
                    deleteBot(selectedBot.id);
                    setSelectedBot(null);
                  }}
                  style={{
                    padding: '12px 20px',
                    background: '#dc3545',
                    color: 'white',
                    border: 'none',
                    borderRadius: '10px',
                    cursor: 'pointer',
                    fontWeight: 'bold',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px'
                  }}
                >
                  <Trash2 size={18} />
                  Deletar
                </button>
              </div>
            </div>
          </div>
        )}
        <style>{`
      @keyframes spin {
        from { transform: rotate(0deg); }
        to { transform: rotate(360deg); }
      }
    `}</style>
  </div>
);
  }

  export default TradingBotsPage;