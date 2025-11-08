// front/crypto-monitor-frontend/src/hooks/useFormValidation.js
// ✅ COPIE ESTE ARQUIVO COMPLETO

import { useState, useCallback } from 'react';

/**
 * Hook para validação de formulários
 * @param {Object} initialValues - Valores iniciais do formulário
 * @param {Object} validationRules - Regras de validação
 * @returns {Object} - Estado e funções do formulário
 */
export const useFormValidation = (initialValues, validationRules) => {
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  // ✅ Validar um campo específico
  const validateField = useCallback((name, value) => {
    const rules = validationRules[name];
    if (!rules) return null;

    // Required
    if (rules.required && !value) {
      return rules.required.message || 'Campo obrigatório';
    }

    // Min length
    if (rules.minLength && value.length < rules.minLength.value) {
      return rules.minLength.message || `Mínimo ${rules.minLength.value} caracteres`;
    }

    // Max length
    if (rules.maxLength && value.length > rules.maxLength.value) {
      return rules.maxLength.message || `Máximo ${rules.maxLength.value} caracteres`;
    }

    // Email
    if (rules.email) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(value)) {
        return rules.email.message || 'Digite um email válido';
      }
    }

    // Pattern
    if (rules.pattern && !rules.pattern.value.test(value)) {
      return rules.pattern.message || 'Formato inválido';
    }

    // Custom validation
    if (rules.validate && typeof rules.validate === 'function') {
      return rules.validate(value, values);
    }

    return null;
  }, [validationRules, values]);

  // ✅ Validar todos os campos
  const validateAll = useCallback(() => {
    const newErrors = {};
    let isValid = true;

    Object.keys(validationRules).forEach(name => {
      const error = validateField(name, values[name]);
      if (error) {
        newErrors[name] = error;
        isValid = false;
      }
    });

    setErrors(newErrors);
    return isValid;
  }, [values, validateField, validationRules]);

  // ✅ Atualizar valor de um campo
  const handleChange = useCallback((name, value) => {
    setValues(prev => ({ ...prev, [name]: value }));
    
    // Validar em tempo real se o campo já foi tocado
    if (touched[name]) {
      const error = validateField(name, value);
      setErrors(prev => ({
        ...prev,
        [name]: error
      }));
    }
  }, [touched, validateField]);

  // ✅ Marcar campo como tocado (ao sair do campo)
  const handleBlur = useCallback((name) => {
    setTouched(prev => ({ ...prev, [name]: true }));
    const error = validateField(name, values[name]);
    setErrors(prev => ({ ...prev, [name]: error }));
  }, [values, validateField]);

  // ✅ Submit do formulário
  const handleSubmit = useCallback(async (onSubmit) => {
    // Marcar todos os campos como tocados
    const allTouched = Object.keys(validationRules).reduce((acc, key) => {
      acc[key] = true;
      return acc;
    }, {});
    setTouched(allTouched);

    // Validar
    const isValid = validateAll();
    
    if (!isValid) {
      return false;
    }

    setIsSubmitting(true);
    
    try {
      await onSubmit(values);
      return true;
    } catch (error) {
      console.error('Erro no submit:', error);
      return false;
    } finally {
      setIsSubmitting(false);
    }
  }, [values, validateAll, validationRules]);

  // ✅ Reset do formulário
  const reset = useCallback(() => {
    setValues(initialValues);
    setErrors({});
    setTouched({});
    setIsSubmitting(false);
  }, [initialValues]);

  // ✅ Setar erro manualmente (para erros do servidor)
  const setError = useCallback((name, message) => {
    setErrors(prev => ({ ...prev, [name]: message }));
  }, []);

  return {
    values,
    errors,
    touched,
    isSubmitting,
    handleChange,
    handleBlur,
    handleSubmit,
    reset,
    setError,
    isValid: Object.keys(errors).length === 0
  };
};

// ✅ Regras de validação comuns (reutilizáveis)
export const commonValidations = {
  email: {
    required: { message: 'Email é obrigatório' },
    email: { message: 'Digite um email válido' }
  },
  
  password: {
    required: { message: 'Senha é obrigatória' },
    minLength: { value: 6, message: 'Senha deve ter pelo menos 6 caracteres' }
  },
  
  username: {
    required: { message: 'Nome de usuário é obrigatório' },
    minLength: { value: 3, message: 'Mínimo 3 caracteres' },
    maxLength: { value: 20, message: 'Máximo 20 caracteres' },
    pattern: {
      value: /^[a-zA-Z0-9_]+$/,
      message: 'Apenas letras, números e underscore'
    }
  },
  
  confirmPassword: (passwordFieldName = 'password') => ({
    required: { message: 'Confirmação de senha é obrigatória' },
    validate: (value, values) => {
      if (value !== values[passwordFieldName]) {
        return 'As senhas não coincidem';
      }
      return null;
    }
  })
};

export default useFormValidation;