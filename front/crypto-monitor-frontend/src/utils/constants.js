// constants.js
export const API_BASE_URL = (() => {
  // 1. Prioridade: variável de ambiente
  if (process.env.REACT_APP_API_URL) {
    return process.env.REACT_APP_API_URL;
  }
  
  // 2. Fallback: baseado no ambiente
  if (window.location.hostname === 'localhost') {
    return 'http://localhost:8080/crypto-monitor/api';
  }
  
  // 3. Produção (Vercel → Render)
  return 'https://crypto-monitor-api-tkla.onrender.com/crypto-monitor/api';
})();

console.log('🔗 API Base URL:', API_BASE_URL);