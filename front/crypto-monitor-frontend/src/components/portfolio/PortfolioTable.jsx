// front/crypto-monitor-frontend/src/components/portfolio/PortfolioTable.jsx

import React from 'react';
import { Wallet } from 'lucide-react';

function PortfolioTable({ portfolio }) {
  const formatCurrency = (value) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(value);
  };

  const formatQuantity = (value) => {
    return parseFloat(value).toFixed(8);
  };

  if (portfolio.length === 0) {
    return (
      <div className="bg-white rounded-xl shadow-md p-12 text-center">
        <Wallet size={48} className="mx-auto mb-4 text-gray-300" />
        <p className="text-lg font-semibold text-gray-500">Portfolio vazio</p>
        <p className="text-sm mt-2 text-gray-400">
          Adicione sua primeira transação para começar
        </p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-xl shadow-md overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-gradient-to-r from-indigo-500 to-purple-600 text-white">
            <tr>
              <th className="px-6 py-4 text-left font-bold">Ativo</th>
              <th className="px-6 py-4 text-right font-bold">Quantidade</th>
              <th className="px-6 py-4 text-right font-bold">Preço Médio</th>
              <th className="px-6 py-4 text-right font-bold">Preço Atual</th>
              <th className="px-6 py-4 text-right font-bold">Total Investido</th>
              <th className="px-6 py-4 text-right font-bold">Valor Atual</th>
              <th className="px-6 py-4 text-right font-bold">Lucro/Prejuízo</th>
              <th className="px-6 py-4 text-right font-bold">%</th>
            </tr>
          </thead>
          <tbody>
            {portfolio.map((item, index) => {
              const isProfitable = item.profitLoss >= 0;
              
              return (
                <tr 
                  key={item.id} 
                  className={`border-b hover:bg-gray-50 transition-colors ${
                    index % 2 === 0 ? 'bg-white' : 'bg-gray-50'
                  }`}
                >
                  {/* Ativo */}
                  <td className="px-6 py-4">
                    <div>
                      <p className="font-bold text-gray-800">{item.coinName}</p>
                      <p className="text-sm text-gray-500">{item.coinSymbol}</p>
                    </div>
                  </td>

                  {/* Quantidade */}
                  <td className="px-6 py-4 text-right font-mono">
                    {formatQuantity(item.quantity)}
                  </td>

                  {/* Preço Médio */}
                  <td className="px-6 py-4 text-right font-semibold">
                    {formatCurrency(item.averageBuyPrice)}
                  </td>

                  {/* Preço Atual */}
                  <td className="px-6 py-4 text-right font-semibold">
                    {formatCurrency(item.currentPrice)}
                  </td>

                  {/* Total Investido */}
                  <td className="px-6 py-4 text-right font-semibold">
                    {formatCurrency(item.totalInvested)}
                  </td>

                  {/* Valor Atual */}
                  <td className="px-6 py-4 text-right font-bold text-indigo-600">
                    {formatCurrency(item.currentValue)}
                  </td>

                  {/* Lucro/Prejuízo */}
                  <td className={`px-6 py-4 text-right font-bold ${
                    isProfitable ? 'text-green-600' : 'text-red-600'
                  }`}>
                    {isProfitable ? '+' : ''}{formatCurrency(item.profitLoss)}
                  </td>

                  {/* Percentual */}
                  <td className={`px-6 py-4 text-right font-bold ${
                    isProfitable ? 'text-green-600' : 'text-red-600'
                  }`}>
                    <div className="flex items-center justify-end gap-1">
                      {isProfitable ? '↗' : '↘'}
                      {Math.abs(item.profitLossPercent).toFixed(2)}%
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default PortfolioTable;