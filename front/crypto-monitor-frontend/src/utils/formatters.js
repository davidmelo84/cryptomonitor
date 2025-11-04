// ============================================
// FORMATTERS UTILITY
// front/crypto-monitor-frontend/src/utils/formatters.js
// ============================================

/**
 * Formata valores monetários
 * @param {number} value - Valor a ser formatado
 * @param {object} options - Opções de formatação
 * @returns {string} - Valor formatado (ex: "$1,234.56")
 */
export const formatCurrency = (value, options = {}) => {
  const defaultOptions = {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  };

  return new Intl.NumberFormat('en-US', {
    ...defaultOptions,
    ...options
  }).format(value || 0);
};

/**
 * Formata valores monetários de forma compacta
 * @param {number} value - Valor a ser formatado
 * @returns {string} - Valor compacto (ex: "$1.2M", "$45.3B")
 */
export const formatCompactCurrency = (value) => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    notation: 'compact',
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  }).format(value || 0);
};

/**
 * Formata percentuais
 * @param {number} value - Valor a ser formatado
 * @param {number} decimals - Casas decimais (padrão: 2)
 * @returns {string} - Percentual formatado (ex: "5.25%")
 */
export const formatPercent = (value, decimals = 2) => {
  if (value === null || value === undefined) return '0.00%';
  return parseFloat(value).toFixed(decimals) + '%';
};

/**
 * Formata percentuais com sinal
 * @param {number} value - Valor a ser formatado
 * @param {number} decimals - Casas decimais (padrão: 2)
 * @returns {string} - Percentual com sinal (ex: "+5.25%", "-3.10%")
 */
export const formatPercentWithSign = (value, decimals = 2) => {
  if (value === null || value === undefined) return '0.00%';
  const sign = value >= 0 ? '+' : '';
  return sign + parseFloat(value).toFixed(decimals) + '%';
};

/**
 * Formata quantidades de criptomoedas
 * @param {number} value - Valor a ser formatado
 * @param {number} decimals - Casas decimais (padrão: 8)
 * @returns {string} - Quantidade formatada (ex: "0.00123456")
 */
export const formatQuantity = (value, decimals = 8) => {
  if (value === null || value === undefined) return '0';
  return parseFloat(value).toFixed(decimals);
};

/**
 * Formata datas
 * @param {string|Date} dateString - Data a ser formatada
 * @param {string} format - Formato ('short', 'long', 'datetime')
 * @returns {string} - Data formatada
 */
export const formatDate = (dateString, format = 'short') => {
  if (!dateString) return '-';
  
  const date = new Date(dateString);
  
  if (isNaN(date.getTime())) return '-';

  const formats = {
    short: {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    },
    long: {
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    },
    datetime: {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    },
    time: {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    }
  };

  return date.toLocaleString('pt-BR', formats[format] || formats.short);
};

/**
 * Formata números grandes de forma compacta
 * @param {number} value - Valor a ser formatado
 * @returns {string} - Número compacto (ex: "1.5M", "45.3B")
 */
export const formatCompactNumber = (value) => {
  if (value === null || value === undefined) return '0';

  if (value >= 1e12) {
    return (value / 1e12).toFixed(1) + 'T';
  }
  if (value >= 1e9) {
    return (value / 1e9).toFixed(1) + 'B';
  }
  if (value >= 1e6) {
    return (value / 1e6).toFixed(1) + 'M';
  }
  if (value >= 1e3) {
    return (value / 1e3).toFixed(1) + 'K';
  }
  
  return value.toFixed(0);
};

/**
 * Formata números com separadores de milhar
 * @param {number} value - Valor a ser formatado
 * @param {number} decimals - Casas decimais (padrão: 0)
 * @returns {string} - Número formatado (ex: "1,234,567")
 */
export const formatNumber = (value, decimals = 0) => {
  if (value === null || value === undefined) return '0';
  
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  }).format(value);
};

/**
 * Formata Market Cap de criptomoedas
 * @param {number} value - Valor do market cap
 * @returns {string} - Market cap formatado (ex: "$845.2B")
 */
export const formatMarketCap = (value) => {
  if (!value || value === 0) return '-';
  
  if (value >= 1e12) {
    return '$' + (value / 1e12).toFixed(1) + 'T';
  }
  if (value >= 1e9) {
    return '$' + (value / 1e9).toFixed(1) + 'B';
  }
  if (value >= 1e6) {
    return '$' + (value / 1e6).toFixed(1) + 'M';
  }
  
  return formatCurrency(value);
};

