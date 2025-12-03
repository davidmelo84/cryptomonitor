// src/components/ErrorBoundary.jsx
import React from 'react';
import { API_BASE_URL } from '../utils/constants'; 
// import * as Sentry from '@sentry/react';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);

    // 👉 Enviar somente em produção
    if (process.env.NODE_ENV === 'production') {
      // 1. Enviar ao Sentry (caso esteja usando)
      // Sentry.captureException(error, { extra: errorInfo });

      // 2. Enviar ao backend (Render)
      fetch(`${API_BASE_URL}/logs/frontend-error`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: error.toString(),
          stack: error.stack,
          componentStack: errorInfo.componentStack,
          url: window.location.href,
          userAgent: navigator.userAgent,
          timestamp: new Date().toISOString(),
        })
      }).catch(() => {
        // Silenciar erro para não quebrar o app
      });
    }
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100">
          <div className="bg-white p-8 rounded-lg shadow-lg max-w-md text-center">
            <h2 className="text-2xl font-bold text-red-600 mb-4">
              Algo deu errado 😕
            </h2>

            <p className="text-gray-600 mb-4">
              Ocorreu um erro inesperado. Tente recarregar a página.
            </p>

            <button
              onClick={() => window.location.reload()}
              className="bg-indigo-500 text-white px-4 py-2 rounded hover:bg-indigo-600 transition"
            >
              Recarregar Página
            </button>

            {/* Debug no modo dev */}
            {process.env.NODE_ENV === 'development' && (
              <pre className="mt-4 p-3 bg-gray-200 text-sm text-left rounded overflow-x-auto">
                {this.state.error?.toString()}
              </pre>
            )}
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
