import React from 'react';
import { TrendingUp, User, RefreshCw, LogOut } from 'lucide-react';
import Button from '../common/Button';

function Header({ user, lastUpdate, isRefreshing, onRefresh, onLogout }) {
  return (
    <div className="bg-white border-b-4 border-indigo-500 p-5 shadow-md">
      <div className="max-w-[1400px] mx-auto flex justify-between items-center flex-wrap gap-4">
        <div className="flex items-center gap-4">
          <div className="bg-gradient-to-br from-indigo-500 to-purple-600 p-3 rounded-xl">
            <TrendingUp size={32} color="white" />
          </div>
          <div>
            <h1 className="text-2xl font-bold m-0">Crypto Monitor</h1>
            <p className="text-gray-600 text-xs m-0">
              {lastUpdate && `Atualizado ${lastUpdate.toLocaleTimeString()}`}
            </p>
          </div>
        </div>
        
        <div className="flex gap-3 items-center flex-wrap">
          <div className="bg-indigo-50 px-4 py-2 rounded-lg font-bold text-sm flex items-center gap-2">
            <User size={18} color="#667eea" />
            {user?.username}
          </div>
          
          <Button
            onClick={onRefresh}
            disabled={isRefreshing}
            variant="secondary"
            icon={
              <RefreshCw 
                size={16} 
                style={{ animation: isRefreshing ? 'spin 1s linear infinite' : 'none' }} 
              />
            }
            className="text-sm px-4 py-2"
          >
            Atualizar
          </Button>
          
          <Button
            onClick={onLogout}
            variant="danger"
            icon={<LogOut size={16} />}
            className="text-sm px-4 py-2"
          >
            Sair
          </Button>
        </div>
      </div>
    </div>
  );
}

export default Header;