// front/crypto-monitor-frontend/src/components/dashboard/StatusCard.jsx
// ✅ VERSÃO SIMPLIFICADA - Apenas chama a função

import React from 'react';
import { PlayCircle, StopCircle } from 'lucide-react';
import '../../styles/components.css';

function StatusCard({
  isMonitoring,
  onStartStop,
  selectedCryptos,
  monitoringEmail
}) {
  const canStart = selectedCryptos.length > 0 && monitoringEmail && monitoringEmail.trim() !== '';

  const handleClick = () => {
    // Validação visual
    if (!isMonitoring && !canStart) {
      alert('⚠️ Configuração necessária:\n\n• Configure um email válido\n• Selecione pelo menos uma criptomoeda\n\nDepois clique em INICIAR novamente.');
      return;
    }
    
    // Chamar a função do App.jsx
    console.log('StatusCard: Chamando onStartStop()');
    onStartStop();
  };

  return (
    <div className={`status-card ${isMonitoring ? 'active' : 'inactive'}`}>
      <div className="status-card-bg-circle"></div>
      
      <div className="status-content">
        <div className="status-info">
          <div className="status-icon">
            {isMonitoring ? (
              <PlayCircle size={40} />
            ) : (
              <StopCircle size={40} />
            )}
          </div>
          <div>
            <h2 className="status-title">
              {isMonitoring ? '✓ Monitoramento Ativo' : '○ Monitoramento Inativo'}
            </h2>
            <p className="status-description">
              {isMonitoring 
                ? 'Você está recebendo alertas em tempo real'
                : 'Configure e inicie para receber alertas em tempo real'}
            </p>
          </div>
        </div>
        
        <button
          onClick={handleClick}
          className={`status-button ${isMonitoring ? 'active' : 'inactive'}`}
          style={{ opacity: !isMonitoring && !canStart ? 0.5 : 1 }}
        >
          {isMonitoring ? (
            <>
              <StopCircle size={20} />
              Parar
            </>
          ) : (
            <>
              <PlayCircle size={20} />
              Iniciar
            </>
          )}
        </button>
      </div>
    </div>
  );
}

export default StatusCard;