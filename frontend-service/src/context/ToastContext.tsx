import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import { ToastContainer, type ToastProps } from '../components/Toast';

interface ToastContextType {
  showToast: (toast: Omit<ToastProps, 'id' | 'onClose'>) => void;
  showSuccess: (title: string, message: string) => void;
  showError: (title: string, message: string) => void;
  showInfo: (title: string, message: string) => void;
  showWarning: (title: string, message: string) => void;
}

const ToastContext = createContext<ToastContextType | null>(null);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastProps[]>([]);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id));
  }, []);

  const showToast = useCallback((toast: Omit<ToastProps, 'id' | 'onClose'>) => {
    const id = Math.random().toString(36).substring(7);
    setToasts((prev) => [...prev, { ...toast, id, onClose: removeToast }]);
  }, [removeToast]);

  const showSuccess = useCallback((title: string, message: string) => {
    showToast({ title, message, type: 'success' });
  }, [showToast]);

  const showError = useCallback((title: string, message: string) => {
    showToast({ title, message, type: 'error' });
  }, [showToast]);

  const showInfo = useCallback((title: string, message: string) => {
    showToast({ title, message, type: 'info' });
  }, [showToast]);

  const showWarning = useCallback((title: string, message: string) => {
    showToast({ title, message, type: 'warning' });
  }, [showToast]);

  return (
    <ToastContext.Provider value={{ showToast, showSuccess, showError, showInfo, showWarning }}>
      {children}
      <ToastContainer toasts={toasts} onClose={removeToast} />
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
}
