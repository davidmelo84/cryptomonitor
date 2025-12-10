import React from 'react';

// ============================================
// COMPONENTE: Logo Animado Crypto Monitor
// ============================================
const AnimatedLogo = ({ variant = 'login' }) => {
  const isLogin = variant === 'login';
  
  const primaryGradient = isLogin
    ? 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
    : 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)';
    
  const glowColor = isLogin 
    ? 'rgba(102, 126, 234, 0.6)' 
    : 'rgba(17, 153, 142, 0.6)';

  return (
    <div style={{
      textAlign: 'center',
      marginBottom: '2rem'
    }}>
      {/* Logo Container */}
      <div style={{
        position: 'relative',
        display: 'inline-block',
        marginBottom: '1.5rem'
      }}>
        {/* Glow Effect */}
        <div style={{
          position: 'absolute',
          inset: '-20px',
          background: glowColor,
          borderRadius: '50%',
          filter: 'blur(30px)',
          opacity: 0.4,
          animation: 'pulse 3s ease-in-out infinite'
        }} />
        
        {/* Main Logo */}
        <div style={{
          width: '100px',
          height: '100px',
          background: primaryGradient,
          borderRadius: '24px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          position: 'relative',
          boxShadow: `0 0 50px ${glowColor}, 0 20px 40px rgba(0, 0, 0, 0.3)`,
          animation: 'logoFloat 3s ease-in-out infinite',
          transform: 'perspective(1000px) rotateX(0deg) rotateY(0deg)'
        }}>
          {/* Icon: Crypto Symbol */}
          <svg
            width="60"
            height="60"
            viewBox="0 0 24 24"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            style={{
              filter: 'drop-shadow(0 2px 8px rgba(0, 0, 0, 0.3))'
            }}
          >
            {/* Bitcoin/Crypto Icon */}
            <path
              d="M12 2L2 7L12 12L22 7L12 2Z"
              fill="white"
              fillOpacity="0.9"
            />
            <path
              d="M2 17L12 22L22 17"
              stroke="white"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              fillOpacity="0.7"
            />
            <path
              d="M2 12L12 17L22 12"
              stroke="white"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              fillOpacity="0.7"
            />
          </svg>
          
          {/* Shine Effect */}
          <div style={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            borderRadius: '24px',
            background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.3) 0%, transparent 50%)',
            animation: 'shine 3s ease-in-out infinite'
          }} />
        </div>
      </div>

      {/* Title */}
      <h1 style={{
        fontSize: '2.5rem',
        fontWeight: '900',
        margin: '0 0 0.75rem 0',
        background: isLogin
          ? 'linear-gradient(135deg, #667eea 0%, #a78bfa 50%, #c7d2fe 100%)'
          : 'linear-gradient(135deg, #11998e 0%, #38ef7d 50%, #b9f5d8 100%)',
        WebkitBackgroundClip: 'text',
        WebkitTextFillColor: 'transparent',
        backgroundClip: 'text',
        letterSpacing: '-1px',
        textShadow: `0 0 40px ${glowColor}`,
        animation: 'titleGlow 2s ease-in-out infinite'
      }}>
        Crypto Monitor
      </h1>

      {/* Subtitle */}
      <p style={{
        color: '#9ca3af',
        fontSize: '0.9375rem',
        fontWeight: '500',
        letterSpacing: '0.3px',
        margin: 0
      }}>
        {isLogin 
          ? 'Monitore suas criptomoedas em tempo real'
          : 'Junte-se e comece a monitorar seus investimentos'
        }
      </p>

      {/* Animated Particles */}
      <div style={{
        position: 'absolute',
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        width: '200px',
        height: '200px',
        pointerEvents: 'none'
      }}>
        {[...Array(3)].map((_, i) => (
          <div
            key={i}
            style={{
              position: 'absolute',
              width: '4px',
              height: '4px',
              borderRadius: '50%',
              background: isLogin ? '#667eea' : '#11998e',
              opacity: 0.6,
              animation: `particle${i + 1} ${3 + i}s ease-in-out infinite`
            }}
          />
        ))}
      </div>

      <style>{`
        @keyframes logoFloat {
          0%, 100% {
            transform: perspective(1000px) rotateX(0deg) rotateY(0deg) translateY(0px);
          }
          25% {
            transform: perspective(1000px) rotateX(5deg) rotateY(-5deg) translateY(-5px);
          }
          50% {
            transform: perspective(1000px) rotateX(0deg) rotateY(5deg) translateY(-10px);
          }
          75% {
            transform: perspective(1000px) rotateX(-5deg) rotateY(0deg) translateY(-5px);
          }
        }

        @keyframes titleGlow {
          0%, 100% {
            filter: brightness(1);
          }
          50% {
            filter: brightness(1.2);
          }
        }

        @keyframes pulse {
          0%, 100% {
            opacity: 0.4;
            transform: scale(1);
          }
          50% {
            opacity: 0.6;
            transform: scale(1.1);
          }
        }

        @keyframes shine {
          0%, 100% {
            opacity: 0.3;
          }
          50% {
            opacity: 0.6;
          }
        }

        @keyframes particle1 {
          0%, 100% {
            transform: translate(0, 0);
            opacity: 0;
          }
          25% {
            opacity: 0.6;
          }
          50% {
            transform: translate(40px, -40px);
            opacity: 0.8;
          }
          75% {
            opacity: 0.6;
          }
        }

        @keyframes particle2 {
          0%, 100% {
            transform: translate(0, 0);
            opacity: 0;
          }
          25% {
            opacity: 0.6;
          }
          50% {
            transform: translate(-40px, 40px);
            opacity: 0.8;
          }
          75% {
            opacity: 0.6;
          }
        }

        @keyframes particle3 {
          0%, 100% {
            transform: translate(0, 0);
            opacity: 0;
          }
          25% {
            opacity: 0.6;
          }
          50% {
            transform: translate(40px, 40px);
            opacity: 0.8;
          }
          75% {
            opacity: 0.6;
          }
        }
      `}</style>
    </div>
  );
};

