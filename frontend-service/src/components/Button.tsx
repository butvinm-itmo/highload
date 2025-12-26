import { motion } from 'framer-motion';
import type { ButtonHTMLAttributes, ReactNode } from 'react';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  children: ReactNode;
  variant?: 'primary' | 'secondary' | 'danger' | 'success';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
  fullWidth?: boolean;
}

export function Button({
  children,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  fullWidth = false,
  disabled,
  className = '',
  ...props
}: ButtonProps) {
  const baseClasses = 'rounded-lg font-serif font-medium focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-void-950 transition-all duration-300 inline-flex items-center justify-center shadow-lg';

  const variantClasses = {
    primary: 'bg-gradient-to-r from-mystic-600 to-cosmic-600 text-white hover:from-mystic-500 hover:to-cosmic-500 focus:ring-mystic-500 disabled:from-gray-600 disabled:to-gray-700 shadow-mystic hover:shadow-cosmic',
    secondary: 'border-2 text-mystic-300 backdrop-blur-sm hover:border-mystic-500 focus:ring-mystic-500 disabled:text-gray-500 disabled:border-gray-700',
    danger: 'bg-gradient-to-r from-red-600 to-red-700 text-white hover:from-red-500 hover:to-red-600 focus:ring-red-500 disabled:from-gray-600 disabled:to-gray-700 shadow-cosmic',
    success: 'bg-gradient-to-r from-emerald-600 to-teal-600 text-white hover:from-emerald-500 hover:to-teal-500 focus:ring-emerald-500 disabled:from-gray-600 disabled:to-gray-700',
  };

  const variantStyles = {
    primary: {},
    secondary: {
      backgroundColor: 'rgba(3, 7, 18, 0.5)',
      borderColor: 'rgba(124, 58, 237, 0.5)',
    },
    danger: {},
    success: {},
  };

  const sizeClasses = {
    sm: 'px-4 py-2 text-sm',
    md: 'px-5 py-2.5 text-base',
    lg: 'px-8 py-3.5 text-lg',
  };

  const widthClass = fullWidth ? 'w-full' : '';

  return (
    <motion.button
      className={`${baseClasses} ${variantClasses[variant]} ${sizeClasses[size]} ${widthClass} ${className} ${disabled || isLoading ? 'cursor-not-allowed' : ''}`}
      style={variantStyles[variant]}
      whileHover={!disabled && !isLoading ? { scale: 1.02 } : undefined}
      whileTap={!disabled && !isLoading ? { scale: 0.98 } : undefined}
      disabled={disabled || isLoading}
      type={props.type}
      onClick={props.onClick}
    >
      {isLoading && (
        <motion.svg
          className="w-4 h-4 mr-2"
          animate={{ rotate: 360 }}
          transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />
          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          />
        </motion.svg>
      )}
      {children}
    </motion.button>
  );
}
