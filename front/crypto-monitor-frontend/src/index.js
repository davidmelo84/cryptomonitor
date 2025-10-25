// front/crypto-monitor-frontend/src/index.js
import React, { Suspense } from 'react';
import ReactDOM from 'react-dom/client';
import './index.css'; // seu CSS global, se houver

// ✅ Lazy loading do App
const App = React.lazy(() => import('./App'));

// Criação do root
const root = ReactDOM.createRoot(document.getElementById('root'));

// Render com Suspense
root.render(
  <React.StrictMode>
    <Suspense fallback={<div className="min-h-screen flex items-center justify-center">Carregando...</div>}>
      <App />
    </Suspense>
  </React.StrictMode>
);
