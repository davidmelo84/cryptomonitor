// ✅ VERSÃO MELHORADA
export const API_BASE_URL = (() => {
  // 1. Variável de ambiente (PRIORIDADE MÁXIMA)
  if (process.env.REACT_APP_API_URL) {
    console.log('🔗 Usando REACT_APP_API_URL:', process.env.REACT_APP_API_URL);
    return process.env.REACT_APP_API_URL;
  }
  
  // 2. Desenvolvimento local
  if (
    window.location.hostname === 'localhost' || 
    window.location.hostname === '127.0.0.1'
  ) {
    console.log('🔗 Modo desenvolvimento: usando localhost');
    return 'http://localhost:8080/crypto-monitor/api';
  }
  
  // 3. Produção (Vercel → Render)
  const renderUrl = 'https://crypto-monitor-api-tkla.onrender.com/crypto-monitor/api';
  console.log('🔗 Modo produção: usando Render');
  return renderUrl;
})();

console.log('✅ API Base URL configurada:', API_BASE_URL);