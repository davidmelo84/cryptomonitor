/* eslint-disable no-restricted-globals */

addEventListener('message', (e) => {
  const { type, data } = e.data;

  switch (type) {
    case 'CALCULATE_PORTFOLIO':
      const result = calculatePortfolio(data);
      postMessage({ type: 'PORTFOLIO_CALCULATED', result });
      break;
      
    case 'FILTER_CRYPTOS':
      const filtered = filterCryptos(data.cryptos, data.searchTerm);
      postMessage({ type: 'CRYPTOS_FILTERED', result: filtered });
      break;
  }
});

function calculatePortfolio(portfolio) {
  return portfolio.map(item => ({
    ...item,
    currentValue: item.quantity * item.currentPrice,
    profitLoss: (item.quantity * item.currentPrice) - item.totalInvested
  }));
}

function filterCryptos(cryptos, searchTerm) {
  const term = searchTerm.toLowerCase();
  return cryptos.filter(crypto =>
    crypto.name.toLowerCase().includes(term) ||
    crypto.symbol.toLowerCase().includes(term)
  );
}
