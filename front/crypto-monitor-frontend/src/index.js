// src/index.js
import React, { Suspense } from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';

// Loader global
import GlobalLoader from './components/common/GlobalLoader';

// Web Vitals
import reportWebVitals from './reportWebVitals';

// Lazy load do App
const App = React.lazy(() => import('./App'));

// Criação do root
const root = ReactDOM.createRoot(document.getElementById('root'));

root.render(
  <React.StrictMode>
    <Suspense fallback={<GlobalLoader />}>
      <App />
    </Suspense>
  </React.StrictMode>
);

// =================================================================
// ✅ Registro do Service Worker
//    - Usamos SW SIMPLES (navegador + Vercel suportam tranquilo)
//    - Garante cache básico e performance
// =================================================================
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker
      .register('/service-worker.js')
      .then(() => console.log('[SW] Registrado com sucesso'))
      .catch(err => console.warn('[SW] Falha ao registrar:', err));
  });
}

// =================================================================
// ✅ Web Vitals (Performance)
// =================================================================
reportWebVitals((metric) => {
  console.log('[Web Vital]', metric);
});