/**
 * Formata volume de negociação
 * @param {number} value - Valor do volume
 * @returns {string} - Volume formatado (ex: "$1.2B")
 */
export const formatVolume = (value) => {
  if (!value || value === 0) return '-';
  
  if (value >= 1e9) {
    return '$' + (value / 1e9).toFixed(2) + 'B';
  }
  if (value >= 1e6) {
    return '$' + (value / 1e6).toFixed(2) + 'M';
  }
  if (value >= 1e3) {
    return '$' + (value / 1e3).toFixed(2) + 'K';
  }
  
  return formatCurrency(value);
};

/**
 * Formata tempo relativo (ex: "há 2 minutos")
 * @param {string|Date} dateString - Data a ser formatada
 * @returns {string} - Tempo relativo
 */
export const formatRelativeTime = (dateString) => {
  if (!dateString) return '-';
  
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now - date;
  const diffSec = Math.floor(diffMs / 1000);
  const diffMin = Math.floor(diffSec / 60);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);

  if (diffDay > 0) return `há ${diffDay} dia${diffDay > 1 ? 's' : ''}`;
  if (diffHour > 0) return `há ${diffHour} hora${diffHour > 1 ? 's' : ''}`;
  if (diffMin > 0) return `há ${diffMin} minuto${diffMin > 1 ? 's' : ''}`;
  if (diffSec > 0) return `há ${diffSec} segundo${diffSec > 1 ? 's' : ''}`;
  
  return 'agora';
};

/**
 * Trunca texto com reticências
 * @param {string} text - Texto a ser truncado
 * @param {number} maxLength - Comprimento máximo
 * @returns {string} - Texto truncado
 */
export const truncateText = (text, maxLength = 50) => {
  if (!text) return '';
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
};

/**
 * Formata endereço de carteira (ex: "0x1234...5678")
 * @param {string} address - Endereço da carteira
 * @param {number} startChars - Caracteres do início (padrão: 6)
 * @param {number} endChars - Caracteres do fim (padrão: 4)
 * @returns {string} - Endereço formatado
 */
export const formatAddress = (address, startChars = 6, endChars = 4) => {
  if (!address) return '';
  if (address.length <= startChars + endChars) return address;
  
  return `${address.substring(0, startChars)}...${address.substring(address.length - endChars)}`;
};

/**
 * Formata símbolo de crypto em uppercase
 * @param {string} symbol - Símbolo da crypto
 * @returns {string} - Símbolo formatado (ex: "BTC")
 */
export const formatSymbol = (symbol) => {
  if (!symbol) return '';
  return symbol.toUpperCase();
};

/**
 * Valida e formata email
 * @param {string} email - Email a ser validado
 * @returns {object} - { isValid: boolean, formatted: string }
 */
export const validateAndFormatEmail = (email) => {
  if (!email) {
    return { isValid: false, formatted: '' };
  }

  const formatted = email.toLowerCase().trim();
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const isValid = emailRegex.test(formatted);

  return { isValid, formatted };
};

/**
 * Formata classe CSS baseada em valor positivo/negativo
 * @param {number} value - Valor a ser verificado
 * @returns {string} - Classe CSS ('positive', 'negative', 'neutral')
 */
export const getValueClass = (value) => {
  if (value > 0) return 'positive';
  if (value < 0) return 'negative';
  return 'neutral';
};

/**
 * Formata cor baseada em valor positivo/negativo
 * @param {number} value - Valor a ser verificado
 * @returns {string} - Cor HEX
 */
export const getValueColor = (value) => {
  if (value > 0) return '#10b981'; // Verde
  if (value < 0) return '#ef4444'; // Vermelho
  return '#6b7280'; // Cinza
};

// ============================================
// EXPORT DEFAULT (Objeto com todos os formatters)
// ============================================
const formatters = {
  currency: formatCurrency,
  compactCurrency: formatCompactCurrency,
  percent: formatPercent,
  percentWithSign: formatPercentWithSign,
  quantity: formatQuantity,
  date: formatDate,
  compactNumber: formatCompactNumber,
  number: formatNumber,
  marketCap: formatMarketCap,
  volume: formatVolume,
  relativeTime: formatRelativeTime,
  truncateText,
  address: formatAddress,
  symbol: formatSymbol,
  validateEmail: validateAndFormatEmail,
  getValueClass,
  getValueColor
};

export default formatters;