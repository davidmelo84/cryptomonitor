import React from 'react';

function Input({ 
  type = 'text', 
  value, 
  onChange, 
  onKeyDown,
  placeholder, 
  icon,
  className = ''
}) {
  return (
    <div className="relative mb-4">
      <input
        type={type}
        value={value}
        onChange={onChange}
        onKeyDown={onKeyDown}
        placeholder={placeholder}
        className={`w-full p-4 ${icon ? 'pr-12' : ''} border-2 border-gray-200 rounded-lg text-base transition-colors focus:border-indigo-500 focus:outline-none ${className}`}
      />
      {icon && (
        <div className="absolute right-4 top-1/2 -translate-y-1/2">
          {icon}
        </div>
      )}
    </div>
  );
}

export default Input;