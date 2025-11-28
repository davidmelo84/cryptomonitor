// src/components/ErrorBoundary.jsx
import React from 'react';
// import * as Sentry from '@sentry/react'; // 👉 descomente se já usa Sentry

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

    // ============================================================
    // ✅ Enviar erro para Sentry (apenas produção)
    // ============================================================
    if (process.env.NODE_ENV === 'production') {
      // Sentry.captureException(error, { extra: errorInfo });
    }
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100">
          <div className="bg-white p-8 rounded-lg shadow-lg max-w-md text-center">
            <h2 className="text-2xl font-bold text-red-600 mb-4">
              Algo deu errado
            </h2>

            <p className="text-gray-600 mb-4">
              Ocorreu um erro inesperado. Tente recarregar a página.
            </p>

            {/* Botão de recarregar */}
            <button
              onClick={() => window.location.reload()}
              className="bg-indigo-500 text-white px-4 py-2 rounded hover:bg-indigo-600 transition"
            >
              Recarregar Página
            </button>

            {/* Debug opcional em ambiente de desenvolvimento */}
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
