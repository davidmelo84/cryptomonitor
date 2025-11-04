// front/crypto-monitor-frontend/src/components/bots/CreateBotModal.jsx
// ✅ MODAL DE CRIAÇÃO DE BOT - COM DARK MODE

import React, { useState } from 'react';
import { useTheme } from '../../contexts/ThemeContext';
import ThemeToggle from '../common/ThemeToggle';
import '../../styles/CreateBotModal.css';

function CreateBotModal({ token, onClose, onBotCreated }) {
  const { isDark } = useTheme(); // ✅ HOOK DE TEMA
  
  const [formData, setFormData] = useState({
    name: '',
    coinSymbol: 'BTC',
    strategy: 'GRID_TRADING',
    isSimulation: true,
    gridLowerPrice: '',
    gridUpperPrice: '',
    gridLevels: '10',
    amountPerGrid: '',
    dcaAmount: '',
    dcaIntervalMinutes: '60',
    stopLossPercent: '',
    takeProfitPercent: ''
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const strategies = [
    { value: 'GRID_TRADING', label: 'Grid Trading', description: 'Compra e venda em níveis de preço' },
    { value: 'DCA', label: 'Dollar Cost Average', description: 'Compras periódicas automáticas' },
    { value: 'STOP_LOSS', label: 'Stop Loss / Take Profit', description: 'Gestão de risco automática' }
  ];

  const cryptos = ['BTC', 'ETH', 'ADA', 'DOT', 'LINK', 'SOL', 'AVAX', 'MATIC', 'LTC', 'BCH'];

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    // Validações
    if (!formData.name.trim()) {
      setError('Nome do bot é obrigatório');
      setLoading(false);
      return;
    }

    if (formData.strategy === 'GRID_TRADING') {
      if (!formData.gridLowerPrice || !formData.gridUpperPrice || !formData.amountPerGrid) {
        setError('Preencha todos os campos do Grid Trading');
        setLoading(false);
        return;
      }
    }

    if (formData.strategy === 'DCA' && !formData.dcaAmount) {
      setError('Valor do DCA é obrigatório');
      setLoading(false);
      return;
    }

    if (formData.strategy === 'STOP_LOSS') {
      if (!formData.stopLossPercent && !formData.takeProfitPercent) {
        setError('Configure pelo menos Stop Loss ou Take Profit');
        setLoading(false);
        return;
      }
    }

    try {
      const response = await fetch('http://localhost:8080/crypto-monitor/api/bots', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          ...formData,
          gridLowerPrice: parseFloat(formData.gridLowerPrice) || null,
          gridUpperPrice: parseFloat(formData.gridUpperPrice) || null,
          gridLevels: parseInt(formData.gridLevels) || null,
          amountPerGrid: parseFloat(formData.amountPerGrid) || null,
          dcaAmount: parseFloat(formData.dcaAmount) || null,
          dcaIntervalMinutes: parseInt(formData.dcaIntervalMinutes) || null,
          stopLossPercent: parseFloat(formData.stopLossPercent) || null,
          takeProfitPercent: parseFloat(formData.takeProfitPercent) || null
        })
      });

      if (!response.ok) throw new Error('Failed to create bot');

      const data = await response.json();
      
      if (data.success) {
        alert('✅ Bot criado com sucesso!');
        onBotCreated();
      }
    } catch (err) {
      setError('Erro ao criar bot: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div 
        className={`create-bot-modal ${isDark ? 'dark' : ''}`} 
        onClick={e => e.stopPropagation()}
      >
        
        {/* ✅ Theme Toggle no Modal */}
        <div className="theme-toggle-wrapper">
          <ThemeToggle />
        </div>
        
        {/* Header */}
        <div className="modal-header">
          <h2>🤖 Criar Novo Bot</h2>
          <button className="modal-close-btn" onClick={onClose}>✕</button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="bot-form">
          
          {/* Error Message */}
          {error && (
            <div className="error-message">
              ⚠️ {error}
            </div>
          )}

          {/* Basic Info */}
          <div className="form-section">
            <h3>📝 Informações Básicas</h3>
            
            <div className="form-group">
              <label htmlFor="name">Nome do Bot</label>
              <input
                type="text"
                id="name"
                name="name"
                value={formData.name}
                onChange={handleChange}
                placeholder="Ex: BTC Scalper Pro"
                required
              />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="coinSymbol">Criptomoeda</label>
                <select
                  id="coinSymbol"
                  name="coinSymbol"
                  value={formData.coinSymbol}
                  onChange={handleChange}
                  required
                >
                  {cryptos.map(crypto => (
                    <option key={crypto} value={crypto}>{crypto}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="strategy">Estratégia</label>
                <select
                  id="strategy"
                  name="strategy"
                  value={formData.strategy}
                  onChange={handleChange}
                  required
                >
                  {strategies.map(strat => (
                    <option key={strat.value} value={strat.value}>
                      {strat.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="form-group">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  name="isSimulation"
                  checked={formData.isSimulation}
                  onChange={handleChange}
                />
                <span>Modo Simulação (recomendado para testes)</span>
              </label>
            </div>
          </div>

          {/* Strategy Config - Grid Trading */}
          {formData.strategy === 'GRID_TRADING' && (
            <div className="form-section">
              <h3>📊 Configurações Grid Trading</h3>
              
              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="gridLowerPrice">Preço Mínimo ($)</label>
                  <input
                    type="number"
                    id="gridLowerPrice"
                    name="gridLowerPrice"
                    value={formData.gridLowerPrice}
                    onChange={handleChange}
                    placeholder="40000"
                    step="0.01"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="gridUpperPrice">Preço Máximo ($)</label>
                  <input
                    type="number"
                    id="gridUpperPrice"
                    name="gridUpperPrice"
                    value={formData.gridUpperPrice}
                    onChange={handleChange}
                    placeholder="50000"
                    step="0.01"
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="gridLevels">Número de Níveis</label>
                  <input
                    type="number"
                    id="gridLevels"
                    name="gridLevels"
                    value={formData.gridLevels}
                    onChange={handleChange}
                    placeholder="10"
                    min="2"
                    max="50"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="amountPerGrid">Valor por Grid ($)</label>
                  <input
                    type="number"
                    id="amountPerGrid"
                    name="amountPerGrid"
                    value={formData.amountPerGrid}
                    onChange={handleChange}
                    placeholder="100"
                    step="0.01"
                  />
                </div>
              </div>
            </div>
          )}

          {/* Strategy Config - DCA */}
          {formData.strategy === 'DCA' && (
            <div className="form-section">
              <h3>💰 Configurações DCA</h3>
              
              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="dcaAmount">Valor de Compra ($)</label>
                  <input
                    type="number"
                    id="dcaAmount"
                    name="dcaAmount"
                    value={formData.dcaAmount}
                    onChange={handleChange}
                    placeholder="100"
                    step="0.01"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="dcaIntervalMinutes">Intervalo (minutos)</label>
                  <input
                    type="number"
                    id="dcaIntervalMinutes"
                    name="dcaIntervalMinutes"
                    value={formData.dcaIntervalMinutes}
                    onChange={handleChange}
                    placeholder="60"
                    min="1"
                  />
                </div>
              </div>
            </div>
          )}

          {/* Strategy Config - Stop Loss */}
          {formData.strategy === 'STOP_LOSS' && (
            <div className="form-section">
              <h3>🛡️ Configurações Stop Loss / Take Profit</h3>
              
              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="stopLossPercent">Stop Loss (%)</label>
                  <input
                    type="number"
                    id="stopLossPercent"
                    name="stopLossPercent"
                    value={formData.stopLossPercent}
                    onChange={handleChange}
                    placeholder="5"
                    step="0.1"
                    min="0"
                    max="100"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="takeProfitPercent">Take Profit (%)</label>
                  <input
                    type="number"
                    id="takeProfitPercent"
                    name="takeProfitPercent"
                    value={formData.takeProfitPercent}
                    onChange={handleChange}
                    placeholder="10"
                    step="0.1"
                    min="0"
                    max="1000"
                  />
                </div>
              </div>
            </div>
          )}

          {/* Actions */}
          <div className="form-actions">
            <button 
              type="button" 
              className="btn btn-secondary" 
              onClick={onClose} 
              disabled={loading}
            >
              Cancelar
            </button>
            <button 
              type="submit" 
              className="btn btn-primary" 
              disabled={loading}
            >
              {loading ? 'Criando...' : '✅ Criar Bot'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default CreateBotModal;