// front/crypto-monitor-frontend/src/components/pages/RegisterPage.jsx
import React, { useState } from 'react';
import { UserPlus, Mail, Lock, User, Eye, EyeOff, LogIn } from 'lucide-react';

function RegisterPage({ onRegister, onNavigateToLogin, authError }) {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: ''
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.username || !formData.email || !formData.password || !formData.confirmPassword) {
      return;
    }
    setIsSubmitting(true);
    const success = await onRegister(
      formData.username,
      formData.email,
      formData.password,
      formData.confirmPassword
    );
    if (!success) {
      setIsSubmitting(false);
    }
  };

  const getPasswordStrength = () => {
    const length = formData.password.length;
    if (length === 0) return { label: '', width: 0, color: '' };
    if (length < 6) return { label: 'Fraca', width: 33, color: 'bg-red-500' };
    if (length < 10) return { label: 'Média', width: 66, color: 'bg-yellow-500' };
    return { label: 'Forte', width: 100, color: 'bg-green-500' };
  };

  const strength = getPasswordStrength();
  const passwordsMatch = formData.password === formData.confirmPassword;

  return (
    <div className="min-h-screen relative overflow-hidden">
      {/* Background Image */}
      <div 
        className="absolute inset-0 bg-cover bg-center bg-no-repeat"
        style={{
          backgroundImage: "url('https://images.unsplash.com/photo-1621761191319-c6fb62004040?q=80&w=2787')",
        }}
      >
        <div className="absolute inset-0 bg-gradient-to-br from-emerald-900/90 via-teal-900/80 to-cyan-900/90" />
      </div>

      {/* Animated Grid Overlay */}
      <div className="absolute inset-0 opacity-20">
        <div 
          className="w-full h-full"
          style={{
            backgroundImage: `
              linear-gradient(rgba(16, 185, 129, 0.3) 1px, transparent 1px),
              linear-gradient(90deg, rgba(16, 185, 129, 0.3) 1px, transparent 1px)
            `,
            backgroundSize: '50px 50px',
            animation: 'gridMove 20s linear infinite'
          }}
        />
      </div>

      {/* Floating Orbs */}
      <div className="absolute top-[-200px] right-[-200px] w-[500px] h-[500px] rounded-full bg-emerald-500 opacity-20 blur-3xl animate-pulse" />
      <div className="absolute bottom-[-200px] left-[-200px] w-[600px] h-[600px] rounded-full bg-teal-500 opacity-20 blur-3xl animate-pulse" style={{ animationDelay: '1s' }} />

      {/* Content */}
      <div className="relative z-10 min-h-screen flex items-center justify-center p-4">
        <div className="w-full max-w-md">
          {/* Logo & Title */}
          <div className="text-center mb-8" style={{ animation: 'fadeIn 0.8s ease-out' }}>
            <div className="inline-flex items-center justify-center w-20 h-20 rounded-2xl bg-gradient-to-br from-emerald-500 to-teal-600 mb-4 shadow-2xl shadow-emerald-500/50">
              <svg className="w-12 h-12 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
            </div>
            <h1 className="text-4xl font-bold text-white mb-2">
              Criar Conta
            </h1>
            <p className="text-emerald-200">
              Junte-se e comece a monitorar seus investimentos
            </p>
          </div>

          {/* Glass Card */}
          <div 
            className="backdrop-blur-xl bg-white/10 rounded-3xl p-8 shadow-2xl border border-white/20"
            style={{ animation: 'slideUp 0.6s ease-out' }}
          >
            {/* Error Message */}
            {authError && (
              <div className="mb-6 p-4 bg-red-500/20 border border-red-500/50 rounded-xl backdrop-blur-sm">
                <p className="text-red-200 text-sm">⚠️ {authError}</p>
              </div>
            )}

            <div className="space-y-5">
              {/* Username */}
              <div>
                <label className="block text-sm font-medium text-white mb-2">
                  Nome de Usuário
                </label>
                <div className="relative">
                  <input
                    type="text"
                    value={formData.username}
                    onChange={(e) => setFormData({...formData, username: e.target.value})}
                    disabled={isSubmitting}
                    className="w-full px-4 py-3 pl-12 bg-white/10 border border-white/20 rounded-xl text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition-all backdrop-blur-sm disabled:opacity-50"
                    placeholder="Escolha um nome de usuário"
                    minLength={3}
                    required
                  />
                  <User className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-white/50" />
                </div>
              </div>

              {/* Email */}
              <div>
                <label className="block text-sm font-medium text-white mb-2">
                  Email
                </label>
                <div className="relative">
                  <input
                    type="email"
                    value={formData.email}
                    onChange={(e) => setFormData({...formData, email: e.target.value})}
                    disabled={isSubmitting}
                    className="w-full px-4 py-3 pl-12 bg-white/10 border border-white/20 rounded-xl text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition-all backdrop-blur-sm disabled:opacity-50"
                    placeholder="Digite seu email"
                    required
                  />
                  <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-white/50" />
                </div>
              </div>

              {/* Password */}
              <div>
                <label className="block text-sm font-medium text-white mb-2">
                  Senha
                </label>
                <div className="relative">
                  <input
                    type={showPassword ? "text" : "password"}
                    value={formData.password}
                    onChange={(e) => setFormData({...formData, password: e.target.value})}
                    disabled={isSubmitting}
                    className="w-full px-4 py-3 pl-12 pr-12 bg-white/10 border border-white/20 rounded-xl text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition-all backdrop-blur-sm disabled:opacity-50"
                    placeholder="Crie uma senha (mín. 6 caracteres)"
                    minLength={6}
                    required
                  />
                  <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-white/50" />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    disabled={isSubmitting}
                    className="absolute right-4 top-1/2 -translate-y-1/2 text-white/50 hover:text-white transition-colors disabled:opacity-50"
                  >
                    {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                  </button>
                </div>
                
                {/* Password Strength */}
                {formData.password && (
                  <div className="mt-2">
                    <div className="flex items-center justify-between text-xs mb-1">
                      <span className="text-white/70">Força da senha:</span>
                      <span className={`font-bold ${
                        strength.color === 'bg-red-500' ? 'text-red-400' :
                        strength.color === 'bg-yellow-500' ? 'text-yellow-400' :
                        'text-green-400'
                      }`}>
                        {strength.label}
                      </span>
                    </div>
                    <div className="h-2 bg-white/10 rounded-full overflow-hidden backdrop-blur-sm">
                      <div 
                        className={`h-full ${strength.color} transition-all duration-300`}
                        style={{ width: `${strength.width}%` }}
                      />
                    </div>
                  </div>
                )}
              </div>

              {/* Confirm Password */}
              <div>
                <label className="block text-sm font-medium text-white mb-2">
                  Confirmar Senha
                </label>
                <div className="relative">
                  <input
                    type={showConfirmPassword ? "text" : "password"}
                    value={formData.confirmPassword}
                    onChange={(e) => setFormData({...formData, confirmPassword: e.target.value})}
                    disabled={isSubmitting}
                    className="w-full px-4 py-3 pl-12 pr-12 bg-white/10 border border-white/20 rounded-xl text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition-all backdrop-blur-sm disabled:opacity-50"
                    placeholder="Confirme sua senha"
                    required
                  />
                  <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-white/50" />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    disabled={isSubmitting}
                    className="absolute right-4 top-1/2 -translate-y-1/2 text-white/50 hover:text-white transition-colors disabled:opacity-50"
                  >
                    {showConfirmPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                  </button>
                </div>
                {formData.confirmPassword && !passwordsMatch && (
                  <p className="mt-1 text-xs text-red-400">
                    ⚠️ As senhas não coincidem
                  </p>
                )}
              </div>

              {/* Submit Button */}
              <button
                onClick={handleSubmit}
                disabled={isSubmitting || !passwordsMatch || !formData.username || !formData.email || !formData.password}
                className="w-full py-3 px-4 bg-gradient-to-r from-emerald-500 to-teal-600 text-white rounded-xl font-semibold hover:from-emerald-600 hover:to-teal-700 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:ring-offset-2 focus:ring-offset-transparent transition-all transform hover:scale-[1.02] active:scale-[0.98] shadow-lg shadow-emerald-500/50 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
              >
                <span className="flex items-center justify-center gap-2">
                  {isSubmitting ? (
                    <>
                      <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                      Criando conta...
                    </>
                  ) : (
                    <>
                      <UserPlus className="w-5 h-5" />
                      Criar Conta
                    </>
                  )}
                </span>
              </button>
            </div>

            {/* Divider */}
            <div className="my-6 flex items-center">
              <div className="flex-1 h-px bg-white/20" />
              <span className="px-4 text-sm text-white/50">ou</span>
              <div className="flex-1 h-px bg-white/20" />
            </div>

            {/* Login Link */}
            <button
              onClick={onNavigateToLogin}
              disabled={isSubmitting}
              className="w-full py-3 px-4 bg-white/10 backdrop-blur-sm text-white rounded-xl font-semibold hover:bg-white/20 focus:outline-none focus:ring-2 focus:ring-white/50 transition-all border border-white/20 disabled:opacity-50"
            >
              <span className="flex items-center justify-center gap-2">
                <LogIn className="w-5 h-5" />
                Já tem uma conta? <span className="font-bold">Faça login</span>
              </span>
            </button>
          </div>

          {/* Footer */}
          <p className="text-center text-white/60 text-sm mt-6">
            © 2025 Crypto Monitor. Todos os direitos reservados.
          </p>
        </div>
      </div>

      <style>{`
        @keyframes gridMove {
          0% { transform: translate(0, 0); }
          100% { transform: translate(50px, 50px); }
        }
        
        @keyframes fadeIn {
          from { opacity: 0; transform: translateY(-20px); }
          to { opacity: 1; transform: translateY(0); }
        }
        
        @keyframes slideUp {
          from { opacity: 0; transform: translateY(30px); }
          to { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </div>
  );
}

export default RegisterPage;