// front/crypto-monitor-frontend/src/contexts/TelegramContext.jsx
// ✅ SEGURO - Context com criptografia para Telegram

import React, { createContext, useContext, useState, useEffect } from 'react';
import CryptoJS from 'crypto-js';

const TelegramContext = createContext();

// ✅ Chave de criptografia (deve estar nas variáveis de ambiente)
const ENCRYPTION_KEY = process.env.REACT_APP_ENCRYPTION_KEY || 'crypto-monitor-default-key-2024';

// ✅ Funções de criptografia/descriptografia
const encryptData = (data) => {
  try {
    const jsonStr = JSON.stringify(data);
    const encrypted = CryptoJS.AES.encrypt(jsonStr, ENCRYPTION_KEY).toString();
    return encrypted;
  } catch (error) {
    console.error('❌ Erro ao criptografar dados:', error);
    return null;
  }
};

const decryptData = (encryptedData) => {
  try {
    const bytes = CryptoJS.AES.decrypt(encryptedData, ENCRYPTION_KEY);
    const decryptedStr = bytes.toString(CryptoJS.enc.Utf8);
    
    if (!decryptedStr) {
      console.warn('⚠️ Dados criptografados inválidos ou corrompidos');
      return null;
    }
    
    return JSON.parse(decryptedStr);
  } catch (error) {
    console.error('❌ Erro ao descriptografar dados:', error);
    return null;
  }
};

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

  const [isLoading, setIsLoading] = useState(true);

  // ✅ CARREGAR dados criptografados do localStorage ao inicializar
  useEffect(() => {
    const loadConfig = () => {
      try {
        const encrypted = localStorage.getItem('telegram_config_enc');
        
        if (encrypted) {
          const decrypted = decryptData(encrypted);
          
          if (decrypted) {
            // ✅ Validar estrutura dos dados
            if (decrypted.botToken !== undefined && decrypted.chatId !== undefined) {
              setTelegramConfig(decrypted);
              
              if (process.env.NODE_ENV === 'development') {
                console.log('📥 Telegram config carregada (criptografada)');
              }
            } else {
              console.warn('⚠️ Estrutura de dados inválida, usando padrão');
            }
          } else {
            console.warn('⚠️ Falha ao descriptografar, dados podem estar corrompidos');
            // Limpar dados corrompidos
            localStorage.removeItem('telegram_config_enc');
          }
        }
      } catch (error) {
        console.error('❌ Erro ao carregar config do Telegram:', error);
        // Em caso de erro, limpar dados corrompidos
        localStorage.removeItem('telegram_config_enc');
      } finally {
        setIsLoading(false);
      }
    };

    loadConfig();
  }, []);

  // ✅ SALVAR dados CRIPTOGRAFADOS no localStorage sempre que mudar
  useEffect(() => {
    if (isLoading) return; // Não salvar durante carregamento inicial

    try {
      const encrypted = encryptData(telegramConfig);
      
      if (encrypted) {
        localStorage.setItem('telegram_config_enc', encrypted);
        
        if (process.env.NODE_ENV === 'development') {
          console.log('💾 Telegram config salva (criptografada)');
          // ✅ Em dev, mostrar apenas se está configurado, sem expor dados
          console.log('   Bot Token:', telegramConfig.botToken ? '***CONFIGURADO***' : 'vazio');
          console.log('   Chat ID:', telegramConfig.chatId ? '***CONFIGURADO***' : 'vazio');
        }
      }
    } catch (error) {
      console.error('❌ Erro ao salvar config do Telegram:', error);
    }
  }, [telegramConfig, isLoading]);

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
    localStorage.removeItem('telegram_config_enc');
    
    if (process.env.NODE_ENV === 'development') {
      console.log('🗑️ Configurações do Telegram removidas');
    }
  };

  // ✅ Método para verificar se está configurado
  const isConfigured = () => {
    return !!(telegramConfig.botToken && telegramConfig.chatId);
  };

  // ✅ Validar formato do bot token
  const validateBotToken = (token) => {
    if (!token) return { valid: false, error: 'Token vazio' };
    
    // Formato esperado: 123456789:ABC-DEF1234ghIkl-zyx57W2v1u123ew11
    const tokenRegex = /^\d+:[A-Za-z0-9_-]+$/;
    
    if (!tokenRegex.test(token)) {
      return { 
        valid: false, 
        error: 'Formato inválido. Deve ser: 123456789:ABC-DEF...' 
      };
    }
    
    return { valid: true };
  };

  // ✅ Validar formato do chat ID
  const validateChatId = (chatId) => {
    if (!chatId) return { valid: false, error: 'Chat ID vazio' };
    
    // Formato esperado: número positivo ou negativo
    const chatIdRegex = /^-?\d+$/;
    
    if (!chatIdRegex.test(chatId)) {
      return { 
        valid: false, 
        error: 'Deve conter apenas números' 
      };
    }
    
    return { valid: true };
  };

  // ✅ Validar configuração completa
  const validateConfig = () => {
    const tokenValidation = validateBotToken(telegramConfig.botToken);
    if (!tokenValidation.valid) {
      return { valid: false, error: `Token: ${tokenValidation.error}` };
    }
    
    const chatIdValidation = validateChatId(telegramConfig.chatId);
    if (!chatIdValidation.valid) {
      return { valid: false, error: `Chat ID: ${chatIdValidation.error}` };
    }
    
    return { valid: true };
  };

  const value = {
    telegramConfig,
    updateConfig,
    setConnected,
    clearConfig,
    isConfigured,
    validateBotToken,
    validateChatId,
    validateConfig,
    isLoading
  };

  return (
    <TelegramContext.Provider value={value}>
      {children}
    </TelegramContext.Provider>
  );
};