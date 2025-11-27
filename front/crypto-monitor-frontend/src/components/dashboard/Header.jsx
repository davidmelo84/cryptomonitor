// front/crypto-monitor-frontend/src/components/dashboard/Header.jsx

import React from 'react';
import { TrendingUp, User, RefreshCw, LogOut, Wallet, Bot, Send } from 'lucide-react';
import { useTheme } from '../../contexts/ThemeContext';
import ThemeToggle from '../common/ThemeToggle';
import '../../styles/components/dashboard.css';


function Header({ 
  user, 
  lastUpdate, 
  isRefreshing, 
  onRefresh, 
  onLogout,
  onNavigateToPortfolio,
  onNavigateToBots,
  onOpenTelegramConfig // ✅ NOVO
}) {
  const { isDark } = useTheme();

  return (
    <div className={`dashboard-header ${isDark ? 'dark' : ''}`}>
      <div className="header-content">
        {/* Logo */}
        <div className="header-logo">
          <div className="logo-icon">
            <TrendingUp size={32} color="white" />
          </div>
          <div>
            <h1 className="header-title">Crypto Monitor</h1>
            <p className="header-subtitle">
              {lastUpdate && `Atualizado ${lastUpdate.toLocaleTimeString()}`}
            </p>
          </div>
        </div>
        
        {/* Actions */}
        <div className="header-actions">
          <ThemeToggle />
          
          <div className="user-badge">
            <User size={18} color="#667eea" />
            {user?.username}
          </div>
          
          <button
            onClick={onNavigateToPortfolio}
            className="header-btn header-btn-portfolio"
          >
            <Wallet size={16} />
            Portfolio
          </button>

          <button
            onClick={onNavigateToBots}
            className="header-btn header-btn-bots"
          >
            <Bot size={16} />
            Trading Bots
          </button>

          {/* ✅ NOVO: Botão Telegram */}
          <button
            onClick={onOpenTelegramConfig}
            className="header-btn header-btn-telegram"
          >
            <Send size={16} />
            Telegram
          </button>
          
          <button
            onClick={onRefresh}
            disabled={isRefreshing}
            className="header-btn header-btn-refresh"
          >
            <RefreshCw 
              size={16} 
              className={isRefreshing ? 'animate-spin' : ''} 
            />
            Atualizar
          </button>
          
          <button
            onClick={onLogout}
            className="header-btn header-btn-logout"
          >
            <LogOut size={16} />
            Sair
          </button>
        </div>
      </div>
    </div>
  );
}

export default Header;