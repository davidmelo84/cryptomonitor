import React from 'react';

function Button({ 
  children, 
  onClick, 
  fullWidth = false, 
  variant = 'primary',
  disabled = false,
  icon
}) {
  const buttonClasses = [
    'btn',
    `btn-${variant}`,
    fullWidth && 'w-full'
  ].filter(Boolean).join(' ');
  
  return (
    <button onClick={onClick} disabled={disabled} className={buttonClasses}>
      {icon}
      {children}
    </button>
  );
}

export default Button;