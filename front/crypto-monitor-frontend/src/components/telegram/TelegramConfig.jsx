import React, { useState } from 'react';
import { 
  Send, Mail, CheckCircle, AlertCircle, Info, X, 
  Copy, Check, HelpCircle, Zap
} from 'lucide-react';

function TelegramConfig({ userEmail, onClose }) {
  const [config, setConfig] = useState({
    botToken: '',
    chatId: '',
    enabled: false,
    isConnected: false
  });

  const [notifications, setNotifications] = useState({
    email: true,
    telegram: false
  });

  const [testing, setTesting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showInstructions, setShowInstructions] = useState(false);
  const [copiedToken, setCopiedToken] = useState(false);
  const [copiedChatId, setCopiedChatId] = useState(false);

  const handleTest = async () => {
    if (!config.botToken || !config.chatId) {
      setError('⚠️ Preencha o Token do Bot e o Chat ID');
      return;
    }

    setTesting(true);
    setError('');
    setSuccess('');

    try {
      const telegramApiUrl = `https://api.telegram.org/bot${config.botToken}/sendMessage`;
      
      const response = await fetch(telegramApiUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          chat_id: config.chatId,
          text: '🧪 Teste de Conexão do Crypto Monitor\n\n✅ Se você recebeu esta mensagem, o Telegram está configurado corretamente!',
          parse_mode: 'Markdown'
        })
      });

      const data = await response.json();

      if (response.ok && data.ok) {
        setConfig(prev => ({ ...prev, isConnected: true }));
        setSuccess('✅ Conexão estabelecida! Verifique sua mensagem de teste.');
        setError('');
      } else {
        throw new Error(data.description || 'Falha ao conectar');
      }
    } catch (err) {
      console.error('❌ Erro:', err);
      setConfig(prev => ({ ...prev, isConnected: false }));
      
      let errorMessage = 'Erro ao conectar com Telegram';
      if (err.message.includes('Unauthorized')) {
        errorMessage = '❌ Token do Bot inválido. Verifique com @BotFather';
      } else if (err.message.includes('chat not found')) {
        errorMessage = '❌ Chat ID inválido. Verifique com @userinfobot';
      } else {
        errorMessage = `❌ ${err.message}`;
      }
      
      setError(errorMessage);
      setSuccess('');
    } finally {
      setTesting(false);
    }
  };

  const handleSave = () => {
    if (!config.botToken || !config.chatId) {
      setError('⚠️ Preencha todos os campos antes de salvar');
      return;
    }

    // Salvar no localStorage
    localStorage.setItem('telegram_config', JSON.stringify(config));
    setSuccess('💾 Configurações salvas com sucesso!');
    setError('');
  };

  const copyToClipboard = (text, type) => {
    navigator.clipboard.writeText(text);
    if (type === 'token') {
      setCopiedToken(true);
      setTimeout(() => setCopiedToken(false), 2000);
    } else {
      setCopiedChatId(true);
      setTimeout(() => setCopiedChatId(false), 2000);
    }
  };

  return (
    <div className="space-y-6">
      {/* Messages */}
      {error && (
        <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-4 flex items-start gap-3 animate-pulse">
          <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5" />
          <p className="text-red-300 text-sm">{error}</p>
        </div>
      )}

      {success && (
        <div className="bg-emerald-500/10 border border-emerald-500/30 rounded-xl p-4 flex items-start gap-3 animate-pulse">
          <CheckCircle className="w-5 h-5 text-emerald-400 flex-shrink-0 mt-0.5" />
          <p className="text-emerald-300 text-sm">{success}</p>
        </div>
      )}

      {config.isConnected && (
        <div className="bg-emerald-500/10 border border-emerald-500/30 rounded-xl p-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-emerald-500/20 flex items-center justify-center">
              <Zap className="w-5 h-5 text-emerald-400" />
            </div>
            <div className="flex-1">
              <p className="text-emerald-300 font-semibold">Telegram Conectado!</p>
              <p className="text-emerald-400/60 text-sm">Seus alertas serão enviados via Telegram</p>
            </div>
          </div>
        </div>
      )}

      {/* Notification Channels */}
      <div>
        <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
          <Send className="w-5 h-5 text-blue-400" />
          Canais de Notificação
        </h3>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Email Channel */}
          <div 
            onClick={() => setNotifications(prev => ({ ...prev, email: !prev.email }))}
            className={`p-4 rounded-xl border-2 cursor-pointer transition-all ${
              notifications.email
                ? 'bg-blue-500/10 border-blue-500/50 shadow-lg shadow-blue-500/20'
                : 'bg-white/5 border-white/10 hover:bg-white/10'
            }`}
          >
            <div className="flex items-start justify-between mb-3">
              <div className="w-12 h-12 rounded-xl bg-blue-500/20 flex items-center justify-center">
                <Mail className="w-6 h-6 text-blue-400" />
              </div>
              <div className={`w-6 h-6 rounded-md border-2 flex items-center justify-center ${
                notifications.email ? 'bg-blue-500 border-blue-500' : 'border-white/20'
              }`}>
                {notifications.email && <Check className="w-4 h-4 text-white" />}
              </div>
            </div>
            <h4 className="text-white font-semibold mb-1">Email</h4>
            <p className="text-white/60 text-sm truncate">{userEmail}</p>
          </div>

          {/* Telegram Channel */}
          <div 
            onClick={() => setNotifications(prev => ({ ...prev, telegram: !prev.telegram }))}
            className={`p-4 rounded-xl border-2 cursor-pointer transition-all ${
              notifications.telegram
                ? 'bg-blue-500/10 border-blue-500/50 shadow-lg shadow-blue-500/20'
                : 'bg-white/5 border-white/10 hover:bg-white/10'
            }`}
          >
            <div className="flex items-start justify-between mb-3">
              <div className="w-12 h-12 rounded-xl bg-blue-500/20 flex items-center justify-center">
                <Send className="w-6 h-6 text-blue-400" />
              </div>
              <div className={`w-6 h-6 rounded-md border-2 flex items-center justify-center ${
                notifications.telegram ? 'bg-blue-500 border-blue-500' : 'border-white/20'
              }`}>
                {notifications.telegram && <Check className="w-4 h-4 text-white" />}
              </div>
            </div>
            <h4 className="text-white font-semibold mb-1">Telegram</h4>
            <p className={`text-sm ${
              config.isConnected ? 'text-emerald-400' : 'text-white/60'
            }`}>
              {config.isConnected ? '🟢 Conectado' : '⚪ Desconectado'}
            </p>
          </div>
        </div>
      </div>

      {/* Telegram Configuration */}
      {notifications.telegram && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-bold text-white flex items-center gap-2">
              <Settings className="w-5 h-5 text-purple-400" />
              Credenciais do Bot
            </h3>
            <button
              onClick={() => setShowInstructions(!showInstructions)}
              className="flex items-center gap-2 px-3 py-1.5 bg-white/5 hover:bg-white/10 rounded-lg border border-white/10 transition-all text-sm text-white"
            >
              <HelpCircle className="w-4 h-4" />
              {showInstructions ? 'Ocultar' : 'Ver'} Tutorial
            </button>
          </div>

          {/* Instructions */}
          {showInstructions && (
            <div className="bg-blue-500/10 border border-blue-500/30 rounded-xl p-6 space-y-6">
              <div>
                <h4 className="text-white font-bold mb-3 flex items-center gap-2">
                  <span className="w-6 h-6 rounded-full bg-blue-500/20 flex items-center justify-center text-sm">1</span>
                  Criar o Bot
                </h4>
                <ol className="space-y-2 text-sm text-white/80 ml-8">
                  <li>• Abra o Telegram e busque por <code className="bg-white/10 px-2 py-0.5 rounded">@BotFather</code></li>
                  <li>• Digite <code className="bg-white/10 px-2 py-0.5 rounded">/newbot</code></li>
                  <li>• Escolha um nome para seu bot</li>
                  <li>• Escolha um username (deve terminar com "bot")</li>
                  <li>• Copie o <strong>Token</strong> fornecido</li>
                </ol>
              </div>

              <div>
                <h4 className="text-white font-bold mb-3 flex items-center gap-2">
                  <span className="w-6 h-6 rounded-full bg-blue-500/20 flex items-center justify-center text-sm">2</span>
                  Obter o Chat ID
                </h4>
                <ol className="space-y-2 text-sm text-white/80 ml-8">
                  <li>• Busque por <code className="bg-white/10 px-2 py-0.5 rounded">@userinfobot</code> no Telegram</li>
                  <li>• Inicie uma conversa com ele</li>
                  <li>• Ele enviará seu <strong>Chat ID</strong></li>
                  <li>• Copie o número</li>
                </ol>
              </div>

              <div>
                <h4 className="text-white font-bold mb-3 flex items-center gap-2">
                  <span className="w-6 h-6 rounded-full bg-blue-500/20 flex items-center justify-center text-sm">3</span>
                  Ativar o Bot
                </h4>
                <ol className="space-y-2 text-sm text-white/80 ml-8">
                  <li>• Busque pelo seu bot criado (pelo username)</li>
                  <li>• Clique em <strong>Start</strong> ou envie <code className="bg-white/10 px-2 py-0.5 rounded">/start</code></li>
                  <li>• Cole as credenciais aqui e teste a conexão</li>
                </ol>
              </div>

              <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-4">
                <p className="text-red-300 text-sm">
                  <strong>⚠️ Importante:</strong> Mantenha seu Token seguro! Ele dá acesso total ao seu bot.
                </p>
              </div>
            </div>
          )}

          {/* Bot Token */}
          <div>
            <label className="block text-sm font-semibold text-white mb-2">
              🤖 Token do Bot
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                value={config.botToken}
                onChange={(e) => setConfig(prev => ({ ...prev, botToken: e.target.value }))}
                placeholder="123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11"
                className="flex-1 px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white placeholder-white/40 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent font-mono text-sm"
              />
              <button
                onClick={() => copyToClipboard(config.botToken, 'token')}
                disabled={!config.botToken}
                className="px-4 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all disabled:opacity-50"
              >
                {copiedToken ? <Check className="w-5 h-5 text-emerald-400" /> : <Copy className="w-5 h-5 text-white" />}
              </button>
            </div>
          </div>

          {/* Chat ID */}
          <div>
            <label className="block text-sm font-semibold text-white mb-2">
              💬 Chat ID
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                value={config.chatId}
                onChange={(e) => setConfig(prev => ({ ...prev, chatId: e.target.value }))}
                placeholder="123456789"
                className="flex-1 px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white placeholder-white/40 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent font-mono text-sm"
              />
              <button
                onClick={() => copyToClipboard(config.chatId, 'chatId')}
                disabled={!config.chatId}
                className="px-4 py-3 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all disabled:opacity-50"
              >
                {copiedChatId ? <Check className="w-5 h-5 text-emerald-400" /> : <Copy className="w-5 h-5 text-white" />}
              </button>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex gap-3">
            <button
              onClick={handleTest}
              disabled={testing || !config.botToken || !config.chatId}
              className="flex-1 px-6 py-3 bg-gradient-to-r from-blue-500 to-cyan-600 hover:from-blue-600 hover:to-cyan-700 text-white rounded-xl font-semibold transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 shadow-lg shadow-blue-500/20"
            >
              {testing ? (
                <>
                  <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  Testando...
                </>
              ) : (
                <>
                  <Zap className="w-5 h-5" />
                  Testar Conexão
                </>
              )}
            </button>

            <button
              onClick={handleSave}
              disabled={!config.botToken || !config.chatId}
              className="flex-1 px-6 py-3 bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-600 hover:to-teal-700 text-white rounded-xl font-semibold transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 shadow-lg shadow-emerald-500/20"
            >
              <CheckCircle className="w-5 h-5" />
              Salvar Config
            </button>
          </div>
        </div>
      )}

      {/* Tips */}
      <div className="bg-blue-500/10 border border-blue-500/30 rounded-xl p-6">
        <h3 className="text-white font-bold mb-3 flex items-center gap-2">
          <Info className="w-5 h-5 text-blue-400" />
          💡 Dicas
        </h3>
        <ul className="space-y-2 text-sm text-white/80">
          <li>• Você pode habilitar Email e Telegram simultaneamente</li>
          <li>• Suas credenciais ficam salvas localmente no navegador</li>
          <li>• Teste a conexão antes de ativar o monitoramento</li>
          <li>• Garanta que iniciou conversa com o bot no Telegram</li>
        </ul>
      </div>
    </div>
  );
}

export default TelegramConfig;