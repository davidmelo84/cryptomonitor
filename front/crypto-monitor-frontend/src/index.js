// front/crypto-monitor-frontend/src/index.js
import React, { Suspense } from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';

// Web Vitals
import reportWebVitals from './reportWebVitals';

// Lazy load do App
const App = React.lazy(() => import('./App'));

// Criação do root
const root = ReactDOM.createRoot(document.getElementById('root'));

// Render com Suspense
root.render(
  <React.StrictMode>
    <Suspense fallback={
      <div className="min-h-screen flex items-center justify-center">
        Carregando...
      </div>
    }>
      <App />
    </Suspense>
  </React.StrictMode>
);

// =================================================================
// ✅ Registro do Service Worker (não quebra nada, apenas otimiza)
// =================================================================
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker
      .register('/service-worker.js')
      .catch(err => console.warn('Service Worker falhou ao registrar:', err));
  });
}

// =================================================================
// ✅ Métricas de Performance (Web Vitals)
// =================================================================
reportWebVitals((metric) => {
  console.log('[Web Vital]', metric);

  // ===============================================================
  // ✅ Enviar métricas para Google Analytics (opcional)
  // ===============================================================
  /*
  if (window.gtag) {
    window.gtag('event', metric.name, {
      value: metric.value,
      event_category: 'Web Vitals',
      event_label: metric.id,
      non_interaction: true,
    });
  }
  */
});
