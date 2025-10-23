import { useTheme } from '../../contexts/ThemeContext';

function StatCard({ icon, label, value, valueColor = '#333' }) {
  const { isDark } = useTheme();
  
  return (
    <div className={`stat-card ${isDark ? 'dark' : ''}`}>
      <div className="stat-icon">
        {icon}
        <span className="stat-label">{label}</span>
      </div>
      <p className="stat-value" style={{ color: valueColor }}>
        {value}
      </p>
    </div>
  );
}

export default StatCard;