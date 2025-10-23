import React from 'react';
import { Activity, XCircle } from 'lucide-react';

function StatusCard({ isMonitoring, selectedCryptos, monitoringInterval, onStartStop }) {
  return (
    <div className={`status-card ${isMonitoring ? 'active' : 'inactive'}`}>
      <div className="status-card-bg-circle" />
      
      <div className="status-content">
        <div className="status-info">
          <div className="status-icon">
            {isMonitoring ? <Activity size={40} /> : <XCircle size={40} />}
          </div>
          <div>
            <h2 className="status-title">
              {isMonitoring ? '✓ Monitoramento Ativo' : '○ Monitoramento Inativo'}
            </h2>
            <p className="status-description">
              {isMonitoring
                ? `${selectedCryptos.length} moeda(s) • Verificação a cada ${monitoringInterval} min`
                : 'Configure e inicie para receber alertas em tempo real'}
            </p>
          </div>
        </div>
        
        <button
          onClick={onStartStop}
          className={`status-button ${isMonitoring ? 'active' : 'inactive'}`}
        >
          {isMonitoring ? '■ Parar' : '▶ Iniciar'}
        </button>
      </div>
    </div>
  );
}

export default StatusCard;