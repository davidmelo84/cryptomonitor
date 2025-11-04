import React from 'react';
import { AlertCircle } from 'lucide-react';

function ErrorMessage({ message }) {
  return (
    <div className="bg-red-50 text-red-700 p-4 rounded-lg mb-4 flex items-center gap-3">
      <AlertCircle size={20} />
      <span>{message}</span>
    </div>
  );
}

export default ErrorMessage;