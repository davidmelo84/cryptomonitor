// front/crypto-monitor-frontend/src/components/telegram/TelegramConfig.jsx
// ✅ VERSÃO CORRIGIDA - Tratamento de erros adequado

import React, { useState, useEffect } from 'react';
import '../../styles/TelegramConfig.css';

const TelegramConfig = ({ userEmail, token }) => {
  // ============================================
  // STATE MANAGEMENT
  // ============================================
  const [config, setConfig] = useState({
    botToken: '',
    chatId: '',
    enabled: false
  });

  const [status, setStatus] = useState({
    isConnected: false,
    lastTest: null,
    testing: false
  });

  const [notifications, setNotifications] = useState({
    email: true,
    telegram: false
  });

  const [showInstructions, setShowInstructions] = useState(false);
  const [error, setError] = useState('');

  // ============================================
  // LOAD SAVED CONFIG
  // ============================================
  useEffect(() => {
    loadSavedConfig();
  }, []);

  const loadSavedConfig = () => {
    try {
      const saved = localStorage.getItem('telegram_config');
      if (saved) {
        const parsed = JSON.parse(saved);
        setConfig(parsed);
        setNotifications(prev => ({
          ...prev,
          telegram: parsed.enabled
        }));
      }
    } catch (error) {
      console.error('Erro ao carregar config:', error);
    }
  };

  // ============================================
  // SAVE CONFIG
  // ============================================
  const saveConfig = () => {
    try {
      localStorage.setItem('telegram_config', JSON.stringify(config));
      alert('✅ Configurações do Telegram salvas!');
      setError('');
    } catch (error) {
      console.error('Erro ao salvar:', error);
      setError('Erro ao salvar configurações');
    }
  };

  // ============================================
  // TEST TELEGRAM CONNECTION - CORRIGIDO
  // ============================================
  const testConnection = async () => {
    if (!config.botToken || !config.chatId) {
      setError('⚠️ Preencha o Token do Bot e o Chat ID');
      return;
    }

    setStatus(prev => ({ ...prev, testing: true }));
    setError('');

    try {
      console.log('🧪 Testando conexão Telegram...');
      console.log('   Token:', config.botToken.substring(0, 10) + '...');
      console.log('   Chat ID:', config.chatId);

      // ✅ CORREÇÃO: Enviar via Telegram API diretamente
      // Isso evita depender de um endpoint backend que pode não existir
      const telegramApiUrl = `https://api.telegram.org/bot${config.botToken}/sendMessage`;
      
      const response = await fetch(telegramApiUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          chat_id: config.chatId,
          text: '🧪 Teste de Conexão do Crypto Monitor\n\n✅ Se você recebeu esta mensagem, o Telegram está configurado corretamente!',
          parse_mode: 'Markdown'
        })
      });

      const data = await response.json();

      console.log('📡 Resposta Telegram:', data);

      if (response.ok && data.ok) {
        setStatus({
          isConnected: true,
          lastTest: new Date().toLocaleString('pt-BR'),
          testing: false
        });
        
        // Salvar automaticamente se conectou
        const updatedConfig = { ...config, enabled: true };
        setConfig(updatedConfig);
        localStorage.setItem('telegram_config', JSON.stringify(updatedConfig));
        
        alert('✅ Conexão com Telegram estabelecida!\n\nVerifique sua mensagem de teste.');
        setError('');
      } else {
        throw new Error(data.description || 'Falha ao conectar');
      }
    } catch (error) {
      console.error('❌ Erro:', error);
      
      setStatus(prev => ({
        ...prev,
        isConnected: false,
        testing: false
      }));
      
      // ✅ Mensagens de erro mais específicas
      let errorMessage = 'Erro ao conectar com Telegram';
      
      if (error.message.includes('Unauthorized')) {
        errorMessage = '❌ Token do Bot inválido. Verifique o token com @BotFather';
      } else if (error.message.includes('chat not found')) {
        errorMessage = '❌ Chat ID inválido. Verifique com @userinfobot';
      } else if (error.message.includes('NetworkError') || error.message.includes('Failed to fetch')) {
        errorMessage = '❌ Erro de conexão. Verifique sua internet';
      } else {
        errorMessage = `❌ ${error.message}`;
      }
      
      setError(errorMessage);
    }
  };

  // ============================================
  // TOGGLE NOTIFICATION CHANNELS
  // ============================================
  const toggleNotificationChannel = (channel) => {
    setNotifications(prev => ({
      ...prev,
      [channel]: !prev[channel]
    }));

    if (channel === 'telegram') {
      setConfig(prev => ({
        ...prev,
        enabled: !prev.enabled
      }));
    }
  };

  // ============================================
  // RENDER
  // ============================================
  return (
    <div className="telegram-config">
      
      {/* HEADER */}
      <div className="telegram-header">
        <h2 className="telegram-title">
          <span className="telegram-icon">📱</span>
          Configuração do Telegram
        </h2>
        <p className="telegram-subtitle">
          Configure notificações via Telegram Bot
        </p>
      </div>

      {/* ERROR MESSAGE */}
      {error && (
        <div style={{
          background: '#fee2e2',
          color: '#991b1b',
          padding: '12px 16px',
          borderRadius: '10px',
          marginBottom: '20px',
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          fontWeight: '500'
        }}>
          ⚠️ {error}
        </div>
      )}

      {/* NOTIFICATION CHANNELS */}
      <div className="telegram-section">
        <h3 className="section-title">Canais de Notificação</h3>
        
        <div className="channel-grid">
          {/* Email Channel */}
          <div 
            className={`channel-card ${notifications.email ? 'channel-active' : ''}`}
            onClick={() => toggleNotificationChannel('email')}
          >
            <div className="channel-icon">📧</div>
            <div className="channel-info">
              <h4 className="channel-name">Email</h4>
              <p className="channel-email">{userEmail}</p>
            </div>
            <div className="channel-checkbox">
              {notifications.email && <span className="checkmark">✓</span>}
            </div>
          </div>

          {/* Telegram Channel */}
          <div 
            className={`channel-card ${notifications.telegram ? 'channel-active' : ''}`}
            onClick={() => toggleNotificationChannel('telegram')}
          >
            <div className="channel-icon">💬</div>
            <div className="channel-info">
              <h4 className="channel-name">Telegram</h4>
              <p className="channel-status">
                {status.isConnected ? '🟢 Conectado' : '⚪ Desconectado'}
              </p>
            </div>
            <div className="channel-checkbox">
              {notifications.telegram && <span className="checkmark">✓</span>}
            </div>
          </div>
        </div>
      </div>

      {/* TELEGRAM CONFIGURATION */}
      {notifications.telegram && (
        <div className="telegram-section">
          <h3 className="section-title">Credenciais do Bot</h3>
          
          {/* Bot Token */}
          <div className="form-group">
            <label className="form-label">
              🤖 Token do Bot
              <button 
                className="help-button"
                onClick={() => setShowInstructions(!showInstructions)}
                type="button"
              >
                ?
              </button>
            </label>
            <input
              type="text"
              value={config.botToken}
              onChange={(e) => {
                setConfig(prev => ({ ...prev, botToken: e.target.value }));
                setError('');
              }}
              placeholder="123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11"
              className="telegram-input"
            />
          </div>

          {/* Chat ID */}
          <div className="form-group">
            <label className="form-label">💬 Chat ID</label>
            <input
              type="text"
              value={config.chatId}
              onChange={(e) => {
                setConfig(prev => ({ ...prev, chatId: e.target.value }));
                setError('');
              }}
              placeholder="123456789"
              className="telegram-input"
            />
          </div>

          {/* Action Buttons */}
          <div className="button-group">
            <button 
              onClick={testConnection}
              disabled={status.testing || !config.botToken || !config.chatId}
              className={`telegram-button primary ${status.testing ? 'disabled' : ''}`}
            >
              {status.testing ? '⏳ Testando...' : '🧪 Testar Conexão'}
            </button>

            <button 
              onClick={saveConfig}
              disabled={!config.botToken || !config.chatId}
              className="telegram-button success"
            >
              💾 Salvar Configurações
            </button>
          </div>

          {/* Connection Status */}
          {status.lastTest && (
            <div className={`status-badge ${status.isConnected ? 'success' : 'error'}`}>
              {status.isConnected ? '✅' : '❌'} 
              {status.isConnected 
                ? `Conectado - Último teste: ${status.lastTest}`
                : `Falha na conexão - Último teste: ${status.lastTest}`
              }
            </div>
          )}
        </div>
      )}

      {/* INSTRUCTIONS */}
      {showInstructions && (
        <div className="instructions">
          <h3 className="instructions-title">
            📚 Como configurar o Telegram Bot
          </h3>
          
          <div className="instructions-content">
            <h4>1️⃣ Criar o Bot</h4>
            <ol className="instructions-list">
              <li>Abra o Telegram e busque por <code>@BotFather</code></li>
              <li>Digite <code>/newbot</code></li>
              <li>Escolha um nome para seu bot</li>
              <li>Escolha um username (deve terminar com "bot")</li>
              <li>Copie o <strong>Token</strong> fornecido</li>
            </ol>

            <h4>2️⃣ Obter o Chat ID</h4>
            <ol className="instructions-list">
              <li>Busque por <code>@userinfobot</code> no Telegram</li>
              <li>Inicie uma conversa com ele</li>
              <li>Ele enviará seu <strong>Chat ID</strong></li>
              <li>Copie o número</li>
            </ol>

            <h4>3️⃣ Ativar o Bot</h4>
            <ol className="instructions-list">
              <li>Busque pelo seu bot criado (pelo username)</li>
              <li>Clique em <strong>Start</strong> ou envie <code>/start</code></li>
              <li>Cole as credenciais aqui e teste a conexão</li>
            </ol>

            <div className="warning-box">
              ⚠️ <strong>Importante:</strong> Mantenha seu Token seguro! 
              Ele dá acesso total ao seu bot.
            </div>
          </div>

          <button 
            onClick={() => setShowInstructions(false)}
            className="close-instructions"
          >
            Entendi ✓
          </button>
        </div>
      )}

      {/* TIPS */}
      <div className="tips-section">
        <h3 className="tips-title">💡 Dicas</h3>
        <ul className="tips-list">
          <li>✅ Você pode habilitar Email e Telegram simultaneamente</li>
          <li>🔒 Suas credenciais ficam salvas localmente no navegador</li>
          <li>🧪 Teste a conexão antes de ativar o monitoramento</li>
          <li>📱 Garanta que iniciou conversa com o bot no Telegram</li>
          <li>🌐 A conexão é testada diretamente via API do Telegram</li>
        </ul>
      </div>
    </div>
  );
};

export default TelegramConfig;