import React from 'react';
import { Activity, XCircle } from 'lucide-react';

function StatusCard({ isMonitoring, selectedCryptos, monitoringInterval, onStartStop }) {
  return (
    <div
      className={`p-8 rounded-[20px] mb-8 text-white shadow-xl relative overflow-hidden ${
        isMonitoring
          ? 'bg-gradient-to-br from-teal-500 to-green-400'
          : 'bg-gradient-to-br from-pink-400 to-red-500'
      }`}
    >
      <div className="absolute -top-12 -right-12 w-48 h-48 bg-white/10 rounded-full" />
      
      <div className="flex justify-between items-center flex-wrap gap-5 relative">
        <div className="flex items-center gap-5">
          <div className="bg-white/20 p-4 rounded-[15px]">
            {isMonitoring ? <Activity size={40} /> : <XCircle size={40} />}
          </div>
          <div>
            <h2 className="text-3xl font-bold m-0">
              {isMonitoring ? '✓ Monitoramento Ativo' : '○ Monitoramento Inativo'}
            </h2>
            <p className="text-base mt-2 mb-0 opacity-90">
              {isMonitoring
                ? `${selectedCryptos.length} moeda(s) • Verificação a cada ${monitoringInterval} min`
                : 'Configure e inicie para receber alertas em tempo real'}
            </p>
          </div>
        </div>
        
        <button
          onClick={onStartStop}
          className="bg-white px-12 py-5 rounded-xl text-lg font-bold cursor-pointer shadow-lg transition-transform duration-200 hover:scale-105"
          style={{ color: isMonitoring ? '#f5576c' : '#11998e' }}
        >
          {isMonitoring ? '■ Parar' : '▶ Iniciar'}
        </button>
      </div>
    </div>
  );
}

export default StatusCard;