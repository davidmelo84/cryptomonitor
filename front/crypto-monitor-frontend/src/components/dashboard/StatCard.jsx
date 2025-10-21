import React from 'react';

function StatCard({ icon, label, value, valueColor = '#333' }) {
  return (
    <div className="bg-white p-5 rounded-[15px] shadow-md">
      <div className="flex items-center gap-3 mb-3">
        {icon}
        <span className="text-sm text-gray-600">{label}</span>
      </div>
      <p className="m-0 text-4xl font-bold" style={{ color: valueColor }}>
        {value}
      </p>
    </div>
  );
}

export default StatCard;