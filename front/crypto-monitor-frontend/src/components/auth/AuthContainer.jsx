import React from 'react';

function AuthContainer({ children, variant = 'login' }) {
  const floatingCircles = variant === 'login' ? [
    { width: '300px', height: '300px', top: '-150px', left: '-150px' },
    { width: '200px', height: '200px', bottom: '-100px', right: '-100px' }
  ] : [
    { width: '250px', height: '250px', top: '10%', right: '10%' }
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center p-5 relative overflow-hidden">
      {floatingCircles.map((style, index) => (
        <div
          key={index}
          className="absolute rounded-full bg-white/10 animate-float"
          style={style}
        />
      ))}
      
      <div className="bg-white rounded-[20px] p-10 shadow-2xl max-w-[450px] w-full relative z-10">
        {children}
      </div>

      <style>{`
        @keyframes float {
          0%, 100% { transform: translateY(0px); }
          50% { transform: translateY(-20px); }
        }
        .animate-float {
          animation: float 6s ease-in-out infinite;
        }
      `}</style>
    </div>
  );
}

export default AuthContainer;