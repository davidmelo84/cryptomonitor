import React, { useState } from 'react';
import { Settings, Copy, Check } from 'lucide-react';

function SettingsCard({
  monitoringEmail,
  setMonitoringEmail,
  monitoringInterval,
  setMonitoringInterval,
  buyThreshold,
  setBuyThreshold,
  sellThreshold,
  setSellThreshold
}) {
  const [copiedEmail, setCopiedEmail] = useState(false);

  const copyEmailToClipboard = () => {
    navigator.clipboard.writeText(monitoringEmail);
    setCopiedEmail(true);
    setTimeout(() => setCopiedEmail(false), 2000);
  };

  return (
    <div className="bg-white p-8 rounded-[20px] mb-8 shadow-md">
      <h2 className="flex items-center gap-3 mb-6 text-2xl font-bold">
        <Settings size={28} color="#667eea" />
        Configurações de Monitoramento
      </h2>
      
      <div className="grid grid-cols-[repeat(auto-fit,minmax(250px,1fr))] gap-5">
        <div>
          <label className="block mb-3 font-bold text-sm text-gray-800">
            📧 Email para Alertas
          </label>
          <div className="flex gap-3">
            <input
              type="email"
              value={monitoringEmail}
              onChange={(e) => setMonitoringEmail(e.target.value)}
              className="flex-1 p-3 border-2 border-gray-200 rounded-lg text-sm focus:border-indigo-500 focus:outline-none"
              placeholder="seu@email.com"
            />
            {monitoringEmail && (
              <button
                onClick={copyEmailToClipboard}
                className="p-3 text-white border-none rounded-lg cursor-pointer"
                style={{ background: copiedEmail ? '#10b981' : '#667eea' }}
              >
                {copiedEmail ? <Check size={20} /> : <Copy size={20} />}
              </button>
            )}
          </div>
        </div>
        
        <div>
          <label className="block mb-3 font-bold text-sm text-gray-800">
            ⏱️ Intervalo de Verificação
          </label>
          <select
            value={monitoringInterval}
            onChange={(e) => setMonitoringInterval(parseInt(e.target.value))}
            className="w-full p-3 border-2 border-gray-200 rounded-lg text-sm cursor-pointer focus:border-indigo-500 focus:outline-none"
          >
            <option value={1}>1 minuto</option>
            <option value={5}>5 minutos ⭐ Recomendado</option>
            <option value={10}>10 minutos</option>
            <option value={15}>15 minutos</option>
            <option value={30}>30 minutos</option>
            <option value={60}>1 hora</option>
          </select>
        </div>
        
        <div>
          <label className="block mb-3 font-bold text-sm text-gray-800">
            📉 Alerta de Compra
          </label>
          <input
            type="number"
            value={buyThreshold}
            onChange={(e) => setBuyThreshold(parseFloat(e.target.value) || 0)}
            className="w-full p-3 border-2 border-gray-200 rounded-lg text-sm focus:border-indigo-500 focus:outline-none"
            step="0.5"
            min="0"
            placeholder="% de queda"
          />
        </div>
        
        <div>
          <label className="block mb-3 font-bold text-sm text-gray-800">
            📈 Alerta de Venda
          </label>
          <input
            type="number"
            value={sellThreshold}
            onChange={(e) => setSellThreshold(parseFloat(e.target.value) || 0)}
            className="w-full p-3 border-2 border-gray-200 rounded-lg text-sm focus:border-indigo-500 focus:outline-none"
            step="0.5"
            min="0"
            placeholder="% de alta"
          />
        </div>
      </div>
    </div>
  );
}

export default SettingsCard;