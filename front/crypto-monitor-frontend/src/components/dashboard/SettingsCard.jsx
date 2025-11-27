import React, { useState } from 'react';
import { Settings, Copy, Check } from 'lucide-react';
import { useTheme } from '../../contexts/ThemeContext';
import '../../styles/components/dashboard.css';


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
  const { isDark } = useTheme();
  const [copiedEmail, setCopiedEmail] = useState(false);

  const copyEmailToClipboard = () => {
    navigator.clipboard.writeText(monitoringEmail);
    setCopiedEmail(true);
    setTimeout(() => setCopiedEmail(false), 2000);
  };

  return (
    <div className={`settings-card ${isDark ? 'dark' : ''}`}>
      <h2 className="settings-card-header">
        <Settings size={28} color="#667eea" />
        Configurações de Monitoramento
      </h2>
      
      <div className="settings-grid">
        {/* Email */}
        <div className="settings-field">
          <label className="settings-label">
            📧 Email para Alertas
          </label>
          <div className="email-input-wrapper">
            <input
              type="email"
              value={monitoringEmail}
              onChange={(e) => setMonitoringEmail(e.target.value)}
              className="settings-input"
              placeholder="seu@email.com"
            />
            {monitoringEmail && (
              <button
                onClick={copyEmailToClipboard}
                className={`copy-button ${copiedEmail ? 'copied' : ''}`}
              >
                {copiedEmail ? <Check size={20} /> : <Copy size={20} />}
              </button>
            )}
          </div>
        </div>
        
        {/* Intervalo */}
        <div className="settings-field">
          <label className="settings-label">
            ⏱️ Intervalo de Verificação
          </label>
          <select
            value={monitoringInterval}
            onChange={(e) => setMonitoringInterval(parseInt(e.target.value))}
            className="settings-select"
          >
            <option value={1}>1 minuto</option>
            <option value={5}>5 minutos ⭐ Recomendado</option>
            <option value={10}>10 minutos</option>
            <option value={15}>15 minutos</option>
            <option value={30}>30 minutos</option>
            <option value={60}>1 hora</option>
          </select>
        </div>
        
        {/* Buy Threshold */}
        <div className="settings-field">
          <label className="settings-label">
            📉 Alerta de Compra
          </label>
          <input
            type="number"
            value={buyThreshold}
            onChange={(e) => setBuyThreshold(parseFloat(e.target.value) || 0)}
            className="settings-input"
            step="0.5"
            min="0"
            placeholder="% de queda"
          />
        </div>
        
        {/* Sell Threshold */}
        <div className="settings-field">
          <label className="settings-label">
            📈 Alerta de Venda
          </label>
          <input
            type="number"
            value={sellThreshold}
            onChange={(e) => setSellThreshold(parseFloat(e.target.value) || 0)}
            className="settings-input"
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