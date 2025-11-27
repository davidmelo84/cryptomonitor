// front/crypto-monitor-frontend/src/components/bots/CreateBotModal.jsx 
// ✅ AGORA 100% SEM URL HARDCODED

import React, { useState } from 'react';
import { useTheme } from '../../contexts/ThemeContext';
import ThemeToggle from '../common/ThemeToggle';
import { API_BASE_URL } from '../../utils/constants'; // ✅ IMPORTAÇÃO CERTA
import '../../styles/CreateBotModal.css';

function CreateBotModal({ token, onClose, onBotCreated }) {
  const { isDark } = useTheme();

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
      const response = await fetch(`${API_BASE_URL}/bots`, {   // ✅ AGORA USANDO API_BASE_URL
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
        
        {/* Theme Toggle */}
        <div className="theme-toggle-wrapper">
          <ThemeToggle />
        </div>

        <div className="modal-header">
          <h2>🤖 Criar Novo Bot</h2>
          <button className="modal-close-btn" onClick={onClose}>✕</button>
        </div>

        <form onSubmit={handleSubmit} className="bot-form">
          
          {error && (
            <div className="error-message">
              ⚠️ {error}
            </div>
          )}

          {/* ... resto do código permanece IDENTICO ... */}
          
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
