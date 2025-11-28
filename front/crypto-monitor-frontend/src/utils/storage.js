// front/crypto-monitor-frontend/src/utils/storage.js
// ✅ UTILITÁRIO PARA GERENCIAR STORAGE (sessionStorage vs localStorage)

/**
 * Salva dados de autenticação
 * @param {string} token - JWT token
 * @param {object} user - Dados do usuário
 * @param {boolean} rememberMe - Se deve persistir após fechar aba
 */
export const saveAuthData = (token, user, rememberMe = false) => {
  const storage = rememberMe ? localStorage : sessionStorage;
  
  storage.setItem('token', token);
  storage.setItem('user', JSON.stringify(user));
  storage.setItem('rememberMe', rememberMe.toString());
  
  console.log(`✅ Auth salva em ${rememberMe ? 'localStorage' : 'sessionStorage'}`);
};

/**
 * Carrega dados de autenticação (tenta ambos os storages)
 * @returns {object|null} - { token, user, rememberMe } ou null
 */
export const loadAuthData = () => {
  // 1️⃣ Tentar localStorage primeiro (persist)
  let token = localStorage.getItem('token');
  let user = localStorage.getItem('user');
  let rememberMe = localStorage.getItem('rememberMe') === 'true';
  
  // 2️⃣ Se não encontrou, tentar sessionStorage
  if (!token) {
    token = sessionStorage.getItem('token');
    user = sessionStorage.getItem('user');
    rememberMe = false;
  }
  
  // 3️⃣ Validar
  if (!token || !user) {
    console.log('🔒 Nenhuma sessão ativa');
    return null;
  }
  
  try {
    const parsedUser = JSON.parse(user);
    console.log(`✅ Sessão restaurada (${rememberMe ? 'persist' : 'temporária'}):`, parsedUser.username);
    return { token, user: parsedUser, rememberMe };
  } catch (error) {
    console.error('❌ Erro ao parsear user:', error);
    clearAuthData();
    return null;
  }
};

/**
 * Remove dados de autenticação (logout)
 */
export const clearAuthData = () => {
  // Limpar ambos os storages
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  localStorage.removeItem('rememberMe');
  
  sessionStorage.removeItem('token');
  sessionStorage.removeItem('user');
  
  console.log('🚪 Dados de autenticação removidos');
};

/**
 * Verifica se há sessão ativa
 * @returns {boolean}
 */
export const hasActiveSession = () => {
  return !!(localStorage.getItem('token') || sessionStorage.getItem('token'));
};

/**
 * Atualiza apenas o token (útil para refresh)
 * @param {string} newToken 
 */
export const updateToken = (newToken) => {
  const rememberMe = localStorage.getItem('rememberMe') === 'true';
  const storage = rememberMe ? localStorage : sessionStorage;
  storage.setItem('token', newToken);
  console.log('🔄 Token atualizado');
};