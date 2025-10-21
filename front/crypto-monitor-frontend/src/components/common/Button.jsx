import React from 'react';

function Button({ 
  children, 
  onClick, 
  fullWidth = false, 
  variant = 'primary',
  disabled = false,
  icon,
  className = ''
}) {
  const baseStyles = "px-4 py-3 border-none rounded-lg font-bold cursor-pointer transition-transform duration-200 flex items-center justify-center gap-2";
  
  const variants = {
    primary: "bg-gradient-to-r from-indigo-500 to-purple-600 text-white shadow-lg hover:shadow-xl",
    success: "bg-gradient-to-r from-teal-500 to-green-400 text-white shadow-lg hover:shadow-xl",
    danger: "bg-red-500 text-white shadow-lg hover:shadow-xl",
    secondary: "bg-indigo-500 text-white hover:bg-indigo-600"
  };
  
  const widthClass = fullWidth ? 'w-full' : '';
  const variantClass = variants[variant] || variants.primary;
  const disabledClass = disabled ? 'opacity-50 cursor-not-allowed' : 'hover:scale-105';
  
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`${baseStyles} ${widthClass} ${variantClass} ${disabledClass} ${className}`}
    >
      {icon}
      {children}
    </button>
  );
}

export default Button;