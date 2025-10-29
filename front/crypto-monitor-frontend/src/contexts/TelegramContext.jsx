// front/crypto-monitor-frontend/src/contexts/TelegramContext.jsx
// ✅ Context para persistir configurações do Telegram

import React, { createContext, useContext, useState, useEffect } from 'react';

const TelegramContext = createContext();

// Hook customizado para usar o contexto
export const useTelegram = () => {
  const context = useContext(TelegramContext);
  if (!context) {
    throw new Error('useTelegram deve ser usado dentro de TelegramProvider');
  }
  return context;
};

// Provider do contexto
export const TelegramProvider = ({ children }) => {
  const [telegramConfig, setTelegramConfig] = useState({
    botToken: '',
    chatId: '',
    enabled: false,
    isConnected: false,
    lastTest: null
  });

  // ✅ CARREGAR do localStorage ao inicializar
  useEffect(() => {
    try {
      const saved = localStorage.getItem('telegram_config');
      if (saved) {
        const parsed = JSON.parse(saved);
        console.log('📥 Telegram config carregada do localStorage:', parsed);
        setTelegramConfig(parsed);
      }
    } catch (error) {
      console.error('❌ Erro ao carregar config do Telegram:', error);
    }
  }, []);

  // ✅ SALVAR no localStorage sempre que mudar
  useEffect(() => {
    try {
      localStorage.setItem('telegram_config', JSON.stringify(telegramConfig));
      console.log('💾 Telegram config salva no localStorage:', telegramConfig);
    } catch (error) {
      console.error('❌ Erro ao salvar config do Telegram:', error);
    }
  }, [telegramConfig]);

  // ✅ Método para atualizar configurações
  const updateConfig = (newConfig) => {
    setTelegramConfig(prev => ({
      ...prev,
      ...newConfig
    }));
  };

  // ✅ Método para marcar como conectado
  const setConnected = (isConnected) => {
    setTelegramConfig(prev => ({
      ...prev,
      isConnected,
      lastTest: isConnected ? new Date().toLocaleString('pt-BR') : prev.lastTest,
      enabled: isConnected ? true : prev.enabled
    }));
  };

  // ✅ Método para limpar configurações
  const clearConfig = () => {
    setTelegramConfig({
      botToken: '',
      chatId: '',
      enabled: false,
      isConnected: false,
      lastTest: null
    });
    localStorage.removeItem('telegram_config');
    console.log('🗑️ Configurações do Telegram removidas');
  };

  // ✅ Método para verificar se está configurado
  const isConfigured = () => {
    return !!(telegramConfig.botToken && telegramConfig.chatId);
  };

  const value = {
    telegramConfig,
    updateConfig,
    setConnected,
    clearConfig,
    isConfigured
  };

  return (
    <TelegramContext.Provider value={value}>
      {children}
    </TelegramContext.Provider>
  );
};