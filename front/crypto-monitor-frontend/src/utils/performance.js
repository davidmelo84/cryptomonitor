// front/crypto-monitor-frontend/src/utils/performance.js
export function measurePerformance(componentName) {
  return function(Component) {
    return function WrappedComponent(props) {
      React.useEffect(() => {
        const start = performance.now();
        
        return () => {
          const end = performance.now();
          const renderTime = end - start;
          
          if (renderTime > 16) { // > 60fps
            console.warn(`⚠️ ${componentName} render time: ${renderTime.toFixed(2)}ms`);
          }
        };
      });

      return <Component {...props} />;
    };
  };
}

// Uso:
export default measurePerformance('CryptoCard')(CryptoCard);