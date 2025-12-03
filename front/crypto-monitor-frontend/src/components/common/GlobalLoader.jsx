// src/components/common/GlobalLoader.jsx
import React from 'react';
import '../../styles/components/global-loader.css';

function GlobalLoader() {
  return (
    <div className="global-loader">
      <div className="global-loader-spinner" />
      <p className="global-loader-text">
        Carregando Crypto Monitor...
      </p>
    </div>
  );
}

export default GlobalLoader;