// ============================================
// EXEMPLO DE USO
// ============================================
export default function LogoDemo() {
  const [variant, setVariant] = React.useState('login');

  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%)',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '2rem',
      position: 'relative'
    }}>
      {/* Grid Background */}
      <div style={{
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%',
        height: '100%',
        backgroundImage: 
          'linear-gradient(rgba(102, 126, 234, 0.1) 1px, transparent 1px), linear-gradient(90deg, rgba(102, 126, 234, 0.1) 1px, transparent 1px)',
        backgroundSize: '50px 50px',
        animation: 'gridMove 20s linear infinite',
        pointerEvents: 'none'
      }} />

      {/* Logo */}
      <AnimatedLogo variant={variant} />

      {/* Toggle Button */}
      <button
        onClick={() => setVariant(variant === 'login' ? 'register' : 'login')}
        style={{
          marginTop: '2rem',
          padding: '0.75rem 1.5rem',
          background: 'rgba(255, 255, 255, 0.1)',
          backdropFilter: 'blur(10px)',
          border: '1px solid rgba(255, 255, 255, 0.2)',
          borderRadius: '10px',
          color: 'white',
          fontWeight: '600',
          cursor: 'pointer',
          transition: 'all 0.3s ease'
        }}
        onMouseEnter={(e) => {
          e.target.style.background = 'rgba(255, 255, 255, 0.2)';
          e.target.style.transform = 'translateY(-2px)';
        }}
        onMouseLeave={(e) => {
          e.target.style.background = 'rgba(255, 255, 255, 0.1)';
          e.target.style.transform = 'translateY(0)';
        }}
      >
        Alternar: {variant === 'login' ? 'Ver Registro' : 'Ver Login'}
      </button>

      <style>{`
        @keyframes gridMove {
          0% {
            transform: translate(0, 0);
          }
          100% {
            transform: translate(50px, 50px);
          }
        }
      `}</style>
    </div>
  );
}   