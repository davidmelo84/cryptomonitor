// front/crypto-monitor-frontend/src/components/telegram/TelegramConfig.jsx
// ✅ VERSÃO COM PERSISTÊNCIA - Usa TelegramContext

import React, { useState, useEffect } from 'react';
import { useTelegram } from '../../contexts/TelegramContext';
import '../../styles/TelegramConfig.css';

const TelegramConfig = ({ userEmail }) => {
  // ✅ USAR CONTEXT ao invés de state local
  const { telegramConfig, updateConfig, setConnected, isConfigured } = useTelegram();

  const [status, setStatus] = useState({
    testing: false
  });

  const [notifications, setNotifications] = useState({
    email: true,
    telegram: false
  });

  const [showInstructions, setShowInstructions] = useState(false);
  const [error, setError] = useState('');

  // ✅ Sincronizar estado de notificações com o Context
  useEffect(() => {
    setNotifications(prev => ({
      ...prev,
      telegram: telegramConfig.enabled
    }));
  }, [telegramConfig.enabled]);

  // ✅ SALVAR configurações - agora salva automaticamente via Context
  const saveConfig = () => {
    if (!telegramConfig.botToken || !telegramConfig.chatId) {
      setError('⚠️ Preencha o Token do Bot e o Chat ID');
      return;
    }

    // O Context já salva automaticamente no localStorage
    alert('✅ Configurações do Telegram salvas com sucesso!');
    setError('');
  };

  // ✅ TESTAR CONEXÃO
  const testConnection = async () => {
    if (!telegramConfig.botToken || !telegramConfig.chatId) {
      setError('⚠️ Preencha o Token do Bot e o Chat ID');
      return;
    }

    setStatus({ testing: true });
    setError('');

    try {
      console.log('🧪 Testando conexão Telegram...');
      console.log('   Token:', telegramConfig.botToken.substring(0, 10) + '...');
      console.log('   Chat ID:', telegramConfig.chatId);

      const telegramApiUrl = `https://api.telegram.org/bot${telegramConfig.botToken}/sendMessage`;
      
      const response = await fetch(telegramApiUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          chat_id: telegramConfig.chatId,
          text: '🧪 Teste de Conexão do Crypto Monitor\n\n✅ Se você recebeu esta mensagem, o Telegram está configurado corretamente!',
          parse_mode: 'Markdown'
        })
      });

      const data = await response.json();

      if (response.ok && data.ok) {
        // ✅ Marcar como conectado no Context
        setConnected(true);
        
        alert('✅ Conexão com Telegram estabelecida!\n\nVerifique sua mensagem de teste.');
        setError('');
      } else {
        throw new Error(data.description || 'Falha ao conectar');
      }
    } catch (error) {
      console.error('❌ Erro:', error);
      
      // ✅ Marcar como desconectado
      setConnected(false);
      
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
    } finally {
      setStatus({ testing: false });
    }
  };

  // ✅ TOGGLE de canais de notificação
  const toggleNotificationChannel = (channel) => {
    setNotifications(prev => ({
      ...prev,
      [channel]: !prev[channel]
    }));

    if (channel === 'telegram') {
      updateConfig({ enabled: !telegramConfig.enabled });
    }
  };

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

      {/* ✅ STATUS DE CONFIGURAÇÃO */}
      {isConfigured() && (
        <div style={{
          background: telegramConfig.isConnected ? '#d1fae5' : '#fef3c7',
          color: telegramConfig.isConnected ? '#065f46' : '#92400e',
          padding: '12px 16px',
          borderRadius: '10px',
          marginBottom: '20px',
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          fontWeight: '500'
        }}>
          {telegramConfig.isConnected ? '✅' : '⚠️'} 
          {telegramConfig.isConnected 
            ? 'Telegram configurado e testado com sucesso!'
            : 'Telegram configurado. Clique em "Testar Conexão" para validar.'
          }
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
                {telegramConfig.isConnected ? '🟢 Conectado' : '⚪ Desconectado'}
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
              value={telegramConfig.botToken}
              onChange={(e) => {
                updateConfig({ botToken: e.target.value });
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
              value={telegramConfig.chatId}
              onChange={(e) => {
                updateConfig({ chatId: e.target.value });
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
              disabled={status.testing || !telegramConfig.botToken || !telegramConfig.chatId}
              className={`telegram-button primary ${status.testing ? 'disabled' : ''}`}
            >
              {status.testing ? '⏳ Testando...' : '🧪 Testar Conexão'}
            </button>

            <button 
              onClick={saveConfig}
              disabled={!telegramConfig.botToken || !telegramConfig.chatId}
              className="telegram-button success"
            >
              💾 Salvar Configurações
            </button>
          </div>

          {/* Connection Status */}
          {telegramConfig.lastTest && (
            <div className={`status-badge ${telegramConfig.isConnected ? 'success' : 'error'}`}>
              {telegramConfig.isConnected ? '✅' : '❌'} 
              {telegramConfig.isConnected 
                ? `Conectado - Último teste: ${telegramConfig.lastTest}`
                : `Falha na conexão - Último teste: ${telegramConfig.lastTest}`
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
          <li>💾 As configurações persistem após fechar o modal</li>
          <li>🧪 Teste a conexão antes de ativar o monitoramento</li>
          <li>📱 Garanta que iniciou conversa com o bot no Telegram</li>
        </ul>
      </div>
    </div>
  );
};

export default TelegramConfig;